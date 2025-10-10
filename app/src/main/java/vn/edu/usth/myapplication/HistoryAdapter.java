/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: HistoryAdapter.java
 * Last Modified: 10/10/2025 9:51
 */

package vn.edu.usth.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<String[]> items; // Each item is [sourceText, translatedText]

    public HistoryAdapter(List<String[]> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_translation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String[] entry = items.get(position);
        if (entry.length >= 2) {
            holder.sourceText.setText(entry[0]);
            holder.translatedText.setText(entry[1]);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView sourceText;
        TextView translatedText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            sourceText = itemView.findViewById(R.id.et_source_text);
            translatedText = itemView.findViewById(R.id.et_translated_text);
        }
    }
}

