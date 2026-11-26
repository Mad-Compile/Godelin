import groovy.json.JsonSlurper

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Local-only debug signing. Keep secrets.json outside any assets/res directory:
// anything under src/debug/assets would be packaged into the APK.
val debugSecretsFile = rootProject.file("app/src/debug/secrets.json")
val debugSigning = if (debugSecretsFile.isFile) {
    try {
        val root = JsonSlurper().parse(debugSecretsFile) as? Map<*, *>
        val signing = root?.get("signing") as? Map<*, *>
        if (signing?.get("configured") == true) signing else null
    } catch (error: Exception) {
        throw GradleException("Could not read app/src/debug/secrets.json. Check that it contains valid JSON.", error)
    }
} else null

fun signingValue(name: String): String? =
    (debugSigning?.get(name) as? String)?.takeIf { it.isNotBlank() }

val debugKeystorePath = signingValue("keystore_path")
val debugKeystorePassword = signingValue("keystore_password")
val debugKeyAlias = signingValue("key_alias")
val debugKeyPassword = signingValue("key_password")
val hasCustomDebugSigning = listOf(
    debugKeystorePath,
    debugKeystorePassword,
    debugKeyAlias,
    debugKeyPassword
).all { !it.isNullOrBlank() }

val releaseStoreFile = System.getenv("GODELIN_RELEASE_STORE_FILE")
val releaseStorePassword = System.getenv("GODELIN_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = System.getenv("GODELIN_RELEASE_KEY_ALIAS")
val releaseKeyPassword = System.getenv("GODELIN_RELEASE_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.godelin.browser"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.godelin.browser"
        minSdk = 26
        targetSdk = 36
        versionCode = (System.getenv("GODELIN_VERSION_CODE") ?: "2").toInt()
        versionName = System.getenv("GODELIN_VERSION_NAME") ?: "0.1.1-dev"
    }

    signingConfigs {
        if (hasCustomDebugSigning) {
            create("localDebug") {
                storeFile = rootProject.file(requireNotNull(debugKeystorePath))
                storePassword = requireNotNull(debugKeystorePassword)
                keyAlias = requireNotNull(debugKeyAlias)
                keyPassword = requireNotNull(debugKeyPassword)
            }
        }
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(requireNotNull(releaseStoreFile))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        debug {
            // Falls back to Android's standard debug key when local secrets are absent/incomplete.
            if (hasCustomDebugSigning) {
                signingConfig = signingConfigs.getByName("localDebug")
            }
        }
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.webkit:webkit:1.14.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
