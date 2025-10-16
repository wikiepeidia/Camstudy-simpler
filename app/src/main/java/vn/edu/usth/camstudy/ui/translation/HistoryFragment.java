/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: HistoryFragment.java
 * Last Modified: 5/10/2025 3:34
 */

package vn.edu.usth.camstudy.ui.translation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import vn.edu.usth.camstudy.R;
import vn.edu.usth.camstudy.core.TranslationHistoryDatabase;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private TranslationHistoryDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_history, container, false);
        recyclerView = v.findViewById(R.id.recycler_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        db = new TranslationHistoryDatabase(getContext());
        loadHistory();
        return v;
    }

    private void loadHistory() {
        List<String[]> list = db.getAllTranslations();
        recyclerView.setAdapter(new HistoryAdapter(list));
    }
}