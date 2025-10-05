/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: HistoryFragment.java
 * Last Modified: 5/10/2025 3:34
 */

package vn.edu.usth.myapplication;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerPhotos;
    private LinearLayout emptyState;
    private final List<PhotoEntry> photoList = new ArrayList<>();
    private PhotoHistoryAdapter photoAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        recyclerPhotos = view.findViewById(R.id.recycler_photos);
        emptyState = view.findViewById(R.id.empty_state);

        setupRecyclerView();
        loadPhotos();

        return view;
    }

    private void setupRecyclerView() {
        photoAdapter = new PhotoHistoryAdapter(photoList);
        photoAdapter.setOnPhotoClickListener(this::showPhotoDialog);
        recyclerPhotos.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerPhotos.setAdapter(photoAdapter);
    }

    private void showPhotoDialog(PhotoEntry photoEntry) {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_photo_zoom);

        ImageView imageView = dialog.findViewById(R.id.zoomed_image);
        ImageButton btnBack = dialog.findViewById(R.id.btn_back);
        TextView txtDate = dialog.findViewById(R.id.txt_photo_date);

        // Load the image - getUri() already returns a Uri object
        try {
            Glide.with(requireContext())
                    .load(photoEntry.getUri())
                    .into(imageView);
        } catch (Exception e) {
            imageView.setImageURI(photoEntry.getUri());
        }

        // Set date
        SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        txtDate.setText(fmt.format(photoEntry.getDateTaken()));

        // Back button closes dialog
        btnBack.setOnClickListener(v -> dialog.dismiss());

        // Click on image also closes dialog
        imageView.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void loadPhotos() {
        photoList.clear();
        photoList.addAll(new PhotoDatabase(requireContext()).getAll());

        if (photoList.isEmpty()) {
            showEmptyState();
        } else {
            showPhotoGrid();
        }

        photoAdapter.notifyDataSetChanged();
    }

    private void showEmptyState() {
        emptyState.setVisibility(View.VISIBLE);
        recyclerPhotos.setVisibility(View.GONE);
    }

    private void showPhotoGrid() {
        emptyState.setVisibility(View.GONE);
        recyclerPhotos.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPhotos(); // Refresh photos when fragment resumes
    }
}
