/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: MyApplication.java
 * Last Modified: 5/10/2025 10:43
 */

package vn.edu.usth.myapplication;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class DarkMode extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Apply theme as soon as app starts - BEFORE any activities are created
        applyTheme();
    }

    private void applyTheme() {
        SharedPreferences prefs = getSharedPreferences("PhotoMagicPrefs", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);

        android.util.Log.d("MyApplication", "Applying theme - Dark mode: " + isDarkMode);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}

