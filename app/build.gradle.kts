plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.savetransactionapp"

    // Menggunakan compileSdk versi preview (36) sesuai settingan Anda.
    // Jika nanti ada error, bisa diganti ke 34 atau 35 (versi stabil).
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.savetransactionapp"

        // UBAH KE 26 AGAR BISA JALAN DI HP UMUM (Android 8.0+)
        minSdk = 26

        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
}

dependencies {
    // --- FIREBASE (BOM Version Controls) ---
    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore")

    // --- GOOGLE ML KIT (OCR) ---
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")

    // --- UI & MEDIA HELPERS ---
    implementation("com.github.bumptech.glide:glide:4.16.0") // Loading Gambar
    implementation("com.github.chrisbanes:PhotoView:2.3.0")   // Zoom Gambar
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0") // Grafik (Opsional)

    // --- CAMERA X ---
    val camerax_version = "1.3.0"
    implementation("androidx.camera:camera-core:${camerax_version}")
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
    implementation("androidx.camera:camera-view:${camerax_version}")

    // --- STANDARD ANDROID LIBRARIES ---
    // Menggunakan catalog (libs) agar lebih bersih dan tidak duplikat
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // --- TESTING ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}