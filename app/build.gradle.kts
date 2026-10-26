
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ecosdelequilibrio.game"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ecosdelequilibrio.game"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
