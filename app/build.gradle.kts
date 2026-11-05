import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("androidx.room")
    id("com.google.gms.google-services")
    kotlin("kapt")
}

// Secrets are read from local.properties (NOT committed) so nothing sensitive
// lives in source control or build.gradle. See local.properties.example.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// Reads a local.properties value tolerant of the usual copy/paste slips: surrounding spaces or quotes,
// a trailing slash, and characters that would break the generated BuildConfig string literal.
fun configValue(name: String, default: String): String =
    localProperties.getProperty(name, default)
        .trim()
        .removeSurrounding("\"")
        .removeSurrounding("'")
        .trim()
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

android {
    namespace = "com.rentease.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.rentease.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String", "SUPABASE_URL",
            "\"${configValue("SUPABASE_URL", "https://not-configured.invalid")}\""
        )
        buildConfigField(
            "String", "SUPABASE_ANON_KEY",
            "\"${configValue("SUPABASE_ANON_KEY", "")}\""
        )
        buildConfigField(
            "String", "GOOGLE_WEB_CLIENT_ID",
            "\"${configValue("GOOGLE_WEB_CLIENT_ID", "")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        // TopAppBar / DropdownMenu-style Material 3 APIs are still annotated
        // @ExperimentalMaterial3Api in the pinned BOM (material3 1.3.x). An
        // opt-in violation is a compile ERROR, so opt in once here instead of
        // annotating every screen.
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi"
        )
    }

    lint {
        // Fail the build on lint ERRORS (default), never on warnings; skip the
        // release-only vital check so `./gradlew lintDebug` is the single gate.
        abortOnError = true
        warningsAsErrors = false
        checkReleaseBuilds = false
        htmlReport = true
        xmlReport = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        // Required for Robolectric-backed Room/WorkManager unit tests to
        // load the real AndroidManifest.xml and resources.
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Core / Compose
    implementation("androidx.core:core-ktx:1.13.1")
    // AppCompatDelegate.setApplicationLocales() — real per-app language
    // persistence (Settings screen language picker), backed by the OS on
    // API 33+ and by AppCompat's own storage below that, so it survives
    // process death/app restart without RentEase managing that itself.
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(platform("androidx.compose:compose-bom:2024.09.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-compiler:2.52")

    // Networking — Retrofit/OkHttp talk to Supabase's PostgREST + GoTrue REST endpoints
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Realtime (Supabase websocket chat/notifications) — plain OkHttp WebSocket, no extra SDK needed
    // (OkHttp above already provides WebSocket support)

    // Room (offline cache + outbox)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // WorkManager (outbox sync)
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.hilt:hilt-work:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0")

    // Security — encrypted token storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Auth — Google Sign-In via Credential Manager
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Firebase (BoM manages versions) — Cloud Messaging for push notifications
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // NOTE: Google Maps (maps-compose / play-services-maps) was previously
    // declared here but no code ever used it and no Maps API key is configured,
    // so the dependencies were removed rather than implying the feature exists.
    // Listing detail shows distance-to-campus as text (km_from_campus).

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    // Robolectric — lets Room (real SQLite) and WorkManager tests run as
    // fast JVM unit tests (./gradlew testDebugUnitTest) instead of needing
    // an emulator for androidTest. Keeps CI simple and fast.
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.work:work-testing:2.9.1")
    testImplementation("androidx.test:core:1.6.1")
    // Real HTTP round-trips (Retrofit -> OkHttp -> local server) for the REST
    // contract tests; proves paths/headers/JSON bodies, unlike mocked APIs.
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.02"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
