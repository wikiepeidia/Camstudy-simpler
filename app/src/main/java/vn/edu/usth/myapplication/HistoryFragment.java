/*
 * Copyright (c) 2025 Pham The Minh
 * All rights reserved.
 * Project: My Application
 * File: HistoryFragment.java
 * Last Modified: 26/9/2025 8:33
 */

package vn.edu.usth.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerPhotos;
    private LinearLayout emptyState;
    private PhotoAdapter photoAdapter;
    private final List<Uri> photoList = new ArrayList<>();

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
        photoAdapter = new PhotoAdapter(photoList);
        recyclerPhotos.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerPhotos.setAdapter(photoAdapter);
    }

    private void loadPhotos() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            showEmptyState();
            return;
        }

        photoList.clear();

        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN
        };

        String selection = MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?";
        String[] selectionArgs = {"%PhotoMagic%"};
        String sortOrder = MediaStore.Images.Media.DATE_TAKEN + " DESC";

        try (Cursor cursor = requireContext().getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder)) {

            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    Uri contentUri = Uri.withAppendedPath(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            String.valueOf(id));
                    photoList.add(contentUri);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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
