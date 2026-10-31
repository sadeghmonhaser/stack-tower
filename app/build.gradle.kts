plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.stacktower"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.stacktower"
        minSdk = 21
        targetSdk = 34
        versionCode = 3
        versionName = "1.2"
    }

    signingConfigs {
        create("release") {
            val ksPath = System.getenv("KEYSTORE_FILE")
            val ksFile = when {
                !ksPath.isNullOrEmpty() -> {
                    val f = file(ksPath)
                    if (f.isAbsolute) f else rootProject.file(ksPath)
                }
                rootProject.file("release-signing/stacktower-release.jks").exists() -> {
                    rootProject.file("release-signing/stacktower-release.jks")
                }
                rootProject.file("release.jks").exists() -> {
                    rootProject.file("release.jks")
                }
                else -> null
            }

            val pass = System.getenv("KEY_PASSWORD") ?: if (ksFile != null && rootProject.file("release-signing/password.txt").exists()) {
                rootProject.file("release-signing/password.txt").readText().trim()
            } else null

            if (ksFile != null && ksFile.exists() && !pass.isNullOrEmpty()) {
                storeFile = ksFile
                storePassword = pass
                keyAlias = "stacktower"
                keyPassword = pass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
}
