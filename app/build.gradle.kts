import groovy.json.JsonSlurper

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val debugSigningJson = rootProject.file("app/src/debug/secrets.json")
    .takeIf { it.isFile }
    ?.let { JsonSlurper().parse(it) as? Map<*, *> }
    ?: throw GradleException("Missing or invalid app/src/debug/secrets.json")
val configuredKeystorePath = debugSigningJson["keystore_path"] as? String
    ?: throw GradleException("Missing keystore_path in debug secrets")
val debugKeystorePath = configuredKeystorePath
    .replaceFirst("^~", System.getProperty("user.home"))
val debugKeystoreFile = file(debugKeystorePath)
val debugStorePassword = debugSigningJson["store_password"] as? String
    ?: throw GradleException("Missing store_password in debug secrets")
val debugKeyAlias = debugSigningJson["key_alias"] as? String
    ?: throw GradleException("Missing key_alias in debug secrets")
val debugKeyPassword = debugSigningJson["key_password"] as? String
    ?: throw GradleException("Missing key_password in debug secrets")
if (!debugKeystoreFile.isFile) {
    throw GradleException("Development keystore not found at $debugKeystoreFile")
}

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
        create("godelinDev") {
            storeFile = debugKeystoreFile
            storeType = debugSigningJson["store_type"] as? String ?: "JKS"
            storePassword = debugStorePassword
            keyAlias = debugKeyAlias
            keyPassword = debugKeyPassword
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
            signingConfig = signingConfigs.getByName("godelinDev")
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
