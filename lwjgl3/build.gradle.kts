plugins {
    kotlin("jvm")
    application
}

val gdxVersion: String by rootProject.extra

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    // FreeType нужен редактору карт (шрифты BoldPixels); в core уже используется
    implementation("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-desktop")
}

sourceSets["main"].resources.srcDirs("../assets")

// Каталоги raw/ и view/ не должны попадать в jar/dist, но игровые ассеты остаются в jar.
// Превью карт (maps/**/*.png|jpg) — dev-артефакт, в jar не нужен.
tasks.named<Jar>("jar") {
    exclude("**/raw/**")
    exclude("**/view/**")
    exclude("maps/**/*.png")
    exclude("maps/**/*.jpg")
}

application {
    mainClass.set("com.catsandmouses.lwjgl3.Lwjgl3Launcher")
    applicationDefaultJvmArgs = listOf(
        "-Dfile.encoding=UTF-8",
        "-Dstdout.encoding=UTF-8",
        "-Dstderr.encoding=UTF-8"
    )
}

// dev-запуск помечаем явно: `lwjgl3:run` кладёт core в jar, поэтому «по jar» он неотличим от дистрибутива
tasks.named<JavaExec>("run") {
    systemProperty("launch.mode", "d")
    // Путь к исходным assets для редактора карт (`--editor`): jar/APK read-only, пишем в исходники
    systemProperty("assets.dir", rootProject.file("assets").absolutePath)
}

// Запуск собранного дистрибутива: installDist + launcher (аналог desktop-run.bat).
// Конфигурация IDEA «Run Dist» использует эту задачу.
tasks.register<Exec>("runDist") {
    group = "application"
    description = "Собрать автономный дистрибутив и запустить его (installDist + bin/lwjgl3)"
    dependsOn("installDist")
    val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    val binDir = layout.buildDirectory.dir("install/lwjgl3/bin").get().asFile
    workingDir = binDir
    if (isWindows) {
        commandLine("cmd", "/c", "lwjgl3.bat")
    } else {
        commandLine("./lwjgl3")
    }
}

// Редактор карт (dev) — detached-запуск: окно живёт после завершения Gradle-задачи.
// Обычный `run --args="--editor"` (JavaExec) блокирует терминал/агента, пока окно
// открыто; эта задача стартует отдельный процесс и сразу возвращает управление.
tasks.register("runEditor") {
    group = "application"
    description = "Запустить редактор карт (--editor) в фоне, не блокируя терминал"
    dependsOn("classes", ":core:jar")
    val mainClassName = "com.catsandmouses.lwjgl3.Lwjgl3Launcher"
    val assetsDir = rootProject.file("assets")
    val logsDir = layout.buildDirectory.dir("editor-logs")
    val runtimeClasspath = sourceSets["main"].runtimeClasspath
    doLast {
        val logDir = logsDir.get().asFile.apply { mkdirs() }
        val javaBin = File(System.getProperty("java.home"), "bin/java").absolutePath
        ProcessBuilder(
            javaBin,
            "-Dfile.encoding=UTF-8",
            "-Dstdout.encoding=UTF-8",
            "-Dstderr.encoding=UTF-8",
            "-Dassets.dir=${assetsDir.absolutePath}",
            "-Dlaunch.mode=d",
            "-cp", runtimeClasspath.asPath,
            mainClassName,
            "--editor"
        )
            .directory(assetsDir)
            .redirectOutput(File(logDir, "editor-out.log"))
            .redirectError(File(logDir, "editor-err.log"))
            .start()
        logger.lifecycle("Редактор карт запущен в фоне. Логи: $logDir")
    }
}
