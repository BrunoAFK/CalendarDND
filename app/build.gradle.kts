plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.brunoafk.calendardnd"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.brunoafk.calendardnd"
        minSdk = 26
        targetSdk = 36
        versionCode = 10000
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val crashlyticsEnabled = (project.findProperty("crashlyticsEnabled") as String?)
            ?.toBooleanStrictOrNull() ?: true
        buildConfigField("boolean", "CRASHLYTICS_ENABLED", crashlyticsEnabled.toString())

        val analyticsEnabled = (project.findProperty("analyticsEnabled") as String?)
            ?.toBooleanStrictOrNull() ?: true
        buildConfigField("boolean", "ANALYTICS_ENABLED", analyticsEnabled.toString())

        val debugToolsEnabled = (project.findProperty("debugToolsEnabled") as String?)
            ?.toBooleanStrictOrNull() ?: false
        buildConfigField("boolean", "DEBUG_TOOLS_ENABLED", debugToolsEnabled.toString())

        // Manual update defaults (override per flavor).
        buildConfigField("boolean", "MANUAL_UPDATE_ENABLED", "false")
        buildConfigField("String", "MANUAL_UPDATE_URLS", "\"\"")
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("play") {
            dimension = "distribution"
        }
        create("altstore") {
            dimension = "distribution"
        }
        create("manual") {
            dimension = "distribution"
            buildConfigField("boolean", "MANUAL_UPDATE_ENABLED", "true")
            // Comma-separated list of update metadata URLs (primary, fallback).
            buildConfigField(
                "String",
                "MANUAL_UPDATE_URLS",
                "\"https://github.com/BrunoAFK/CalendarDND/releases/latest/download/update.json,https://calendar-dnd.app/update.json\""
            )
        }
    }

    signingConfigs {
        val localProps = java.util.Properties().apply {
            val propsFile = rootProject.file("local.properties")
            if (propsFile.exists()) {
                propsFile.inputStream().use { load(it) }
            }
        }

        fun resolveProp(name: String): String? {
            return (project.findProperty(name) as String?) ?: localProps.getProperty(name)
        }

        val releaseStoreFile = resolveProp("RELEASE_STORE_FILE")
        val releaseStorePassword = resolveProp("RELEASE_STORE_PASSWORD")
        val releaseKeyAlias = resolveProp("RELEASE_KEY_ALIAS")
        val releaseKeyPassword = resolveProp("RELEASE_KEY_PASSWORD")

        if (
            !releaseStoreFile.isNullOrBlank() &&
            !releaseStorePassword.isNullOrBlank() &&
            !releaseKeyAlias.isNullOrBlank() &&
            !releaseKeyPassword.isNullOrBlank()
        ) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfigs.findByName("release")?.let { signingConfig = it }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation("androidx.compose.material:material")
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.accompanist.navigation.animation)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
