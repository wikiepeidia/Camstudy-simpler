/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: SettingsFragment.java
 * Last Modified: 5/10/2025 10:22
 */

package vn.edu.usth.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {

    private SharedPreferences sharedPreferences;
    private SwitchMaterial switchFlash;
    private SwitchMaterial switchDarkMode;
    private UserDatabase userDatabase;

    // Public method to get flash preference for use in other fragments
    public static boolean isFlashEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("PhotoMagicPrefs", Context.MODE_PRIVATE);
        return prefs.getBoolean("flash_mode", false);
    }

    // Public method to check if dark mode is enabled
    public static boolean isDarkModeEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("PhotoMagicPrefs", Context.MODE_PRIVATE);
        return prefs.getBoolean("dark_mode", false);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        sharedPreferences = requireContext().getSharedPreferences("PhotoMagicPrefs", Context.MODE_PRIVATE);
        userDatabase = new UserDatabase(requireContext());

        switchFlash = view.findViewById(R.id.switch_flash);
        switchDarkMode = view.findViewById(R.id.switch_dark_mode);
        LinearLayout logoutLayout = view.findViewById(R.id.layout_logout);

        // Load saved preferences
        loadPreferences();

        // Set up listeners
        switchFlash.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference("flash_mode", isChecked);
            String message = isChecked ? "Flash enabled" : "Flash disabled";
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference("dark_mode", isChecked);
            String message = isChecked ? "Dark mode enabled" : "Dark mode disabled";
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            applyTheme(isChecked);
        });

        logoutLayout.setOnClickListener(v -> showLogoutDialog());

        return view;
    }

    private void loadPreferences() {
        switchFlash.setChecked(sharedPreferences.getBoolean("flash_mode", false));
        switchDarkMode.setChecked(sharedPreferences.getBoolean("dark_mode", false));
    }

    private void savePreference(String key, boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private void applyTheme(boolean isDarkMode) {
        // Save preference first
        savePreference("dark_mode", isDarkMode);

        // Show message that app needs restart for theme change
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Theme Change")
                .setMessage("Please restart the app to apply the new theme")
                .setPositiveButton("OK", null)
                .show();

        // Don't apply theme immediately to avoid fragment recreation issues
        // Theme will be applied on next app launch via MainActivity
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        // Logout from database
        userDatabase.logout();

        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Navigate to login screen and clear all back stack
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
        NavOptions navOptions = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build();
        navController.navigate(R.id.nav_login, null, navOptions);
    }
}
