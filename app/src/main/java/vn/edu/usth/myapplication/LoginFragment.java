/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: LoginFragment.java
 * Last Modified: 5/10/2025 3:3
 */

package vn.edu.usth.myapplication;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

public class LoginFragment extends Fragment {

    private EditText edtEmail, edtPassword;
    private UserDatabase userDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        userDatabase = new UserDatabase(requireContext());

        edtEmail = view.findViewById(R.id.edtEmail);
        edtPassword = view.findViewById(R.id.edtPassword);
        Button btnLogin = view.findViewById(R.id.btnLogin);
        Button btnRegister = view.findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(getContext(), "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if email is registered
            if (!userDatabase.isEmailRegistered(email)) {
                Toast.makeText(getContext(), "Non-existent account. Please register!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate credentials
            if (userDatabase.validateLogin(email, password)) {
                // Save login session in database
                userDatabase.saveLoginSession(email, true);

                Toast.makeText(getContext(), "Login success!", Toast.LENGTH_SHORT).show();
                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
                NavOptions navOptions = new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_welcome, true)
                        .build();
                navController.navigate(R.id.nav_home, null, navOptions);
            } else {
                // Email exists but password is wrong
                Toast.makeText(getContext(), "Wrong password. Please try again!", Toast.LENGTH_SHORT).show();
            }
        });

        btnRegister.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            navController.navigate(R.id.nav_register);
        });

        return view;
    }
}
