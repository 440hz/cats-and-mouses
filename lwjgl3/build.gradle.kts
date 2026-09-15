plugins {
    kotlin("jvm")
    application
}

val gdxVersion: String by rootProject.extra

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    implementation("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-desktop")
}

sourceSets["main"].resources.srcDirs("../assets")

// Каталоги raw/ и view/ не должны попадать в jar/dist, но игровые ассеты остаются в jar.
tasks.named<Jar>("jar") {
    exclude("**/raw/**")
    exclude("**/view/**")
}

application {
    mainClass.set("com.catsandmouses.lwjgl3.Lwjgl3Launcher")
    applicationDefaultJvmArgs = listOf(
        "-Dfile.encoding=UTF-8",
        "-Dstdout.encoding=UTF-8",
        "-Dstderr.encoding=UTF-8"
    )
}
