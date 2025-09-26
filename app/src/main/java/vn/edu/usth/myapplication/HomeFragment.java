/*
 * Copyright (c) 2025 Pham The Minh
 * All rights reserved.
 * Project: My Application
 * File: HomeFragment.java
 * Last Modified: 26/9/2025 9:38
 */

package vn.edu.usth.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Set up navigation for Take Photo card
        MaterialCardView takePhotoCard = view.findViewById(R.id.card_take_photo);
        takePhotoCard.setOnClickListener(v -> {
            BottomNavigationView bottom = requireActivity().findViewById(R.id.bottom_navigation);
            if (bottom != null) bottom.setSelectedItemId(R.id.nav_camera);
        });

        // Set up navigation for View History card
        MaterialCardView viewHistoryCard = view.findViewById(R.id.card_view_history);
        viewHistoryCard.setOnClickListener(v -> {
            BottomNavigationView bottom = requireActivity().findViewById(R.id.bottom_navigation);
            if (bottom != null) bottom.setSelectedItemId(R.id.nav_history);
        });

        return view;
    }
}
