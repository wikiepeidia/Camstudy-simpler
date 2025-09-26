/*
 * Copyright (c) 2025 Pham The Minh
 * All rights reserved.
 * Project: My Application
 * File: SettingsFragment.java
 * Last Modified: 26/9/2025 8:33
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
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {

    private SharedPreferences sharedPreferences;
    private SwitchMaterial switchFlash;
    private SwitchMaterial switchGrid;

    // Public methods to get preferences for use in other fragments
    public static boolean isFlashEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("PhotoMagicPrefs", Context.MODE_PRIVATE);
        return prefs.getBoolean("flash_mode", false);
    }

    public static boolean isGridEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("PhotoMagicPrefs", Context.MODE_PRIVATE);
        return prefs.getBoolean("grid_lines", false);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        sharedPreferences = requireContext().getSharedPreferences("PhotoMagicPrefs", Context.MODE_PRIVATE);

        switchFlash = view.findViewById(R.id.switch_flash);
        switchGrid = view.findViewById(R.id.switch_grid);
        LinearLayout clearCacheLayout = view.findViewById(R.id.layout_clear_cache);

        // Load saved preferences
        loadPreferences();

        // Set up listeners
        switchFlash.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference("flash_mode", isChecked);
        });

        switchGrid.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreference("grid_lines", isChecked);
        });

        clearCacheLayout.setOnClickListener(v -> {
            clearCache();
        });

        return view;
    }

    private void loadPreferences() {
        switchFlash.setChecked(sharedPreferences.getBoolean("flash_mode", false));
        switchGrid.setChecked(sharedPreferences.getBoolean("grid_lines", false));
    }

    private void savePreference(String key, boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private void clearCache() {
        try {
            // Clear app cache
            requireContext().getCacheDir().delete();
            Toast.makeText(requireContext(), "Cache cleared successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to clear cache", Toast.LENGTH_SHORT).show();
        }
    }
}
