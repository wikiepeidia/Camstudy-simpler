/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: RegisterFragment.java
 * Last Modified: 5/10/2025 3:3
 */

package vn.edu.usth.camstudy.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
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
import androidx.navigation.Navigation;

import vn.edu.usth.camstudy.R;
import vn.edu.usth.camstudy.core.UserDatabase;

public class RegisterFragment extends Fragment {

    private EditText edtEmail, edtPassword, edtConfirmPassword;
    private UserDatabase userDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        userDatabase = new UserDatabase(requireContext());
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        edtEmail = view.findViewById(R.id.edtEmail);
        edtPassword = view.findViewById(R.id.edtPassword);
        edtConfirmPassword = view.findViewById(R.id.edtConfirmPass);
        Button btnRegister = view.findViewById(R.id.btnRegister);
        Button btnBack = view.findViewById(R.id.btnBackToLogin);

        btnRegister.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();
            String confirmPassword = edtConfirmPassword.getText().toString().trim();

            // Comprehensive validation
            if (TextUtils.isEmpty(email)) {
                edtEmail.setError("Email is required");
                edtEmail.requestFocus();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edtEmail.setError("Please enter a valid email");
                edtEmail.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(password)) {
                edtPassword.setError("Password is required");
                edtPassword.requestFocus();
                return;
            }

            if (password.length() < 6) {
                edtPassword.setError("Password must be at least 6 characters");
                edtPassword.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(confirmPassword)) {
                edtConfirmPassword.setError("Please confirm your password");
                edtConfirmPassword.requestFocus();
                return;
            }

            if (!password.equals(confirmPassword)) {
                edtConfirmPassword.setError("Passwords do not match");
                edtConfirmPassword.requestFocus();
                return;
            }

            // Check if email already exists using database
            if (userDatabase.checkEmailExists(email)) {
                edtEmail.setError("Email already registered");
                edtEmail.requestFocus();
                Toast.makeText(getContext(), "This email is already registered. Please login instead.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Register user in database
            if (userDatabase.registerUser(email, password)) {
                Toast.makeText(getContext(), "Registration successful! Please login", Toast.LENGTH_SHORT).show();

                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
                navController.popBackStack();
            } else {
                Toast.makeText(getContext(), "Registration failed. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });

        btnBack.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            navController.popBackStack();
        });
    }
}
