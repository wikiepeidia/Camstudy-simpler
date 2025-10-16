/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: MainActivity.java
 * Last Modified: 5/10/2025 10:43
 */

package vn.edu.usth.camstudy.ui.main;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import vn.edu.usth.camstudy.R;
import vn.edu.usth.camstudy.core.UserDatabase;
import vn.edu.usth.camstudy.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private UserDatabase userDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Theme is now applied in MyApplication.onCreate() - no need to apply here
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userDatabase = new UserDatabase(this);

        // Get the NavHostFragment
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();

            // Check if user is logged in and navigate accordingly
            checkLoginStatus(navController);

            // Setup bottom navigation
            BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
            NavigationUI.setupWithNavController(bottomNavigationView, navController);

            // Hide bottom navigation on Welcome, Login and Register screens
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int id = destination.getId();
                if (id == R.id.nav_welcome || id == R.id.nav_login || id == R.id.nav_register ||
                        id == R.id.nav_photo_preview || id == R.id.nav_translation) {
                    bottomNavigationView.setVisibility(View.GONE);
                } else {
                    bottomNavigationView.setVisibility(View.VISIBLE);
                }
            });

            // Fix bottom navigation item selection to prevent bricking
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();

                // Always navigate to the selected destination, clearing back stack to that destination
                if (itemId == R.id.nav_home) {
                    navController.popBackStack(R.id.nav_home, false);
                    if (navController.getCurrentDestination() == null ||
                            navController.getCurrentDestination().getId() != R.id.nav_home) {
                        navController.navigate(R.id.nav_home);
                    }
                    return true;
                } else if (itemId == R.id.nav_camera) {
                    navController.popBackStack(R.id.nav_home, false);
                    navController.navigate(R.id.nav_camera);
                    return true;
                } else if (itemId == R.id.nav_history) {
                    navController.popBackStack(R.id.nav_home, false);
                    navController.navigate(R.id.nav_history);
                    return true;
                } else if (itemId == R.id.nav_settings) {
                    navController.popBackStack(R.id.nav_home, false);
                    navController.navigate(R.id.nav_settings);
                    return true;
                }
                return false;
            });
        }

        // Hide action bar for cleaner UI like the HTML app
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    private void checkLoginStatus(NavController navController) {
        // Check if user is logged in from database
        if (userDatabase.isLoggedIn()) {
            // User is logged in, navigate to home
            navController.navigate(R.id.nav_home);
        }
        // If not logged in, the default destination (nav_welcome) will be shown
    }
}
