plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.sipekat.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sipekat.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 9
        versionName = "3.0.2"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)

    // Lifecycle & ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // RecyclerView & SwipeRefreshLayout
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)

    // Retrofit & OkHttp (menggantikan http package Dart)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Gson
    implementation(libs.gson)

    // Coroutines (menggantikan async/await Dart)
    implementation(libs.kotlinx.coroutines.android)

    // SharedPreferences (menggantikan shared_preferences package Dart)
    implementation(libs.androidx.datastore.preferences)

    // Room (SQLite — menggantikan sqflite Dart)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    // Image loading (Glide — untuk tampilkan gambar dari URL)
    implementation(libs.glide)
    kapt(libs.glide.compiler)

    // CameraX (menggantikan image_picker Dart)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // GPS / Location (menggantikan geolocator Dart)
    implementation(libs.play.services.location)

    // ML Kit Text Recognition (menggantikan google_mlkit_text_recognition Dart)
    implementation(libs.mlkit.text.recognition)

    // Connectivity (menggantikan connectivity_plus Dart)
    implementation(libs.connectivity)

    // EXIF (menggantikan exif Dart)
    implementation(libs.exifinterface)

    // URL Launcher (intents untuk WA, telepon, maps)
    // Di Android native, ini cukup pakai Intent standar

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
