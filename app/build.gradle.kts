plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp") version "2.0.21-1.0.25"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
    id("kotlin-parcelize")
}

android {
    namespace = "com.dravenmiller.swolescroll"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.dravenmiller.swolescroll"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
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
        viewBinding = true
    }
    buildToolsVersion = "36.1.0"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Bill of Materials
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Material 3
    implementation("androidx.compose.material3:material3")



    // Preview & Tooling for (@Preview to work)
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")


    // ViewModel stores Exercise Info
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.4")

    // Room Database holds Exercise Info on App Termination
    implementation ("androidx.room:room-runtime:2.8.3")
    ksp ("androidx.room:room-compiler:2.8.3")
    implementation ("androidx.room:room-ktx:2.8.3")

    // RecyclerView displays Exercise Info
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // To hold lists inside of lists
    implementation("com.google.code.gson:gson:2.10.1")

    // add icons
    implementation("androidx.compose.material:material-icons-extended")
}