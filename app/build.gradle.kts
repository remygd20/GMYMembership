plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.remy.gmymembership"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.remy.gmymembership"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

// AQUÍ ESTÁN TUS DEPENDENCIAS
dependencies {
    // --- WorkManager ---
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // --- Coroutines for Firebase ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.1")

    implementation("com.google.android.gms:play-services-maps:18.2.0")
    // --- 1. NAVEGACIÓN (Fragments) ---
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // --- 2. DISEÑO (Material Design) ---
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // --- 3. FIREBASE (Nube) ---
    implementation(platform("com.google.firebase:firebase-bom:32.7.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // --- 4. LIBRERÍAS BASE ---
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.8.0")

    // --- 5. TESTING ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}