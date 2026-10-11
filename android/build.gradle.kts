import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
}

val gdxVersion: String by rootProject.extra

// Параметры ключа подписи релиза. Файл keystore.properties в git не попадает.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

// Конфигурация для нативных библиотек LibGDX (gdx/gdx-freetype для Android)
val natives = configurations.create("natives")

android {
    namespace = "com.catsandmouses"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.catsandmouses"
        minSdk = 24
        targetSdk = 35
        versionCode = 102001
        versionName = "1.2.1"
    }

    sourceSets["main"].apply {
        kotlin.srcDirs("src/main/kotlin")
        assets.srcDirs("../assets")
        res.srcDirs("res")
        manifest.srcFile("AndroidManifest.xml")
        jniLibs.srcDirs("libs")
    }

    // Подпись релиза берём из keystore.properties (если файла нет — релиз соберётся без подписи)
    signingConfigs {
        create("release") {
            if (keystoreProperties.isNotEmpty()) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (keystoreProperties.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    api("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")

    // Нативные библиотеки gdx и gdx-freetype для Android
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-x86_64")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-x86")
}

// Распаковка нативных .so из natives-зависимостей в libs/<abi> (пакуются в JNI)
val copyAndroidNatives = tasks.register("copyAndroidNatives") {
    doFirst {
        val abis = listOf("armeabi-v7a", "arm64-v8a", "x86_64", "x86")
        abis.forEach { abi ->
            val outputDir = file("libs/$abi")
            outputDir.mkdirs()
            natives.copy().files.forEach { jar ->
                if (jar.name.endsWith("natives-$abi.jar")) {
                    copy {
                        from(zipTree(jar))
                        into(outputDir)
                        include("*.so")
                    }
                }
            }
        }
    }
}

tasks.named("preBuild") {
    dependsOn(copyAndroidNatives)
}

// Каталоги raw/ и view/ хранятся в git только для визуального контроля модели, в APK они не нужны.
// Превью карт (maps/**/*.png) — тоже dev-артефакт. AGP не фильтрует sourceSet.assets через
// .exclude(), поэтому вычищаем их из результата merge-задачи.
tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
    doLast {
        val merged = outputs.files.singleFile
        fileTree(merged).matching {
            include("**/raw/**", "**/view/**", "**/maps/**/*.png", "**/maps/**/*.jpg")
        }.forEach { it.delete() }

        // Мобильная сборка: в APK попадают только уровни с пометкой "use": true из maps.json.
        // Сам манифест в APK фильтруем, чтобы каталог уровней (LevelCatalog) не ссылался на удалённые карты.
        val manifestFile = File(merged, "maps/maps.json")
        if (manifestFile.exists()) {
            val data = JsonSlurper().parse(manifestFile) as Map<*, *>
            val used = (data["levels"] as List<*>)
                .filter { (it as Map<*, *>)["use"] == true }
                .map { it as Map<String, Any?> }
            val usedPaths = used.map { it["path"] as String }.toSet()
            val usedWorlds = usedPaths.mapNotNull { path ->
                val core = path.removePrefix("maps/").removeSuffix(".txt")
                val slash = core.indexOf('/')
                if (slash < 0) null else core.substring(0, slash)
            }.toSet()

            // Карты неактивных уровней и dev-превью к ним (""")
            fileTree(merged).matching { include("**/maps/**/*.txt") }.forEach { f ->
                val rel = f.relativeTo(merged).invariantSeparatorsPath
                if (rel !in usedPaths) f.delete()
            }
            // Миры без активных уровней (их theme.json не нужен в APK)
            fileTree(merged).matching { include("**/maps/*/theme.json") }.forEach { f ->
                val world = f.relativeTo(merged).invariantSeparatorsPath.split('/').getOrNull(1)
                if (world !in usedWorlds) f.delete()
            }
            if ("maps/final.txt" !in usedPaths) {
                File(merged, "maps/final.json").takeIf { it.exists() }?.delete()
            }

            manifestFile.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(mapOf("levels" to used))) + "\n")
            logger.lifecycle("android assets: в APK остаются ${used.size} уровней (use:true из maps.json)")
        }
    }
}

// Публикация версии в RuStore через API: собрать релизный APK и отправить на модерацию.
// Требует rustore-api.properties (gitignored) и активной версии приложения в RuStore.
tasks.register<Exec>("rustoreRelease") {
    group = "publishing"
    description = "Собрать релизный APK и опубликовать версию в RuStore (tools/rustore-publish.ps1 release)"
    dependsOn("assembleRelease")
    workingDir = rootProject.projectDir
    commandLine("pwsh", "-NoProfile", "-File", "tools/rustore-publish.ps1", "release")
}
