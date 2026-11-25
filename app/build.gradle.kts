// SPDX-License-Identifier: GPL-3.0-or-later
import java.util.Properties

plugins {
    id("com.android.application")
}

// Release signing is read from keystore.properties (never committed) or from
// environment variables (for GitHub Actions later). Without either, only debug builds are signed.
val signing = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}
fun signingValue(key: String, env: String): String? = signing.getProperty(key) ?: System.getenv(env)

android {
    namespace = "io.github.chris_v8.removeservicenumbers"
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "io.github.chris_v8.removeservicenumbers"
        // ColorOS 12 is Android 12 (API 31).
        minSdk = 31
        targetSdk = 37
        versionCode = 2
        versionName = "0.1.1"
    }

    signingConfigs {
        val storeFile = signingValue("storeFile", "RSN_KEYSTORE_FILE")
        if (storeFile != null) {
            create("release") {
                this.storeFile = rootProject.file(storeFile)
                storePassword = signingValue("storePassword", "RSN_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "RSN_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "RSN_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            // Keep only the Xposed module descriptor files from Java resources.
            merges += "META-INF/xposed/*"
            excludes += "**"
        }
    }
}

dependencies {
    // Provided by the Xposed framework (Vector) at runtime, so it is not bundled.
    compileOnly("io.github.libxposed:api:102.0.0")
}
