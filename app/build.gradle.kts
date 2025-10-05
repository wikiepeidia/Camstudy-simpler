/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: build.gradle.kts
 * Last Modified: 5/10/2025 5:27
 */

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "vn.edu.usth.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "vn.edu.usth.myapplication"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        mlModelBinding = true
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

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Navigation components
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // CameraX dependencies for embedded camera
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // Image loading library
    implementation(libs.glide)

    // TensorFlow Lite for object detection
    implementation("org.tensorflow:tensorflow-lite-support:0.4.0")
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.0")

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}