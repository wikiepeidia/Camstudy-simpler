/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: SettingsFragment.java
 * Last Modified: 17/10/2025 2:18
 */

package vn.edu.usth.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
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
    private SwitchMaterial switchDarkMode;
    private UserDatabase userDatabase;
    private boolean isDarkModeChanging = false;

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        sharedPreferences = requireContext().getSharedPreferences("PhotoMagicPrefs", Context.MODE_PRIVATE);
        userDatabase = new UserDatabase(requireContext());
        LinearLayout btnFeedback = view.findViewById(R.id.btnFeedback);
        switchDarkMode = view.findViewById(R.id.switch_dark_mode);
        LinearLayout logoutLayout = view.findViewById(R.id.layout_logout);
        TextView txtVersion = view.findViewById(R.id.txt_version);

        // Set version dynamically from BuildConfig
        txtVersion.setText("Version " + BuildConfig.VERSION_NAME);

        // Load saved preferences
        loadPreferences();

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Prevent recursive calls
            if (isDarkModeChanging) {
                return;
            }

            // Get current saved dark mode preference
            boolean currentDarkMode = sharedPreferences.getBoolean("dark_mode", false);

            // If the value actually changed, show restart dialog
            if (currentDarkMode != isChecked) {
                showRestartDialog(isChecked);
            }
        });
        btnFeedback.setOnClickListener(v -> {
            try {
                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.setType("message/rfc822");
                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"kingnopro0002@gmail.com"});
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback for CamStudy App");
                emailIntent.putExtra(Intent.EXTRA_TEXT, "Your feedback here...");
                startActivity(Intent.createChooser(emailIntent, "Send feedback via..."));
            } catch (Exception e) {
                Toast.makeText(getContext(), "No email app found!", Toast.LENGTH_SHORT).show();
            }
        });

        logoutLayout.setOnClickListener(v -> showLogoutDialog());

        return view;
    }

    private void loadPreferences() {
        switchDarkMode.setChecked(sharedPreferences.getBoolean("dark_mode", false));
    }

    private void savePreference(String key, boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private void showRestartDialog(boolean newDarkModeValue) {
        String themeMode = newDarkModeValue ? "Dark Mode" : "Light Mode";

        new AlertDialog.Builder(requireContext())
                .setTitle("Restart Required")
                .setMessage("The app needs to restart to apply " + themeMode + ". Do you want to restart now?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Save the new preference FIRST
                    savePreference("dark_mode", newDarkModeValue);

                    // Force commit to ensure it's saved immediately
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("dark_mode", newDarkModeValue);
                    editor.commit(); // Use commit() instead of apply() for immediate write

                    // Log for debugging
                    android.util.Log.d("SettingsFragment", "Saved dark mode preference: " + newDarkModeValue);

                    // Small delay to ensure preference is saved
                    new android.os.Handler().postDelayed(() -> {
                        restartApp();
                    }, 100);
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // Revert the switch to its previous state
                    isDarkModeChanging = true;
                    switchDarkMode.setChecked(!newDarkModeValue);
                    isDarkModeChanging = false;

                    Toast.makeText(requireContext(), "Theme change cancelled", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false) // Prevent dismissing by clicking outside
                .show();
    }

    private void restartApp() {
        try {
            android.util.Log.d("SettingsFragment", "Restarting app...");

            // Get the app's launch intent
            Intent intent = requireActivity().getPackageManager()
                    .getLaunchIntentForPackage(requireActivity().getPackageName());

            if (intent != null) {
                // Clear all activities and start fresh
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK);

                // Finish current activity first
                requireActivity().finish();

                // Start the activity
                startActivity(intent);

                // Kill the process to ensure complete restart
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        } catch (Exception e) {
            android.util.Log.e("SettingsFragment", "Error restarting app", e);
            Toast.makeText(requireContext(),
                    "Please restart the app manually to apply theme changes",
                    Toast.LENGTH_LONG).show();
        }
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