plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.edgellm"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.edgellm"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures { compose = true }

    // REQUIRED: prevents Ktor duplicate-file build error
    packaging {
        resources {
            excludes += setOf(
                "META-INF/ASL-2.0.txt", "META-INF/LGPL-2.1.txt",
                "META-INF/DEPENDENCIES", "META-INF/LICENSE",
                "META-INF/LICENSE.txt", "META-INF/NOTICE",
                "META-INF/NOTICE.txt", "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties",
                "draftv4/schema", "draftv3/schema"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // ── Core Android ──
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    // ── GGUF Engine: kotlinllamacpp (llama.cpp Kotlin bindings, like PocketPal uses) ──
    // Source: https://github.com/ljcamargo/kotlinllamacpp
    // Features: GGUF models, FD-based scoped storage bypass, multimodal (LLaVA), streaming
    implementation("io.github.ljcamargo:llamacpp-kotlin:0.4.0")

    // ── LiteRT-LM Engine: Official Google SDK (exactly as used in AI Edge Gallery) ──
    // Source: https://ai.google.dev/edge/litert-lm/android
    // 'latest.release' IS valid for Google Maven per official docs
    // Features: .litertlm models, GPU/NPU acceleration, Gemma 4 thinking mode, vision
    implementation("com.google.ai.edge.litertlm:litertlm-android:latest.release")

    // ── Ktor: Local OpenAI-compatible API server (CIO engine, lightweight for Android) ──
    implementation("io.ktor:ktor-server-cio:3.2.3")
    implementation("io.ktor:ktor-server-core:3.2.3")
    implementation("io.ktor:ktor-server-content-negotiation:3.2.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.2.3")

    // ── Camera (Ask Image feature) ──
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // ── Compose ──
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    // ── Material Components bridge (required for themes.xml parent theme) ──
    implementation("com.google.android.material:material:1.12.0")

    // ── Room (model + chat history database) ──
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ── Coroutines + Serialization + HTTP ──
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // ── Coil: image loading for Ask Image preview ──
    implementation("io.coil-kt:coil-compose:2.7.0")

    // ── Markwon: Markdown rendering for skill cards ──
    implementation("io.noties.markwon:core:4.6.2")
}
