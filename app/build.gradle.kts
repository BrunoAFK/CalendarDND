import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
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
        versionCode = 11000
        versionName = "1.10"

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

        val testerTelemetryDefault = (project.findProperty("testerTelemetryDefault") as String?)
            ?.toBooleanStrictOrNull() ?: false
        buildConfigField("boolean", "TESTER_TELEMETRY_DEFAULT", testerTelemetryDefault.toString())

        // Manual update defaults (override per flavor).
        buildConfigField("boolean", "MANUAL_UPDATE_ENABLED", "false")
        buildConfigField("String", "MANUAL_UPDATE_URLS", "\"\"")
        buildConfigField("String", "ALLOWED_SIGNER_SHA256", "\"\"")
        buildConfigField("boolean", "FIREBASE_ENABLED", "true")
        buildConfigField("String", "UMAMI_BASE_URL", "\"https://stats.pavelja.me\"")
        buildConfigField("String", "UMAMI_WEBSITE_ID", "\"f27a86c4-dd07-4ede-8e5e-8d9fd246e2c5\"")
    }

    val manualSignerSha256 = (project.findProperty("manualSignerSha256") as String?)
        ?.trim()
        ?.replace("\"", "")
        .orEmpty()

    flavorDimensions += "distribution"
    productFlavors {
        create("play") {
            dimension = "distribution"
            buildConfigField("boolean", "TESTER_TELEMETRY_DEFAULT", "true")
        }
        create("fdroid") {
            dimension = "distribution"
            buildConfigField("boolean", "FIREBASE_ENABLED", "false")
            buildConfigField("boolean", "CRASHLYTICS_ENABLED", "false")
            buildConfigField("boolean", "ANALYTICS_ENABLED", "false")
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
            buildConfigField(
                "String",
                "ALLOWED_SIGNER_SHA256",
                "\"$manualSignerSha256\""
            )
        }
    }

    signingConfigs {
        val localProps = Properties().apply {
            val propsFile = rootProject.file("local.properties")
            if (propsFile.exists()) {
                propsFile.inputStream().use { this.load(it) }
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
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "Calendar DND (Debug)")
        }
        release {
            isMinifyEnabled = true
            ndk {
                debugSymbolLevel = "FULL"
            }
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

    applicationVariants.all {
        if (flavorName == "fdroid") {
            val variantName = name.replaceFirstChar { it.uppercaseChar() }
            tasks.matching { it.name == "process${variantName}GoogleServices" }
                .configureEach { enabled = false }
            tasks.matching { it.name == "injectCrashlyticsMappingFileId${variantName}" }
                .configureEach { enabled = false }
            tasks.matching { it.name == "uploadCrashlyticsMappingFile${variantName}" }
                .configureEach { enabled = false }
        }
    }
}

dependencies {
    val firebaseBom = platform("com.google.firebase:firebase-bom:34.7.0")
    listOf("play", "manual").forEach { flavor ->
        add("${flavor}Implementation", firebaseBom)
        add("${flavor}Implementation", "com.google.firebase:firebase-analytics")
        add("${flavor}Implementation", "com.google.firebase:firebase-crashlytics")
        add("${flavor}Implementation", "com.google.firebase:firebase-messaging")
        add("${flavor}Implementation", "com.google.firebase:firebase-perf")
    }
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
