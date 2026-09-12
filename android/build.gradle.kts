plugins {
    id("com.android.application")
    kotlin("android")
}

val gdxVersion: String by rootProject.extra

android {
    namespace = "com.catsandmouses"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.catsandmouses"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"
    }

    sourceSets["main"].apply {
        kotlin.srcDirs("src/main/kotlin")
        assets.srcDirs("../assets")
        res.srcDirs("res")
        manifest.srcFile("AndroidManifest.xml")
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
}
