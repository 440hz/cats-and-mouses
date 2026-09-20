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

// --- Неблокирующие (фоновые) запуски Desktop -------------------------------------
// `run` (JavaExec) и `runDist` (Exec) держат терминал/агента, пока окно открыто. Задачи
// ниже стартуют отдельный процесс и сразу завершаются: окно живёт само, а логи пишутся
// в build/<logs>/<prefix>-{out,err}.log. Аргументы игре: -PgameArgs="--start-map 1/2".

fun Project.registerDetachedRun(
    name: String,
    taskDescription: String,
    logsSubdir: String,
    logPrefix: String,
    extraArgs: List<String> = emptyList()
) {
    tasks.register(name) {
        group = "application"
        description = taskDescription
        dependsOn("classes", ":core:jar")
        val mainClassName = "com.catsandmouses.lwjgl3.Lwjgl3Launcher"
        val assetsDir = rootProject.file("assets")
        val logsDir = layout.buildDirectory.dir(logsSubdir)
        val runtimeClasspath = sourceSets["main"].runtimeClasspath
        // `-PgameArgs="--start-map 1/2 --debug"` — аргументы, передаваемые приложению
        val gameArgs: List<String> = (project.findProperty("gameArgs") as String?)
            ?.split(Regex("\\s+"))?.filter { it.isNotBlank() } ?: emptyList()
        doLast {
            val logDir = logsDir.get().asFile.apply { mkdirs() }
            val javaBin = File(System.getProperty("java.home"), "bin/java").absolutePath
            ProcessBuilder(
                listOf(
                    javaBin,
                    "-Dfile.encoding=UTF-8",
                    "-Dstdout.encoding=UTF-8",
                    "-Dstderr.encoding=UTF-8",
                    "-Dassets.dir=${assetsDir.absolutePath}",
                    "-Dlaunch.mode=d",
                    "-cp", runtimeClasspath.asPath,
                    mainClassName
                ) + extraArgs + gameArgs
            )
                .directory(assetsDir)
                .redirectOutput(File(logDir, "$logPrefix-out.log"))
                .redirectError(File(logDir, "$logPrefix-err.log"))
                .start()
            logger.lifecycle("Запущено в фоне: $name. Логи: $logDir")
        }
    }
}

// Игра в фоне (dev). Аргументы: -PgameArgs="--start-map 1/2 --debug"
registerDetachedRun(
    name = "runBg",
    taskDescription = "Запустить игру в фоне (неблокирующе); аргументы: -PgameArgs=\"--start-map 1/2\"",
    logsSubdir = "run-logs",
    logPrefix = "game"
)

// Редактор карт (dev) в фоне.
registerDetachedRun(
    name = "runEditor",
    taskDescription = "Запустить редактор карт (--editor) в фоне, не блокируя терминал",
    logsSubdir = "editor-logs",
    logPrefix = "editor",
    extraArgs = listOf("--editor")
)

// Собранный дистрибутив в фоне (аналог desktop-run.bat без блокировки).
tasks.register("runDistBg") {
    group = "application"
    description = "Собрать дистрибутив и запустить его в фоне (не блокируя терминал)"
    dependsOn("installDist")
    val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    val binDir = layout.buildDirectory.dir("install/lwjgl3/bin").get().asFile
    val logsDir = layout.buildDirectory.dir("run-logs")
    doLast {
        val logDir = logsDir.get().asFile.apply { mkdirs() }
        val builder = if (isWindows) ProcessBuilder("cmd", "/c", "lwjgl3.bat")
                      else ProcessBuilder("./lwjgl3")
        builder
            .directory(binDir)
            .redirectOutput(File(logDir, "dist-out.log"))
            .redirectError(File(logDir, "dist-err.log"))
            .start()
        logger.lifecycle("Дистрибутив запущен в фоне. Логи: $logDir")
    }
}
