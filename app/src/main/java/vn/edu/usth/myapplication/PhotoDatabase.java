/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: PhotoDatabase.java
 * Last Modified: 1/10/2025 4:38
 */

package vn.edu.usth.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PhotoDatabase {
    private static final String PREF_NAME = "PhotoMagicDB";
    private static final String KEY_ENTRIES = "photo_entries"; // each entry: uri|timestamp

    private final SharedPreferences sharedPreferences;

    public PhotoDatabase(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void savePhoto(String uriString, long timestamp) {
        Set<String> existing = sharedPreferences.getStringSet(KEY_ENTRIES, new HashSet<>());
        Set<String> updated = new HashSet<>(existing);
        updated.add(uriString + "|" + timestamp);
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
        Collections.sort(result, (a, b) -> Long.compare(b.getDateTaken(), a.getDateTaken()));
        return result;
    }

    public void clear() {
        sharedPreferences.edit().remove(KEY_ENTRIES).apply();
    }
}
