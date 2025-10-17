/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: HomeFragment.java
 * Last Modified: 17/10/2025 0:56
 */

package vn.edu.usth.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.card.MaterialCardView;

public class HomeFragment extends Fragment {

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    // Navigate to PhotoPreviewFragment with imported image
                    Bundle args = new Bundle();
                    args.putString("photo_uri", uri.toString());
                    args.putLong("timestamp", System.currentTimeMillis());
                    args.putBoolean("is_temp", false); // Imported images are not temp
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
                    navController.navigate(R.id.nav_photo_preview, args);
                } else {
                    Toast.makeText(requireContext(), "No image selected", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Set up navigation for Take Photo card
        MaterialCardView takePhotoCard = view.findViewById(R.id.card_take_photo);
        takePhotoCard.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            navController.navigate(R.id.nav_camera);
        });

        // Set up navigation for View History card
        MaterialCardView viewHistoryCard = view.findViewById(R.id.card_view_history);
        viewHistoryCard.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            navController.navigate(R.id.nav_history);
        });

        // Set up navigation for Import Image card
        MaterialCardView importImageCard = view.findViewById(R.id.card_import_image);
        importImageCard.setOnClickListener(v -> {
            // Launch image picker
            pickImageLauncher.launch("image/*");
        });

        return view;
    }
}
