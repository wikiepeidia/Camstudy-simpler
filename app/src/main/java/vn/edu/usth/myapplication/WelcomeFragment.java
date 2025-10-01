/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: WelcomeFragment.java
 * Last Modified: 1/10/2025 9:20
 */

package vn.edu.usth.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

public class WelcomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Button btnGoLogin = view.findViewById(R.id.btnGoLogin);
        btnGoLogin.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            navController.navigate(R.id.nav_login);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // If already logged in, skip welcome
        SharedPreferences prefs = requireActivity().getSharedPreferences("USER_PREFS", Context.MODE_PRIVATE);
        if (prefs.getBoolean("LOGGED_IN", false)) {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            if (navController.getCurrentDestination() != null && navController.getCurrentDestination().getId() == R.id.nav_welcome) {
                NavOptions navOptions = new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_welcome, true)
                        .build();
                navController.navigate(R.id.nav_home, null, navOptions);
            }
        }
    }
}
