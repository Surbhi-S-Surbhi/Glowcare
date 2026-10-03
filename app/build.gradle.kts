import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
     id("com.google.gms.google-services")
}

/**
 * Real Google Sign-In Web OAuth Client ID, resolved in priority order:
 *   1. Gradle property:   -PgoogleWebClientId=xxxx.apps.googleusercontent.com
 *   2. Environment var:   GOOGLE_WEB_CLIENT_ID=xxxx.apps.googleusercontent.com
 *   3. local.properties:  GOOGLE_WEB_CLIENT_ID=xxxx.apps.googleusercontent.com
 *      (GlowCare/local.properties — git-ignored, preferred for daily use)
 * See README_GOOGLE_SIGNIN.md for the one-time Google Cloud Console setup.
 */
fun resolveGoogleWebClientId(): String {
    (project.findProperty("googleWebClientId") as? String)
        ?.takeIf { it.isNotBlank() }?.let { return it.trim() }
    System.getenv("GOOGLE_WEB_CLIENT_ID")
        ?.takeIf { it.isNotBlank() }?.let { return it.trim() }
    val localProps = Properties()
    val lp = rootProject.file("local.properties")
    if (lp.exists()) lp.inputStream().use { localProps.load(it) }
    return localProps.getProperty("GOOGLE_WEB_CLIENT_ID")?.trim().orEmpty()
}

android {
    namespace = "com.glowcare.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.glowcare.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${resolveGoogleWebClientId()}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.navigation:navigation-compose:2.8.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Google Sign-In (Credential Manager) + Play Billing subscriptions
    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.android.billingclient:billing-ktx:7.1.1")
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    val camerax = "1.3.4"
    implementation("androidx.camera:camera-core:$camerax")
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-video:$camerax")
    implementation("androidx.camera:camera-view:$camerax")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("com.google.guava:guava:33.3.1-android")
}
