/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: SettingsFragment.java
 * Last Modified: 5/10/2025 2:54
 */

package vn.edu.usth.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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

import java.io.File;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private SharedPreferences sharedPreferences;
    private SwitchMaterial switchFlash;

    // Public method to get flash preference for use in other fragments
    public static boolean isFlashEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("PhotoMagicPrefs", Context.MODE_PRIVATE);
        return prefs.getBoolean("flash_mode", false);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        sharedPreferences = requireContext().getSharedPreferences("PhotoMagicPrefs", Context.MODE_PRIVATE);

        switchFlash = view.findViewById(R.id.switch_flash);
        LinearLayout clearCacheLayout = view.findViewById(R.id.layout_clear_cache);
        LinearLayout logoutLayout = view.findViewById(R.id.layout_logout);

        // Load saved preferences
        loadPreferences();

        // Set up listeners
        switchFlash.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference("flash_mode", isChecked);
            String message = isChecked ? "Flash enabled" : "Flash disabled";
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });

        clearCacheLayout.setOnClickListener(v -> clearCache());

        logoutLayout.setOnClickListener(v -> showLogoutDialog());

        return view;
    }

    private void loadPreferences() {
        switchFlash.setChecked(sharedPreferences.getBoolean("flash_mode", false));
    }

    private void savePreference(String key, boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private void clearCache() {
        try {
            File cacheDir = requireContext().getCacheDir();
            File externalCacheDir = requireContext().getExternalCacheDir();

            long deletedSize = 0;

            // Clear internal cache
            if (cacheDir != null && cacheDir.isDirectory()) {
                deletedSize += deleteDir(cacheDir);
            }

            // Clear external cache
            if (externalCacheDir != null && externalCacheDir.isDirectory()) {
                deletedSize += deleteDir(externalCacheDir);
            }

            String sizeStr = formatFileSize(deletedSize);
            Toast.makeText(requireContext(), "Cache cleared: " + sizeStr, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("SettingsFragment", "Failed to clear cache", e);
            Toast.makeText(requireContext(), "Failed to clear cache: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private long deleteDir(File dir) {
        long deletedSize = 0;
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    File file = new File(dir, child);
                    if (file.isDirectory()) {
                        deletedSize += deleteDir(file);
                    } else {
                        long size = file.length();
                        if (file.delete()) {
                            deletedSize += size;
                        }
                    }
                }
            }
        }
        return deletedSize;
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        int exp = (int) (Math.log(size) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format(Locale.US, "%.1f %sB", size / Math.pow(1024, exp), pre);
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
        // Clear user login session
        SharedPreferences userPrefs = requireActivity().getSharedPreferences("USER_PREFS", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = userPrefs.edit();
        editor.putBoolean("LOGGED_IN", false);
        editor.apply();

        // Clear app preferences
        SharedPreferences.Editor appEditor = sharedPreferences.edit();
        appEditor.clear();
        appEditor.apply();

        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Navigate to welcome screen
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
        NavOptions navOptions = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build();
        navController.navigate(R.id.nav_welcome, null, navOptions);
    }
}
