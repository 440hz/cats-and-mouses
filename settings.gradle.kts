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
        // Зеркало Maven Central для libGDX: repo.maven.apache.org блокирует эту сеть (403)
        maven {
            url = uri("https://maven.aliyun.com/repository/central")
            content {
                includeGroup("com.badlogicgames.gdx")
            }
        }
        mavenCentral()
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    }
}

rootProject.name = "CatsAndMouses"
include(":core")
include(":android")
include(":lwjgl3")
