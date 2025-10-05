/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: PhotoDatabase.java
 * Last Modified: 5/10/2025 3:34
 */

package vn.edu.usth.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PhotoDatabase {
    private static final String PREF_NAME = "PhotoMagicDB";
    private static final String KEY_ENTRIES = "photo_entries"; // each entry: uri|timestamp
    private static final int MAX_HISTORY = 20; // Maximum number of photos to keep

    private final SharedPreferences sharedPreferences;

    public PhotoDatabase(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void savePhoto(String uriString, long timestamp) {
        Set<String> existing = sharedPreferences.getStringSet(KEY_ENTRIES, new HashSet<>());
        List<PhotoEntry> allPhotos = new ArrayList<>();

        // Convert existing entries to PhotoEntry objects
        for (String s : existing) {
            String[] parts = s.split("\\|", 2);
            if (parts.length == 2) {
                try {
                    long ts = Long.parseLong(parts[1]);
                    allPhotos.add(new PhotoEntry(parts[0], ts));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // Add new photo
        allPhotos.add(new PhotoEntry(uriString, timestamp));

        // Sort by timestamp (newest first)
        allPhotos.sort((a, b) -> Long.compare(b.getDateTaken(), a.getDateTaken()));

        // Keep only the latest MAX_HISTORY photos
        if (allPhotos.size() > MAX_HISTORY) {
            allPhotos = allPhotos.subList(0, MAX_HISTORY);
        }

        // Convert back to strings and save
        Set<String> updated = new HashSet<>();
        for (PhotoEntry entry : allPhotos) {
            updated.add(entry.getUri() + "|" + entry.getDateTaken());
        }

        sharedPreferences.edit().putStringSet(KEY_ENTRIES, updated).apply();
    }

    public List<PhotoEntry> getAll() {
        Set<String> stored = sharedPreferences.getStringSet(KEY_ENTRIES, new HashSet<>());
        List<PhotoEntry> result = new ArrayList<>();
        for (String s : stored) {
            String[] parts = s.split("\\|", 2);
            if (parts.length == 2) {
                try {
                    long ts = Long.parseLong(parts[1]);
                    result.add(new PhotoEntry(parts[0], ts));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        // newest first
        result.sort((a, b) -> Long.compare(b.getDateTaken(), a.getDateTaken()));
        return result;
    }

    public void clear() {
        sharedPreferences.edit().remove(KEY_ENTRIES).apply();
    }
}
