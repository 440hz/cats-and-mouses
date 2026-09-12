pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        val kotlinVersion: String by settings
        val androidGradlePluginVersion: String by settings
        kotlin("jvm") version kotlinVersion
        kotlin("android") version kotlinVersion
        id("com.android.application") version androidGradlePluginVersion
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    }
}

rootProject.name = "CatsAndMouses"
include(":core")
include(":android")
include(":lwjgl3")
