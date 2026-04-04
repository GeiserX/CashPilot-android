plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.cashpilot.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cashpilot.android"
        minSdk = 26 // Android 8.0 — NotificationListenerService, NetworkStatsManager
        targetSdk = 35

        val ciVersionName = findProperty("VERSION_NAME") as String? ?: "0.1.0"
        val ciVersionCode = (findProperty("VERSION_CODE") as String?)?.toIntOrNull() ?: 1
        versionCode = ciVersionCode
        versionName = ciVersionName
    }

    signingConfigs {
        create("release") {
            val keystorePath = findProperty("CASHPILOT_KEYSTORE_PATH") as String?
                ?: System.getenv("CASHPILOT_KEYSTORE_PATH")
            val keystorePass = findProperty("CASHPILOT_KEYSTORE_PASSWORD") as String?
                ?: System.getenv("CASHPILOT_KEYSTORE_PASSWORD")
            val keyAliasValue = findProperty("CASHPILOT_KEY_ALIAS") as String?
                ?: System.getenv("CASHPILOT_KEY_ALIAS")
            val keyPass = findProperty("CASHPILOT_KEY_PASSWORD") as String?
                ?: System.getenv("CASHPILOT_KEY_PASSWORD")

            if (keystorePath != null && keystorePass != null && keyAliasValue != null && keyPass != null) {
                storeFile = file(keystorePath)
                storePassword = keystorePass
                keyAlias = keyAliasValue
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseConfig = signingConfigs.findByName("release")
            if (releaseConfig?.storeFile != null) {
                signingConfig = releaseConfig
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.androidx.ui.tooling)
}
