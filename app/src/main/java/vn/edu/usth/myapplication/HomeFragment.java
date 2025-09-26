/*
 * Copyright (c) 2025 Pham The Minh
 * All rights reserved.
 * Project: My Application
 * File: HomeFragment.java
 * Last Modified: 26/9/2025 3:10
 */

package vn.edu.usth.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        MaterialButton btnCamera = view.findViewById(R.id.btnCamera);
        MaterialButton btnHistory = view.findViewById(R.id.btnHistory);

        if (btnCamera == null) {
            Log.e(TAG, "btnCamera is null!");
            return view;
        }
        if (btnHistory == null) {
            Log.e(TAG, "btnHistory is null!");
            return view;
        }

        // Take Photo button - opens embedded camera
        btnCamera.setOnClickListener(v -> {
            Log.d(TAG, "Camera button clicked - opening embedded camera!");
            openEmbeddedCamera();
        });

        // View History button - switches to history tab
        btnHistory.setOnClickListener(v -> {
            Log.d(TAG, "History button clicked!");
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_nav);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_history);
            } else {
                Log.e(TAG, "Bottom navigation not found!");
            }
        });

        Log.d(TAG, "HomeFragment setup complete");
        return view;
    }

    private void openEmbeddedCamera() {
        // Navigate to embedded camera fragment
        EmbeddedCameraFragment cameraFragment = new EmbeddedCameraFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, cameraFragment)
                .addToBackStack("camera")
                .commit();
    }
}
