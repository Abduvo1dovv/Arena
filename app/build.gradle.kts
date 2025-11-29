plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.arena"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.arena"
        minSdk = 25 //24 edi eskisi
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true //false
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8  // <--- SHU KERAK
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.firebase.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))

    // 2. Firebase modullari (Versiya raqamisiz!)
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")

    // ----------------------------------------------------------------
    // 3. UI VA NAVIGATION (Sizdagi bor kodlar)
    // ----------------------------------------------------------------
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Qo'shimcha ikonka va navigatsiya
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.material:material-icons-extended:1.6.3")

    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // 2. BIOMETRIC (Barmoq izi)
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    implementation("androidx.activity:activity-compose:1.8.2") // Yoki 1.9.0
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    val cameraVersion = "1.4.1"
    implementation("androidx.camera:camera-core:$cameraVersion")
    implementation("androidx.camera:camera-camera2:$cameraVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraVersion")
    implementation("androidx.camera:camera-video:$cameraVersion")
    implementation("androidx.camera:camera-view:$cameraVersion") // OutputFileOptions shu yerda!
    implementation("androidx.camera:camera-extensions:$cameraVersion")

    // 2. RUXSATLAR (Permissions)
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // 3. RASMNI KO'RSATISH (Coil)
    implementation("io.coil-kt:coil-compose:2.6.0")

    implementation("androidx.wear.compose:compose-material:1.3.0")

    implementation("androidx.compose.runtime:runtime-livedata")

    implementation("com.google.firebase:firebase-storage")
    implementation("com.cloudinary:cloudinary-android:2.3.1")

    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")
    implementation("androidx.media3:media3-common:1.2.0")

}