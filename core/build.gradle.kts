plugins {
    kotlin("jvm")
}

val gdxVersion: String by rootProject.extra

dependencies {
    api("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation(kotlin("test"))
    // Headless-бэкенд: даёт HeadlessFiles, чтобы Gdx.files.internal(...) читал реальные assets
    testImplementation("com.badlogicgames.gdx:gdx-backend-headless:$gdxVersion")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    // Тесты читают настоящие ассеты через Gdx.files.internal("maps/..."): рабочая папка — assets
    workingDir = rootProject.file("assets")
    // maps.json генерируется Gradle-задачей из корневого проекта
    dependsOn(rootProject.tasks.named("generateMapsManifest"))
    // Абсолютный корень проекта — для чтения фикстур tests/ из Kotlin-тестов
    systemProperty("project.root", rootProject.projectDir.absolutePath)
    testLogging {
        events("passed", "skipped", "failed")
    }
}
