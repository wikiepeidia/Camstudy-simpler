/*
 * Copyright (c) 2025 Pham The Minh
 * All rights reserved.
 * Project: My Application
 * File: HistoryFragment.java
 * Last Modified: 26/9/2025 9:38
 */

package vn.edu.usth.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

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
        recyclerPhotos.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerPhotos.setAdapter(photoAdapter);
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
