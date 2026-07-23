plugins {
    id("com.android.application")
}

android {
    namespace = "com.signaltrace.portfolio"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.signaltrace.portfolio"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "0.7.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
