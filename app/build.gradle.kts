/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: build.gradle.kts
 * Last Modified: 5/10/2025 10:22
 */

import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "vn.edu.usth.camstudy"
    compileSdk = 36

    defaultConfig {
        applicationId = "vn.edu.usth.camstudy"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Read Azure API keys from local.properties
        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(FileInputStream(localPropertiesFile))
        }
        buildFeatures {
            buildConfig = true
            viewBinding = true
        }
        buildConfigField(
            "String",
            "AZURE_TRANSLATOR_KEY",
            "\"${properties.getProperty("AZURE_TRANSLATOR_KEY", "")}\""
        )
        buildConfigField(
            "String",
            "AZURE_TRANSLATOR_REGION",
            "\"${properties.getProperty("AZURE_TRANSLATOR_REGION", "")}\""
        )
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
    implementation(libs.navigation.fragment) // Handles navigation between Fragments with NavController.
    implementation(libs.navigation.ui) // Connects navigation with UI elements like BottomNavigationView and ActionBar.

    // CameraX dependencies for embedded camera
    implementation(libs.camerax.core) // Core library for CameraX functionalities.
    implementation(libs.camerax.camera2) // Provides Camera2 implementation for CameraX.
    implementation(libs.camerax.lifecycle) // Integrates CameraX with Android lifecycle components.
    implementation(libs.camerax.view) // Provides CameraView for easy camera preview and image capture.


    // TensorFlow Lite for object detection
    implementation("org.tensorflow:tensorflow-lite-support:0.4.0")
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.0")

    // Azure Cognitive Services Translator
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}