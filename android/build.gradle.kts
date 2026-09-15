plugins {
    id("com.android.application")
    kotlin("android")
}

val gdxVersion: String by rootProject.extra

// Конфигурация для нативных библиотек LibGDX (gdx/gdx-freetype для Android)
val natives = configurations.create("natives")

android {
    namespace = "com.catsandmouses"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.catsandmouses"
        minSdk = 24
        targetSdk = 35
        versionCode = 32
        versionName = "1.0.32"
    }

    sourceSets["main"].apply {
        kotlin.srcDirs("src/main/kotlin")
        assets.srcDirs("../assets")
        res.srcDirs("res")
        manifest.srcFile("AndroidManifest.xml")
        jniLibs.srcDirs("libs")
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
// AGP не фильтрует sourceSet.assets через .exclude(), поэтому вычищаем их из результата merge-задачи.
tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
    doLast {
        val merged = outputs.files.singleFile
        fileTree(merged).matching { include("**/raw/**", "**/view/**") }.forEach { it.delete() }
    }
}
