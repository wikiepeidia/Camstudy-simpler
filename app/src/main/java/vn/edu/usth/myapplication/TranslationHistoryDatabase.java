/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: TranslationHistoryDatabase.java
 * Last Modified: 10/10/2025 9:51
 */

package vn.edu.usth.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TranslationHistoryDatabase {
    private static final String PREF_NAME = "TranslationHistoryDB";
    private static final String KEY_ENTRIES = "translation_entries"; // format: sourceText|translatedText|timestamp
    private static final int MAX_HISTORY = 50; // Maximum number of translations to keep

    private final SharedPreferences sharedPreferences;

    public TranslationHistoryDatabase(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveTranslation(String sourceText, String translatedText) {
        long timestamp = System.currentTimeMillis();
        Set<String> existing = sharedPreferences.getStringSet(KEY_ENTRIES, new HashSet<>());
        List<String> allEntries = new ArrayList<>(existing);

        // Add new entry at the beginning (newest first)
        String newEntry = sourceText + "|" + translatedText + "|" + timestamp;
        allEntries.add(0, newEntry);

        // Keep only the latest MAX_HISTORY entries
        if (allEntries.size() > MAX_HISTORY) {
            allEntries = allEntries.subList(0, MAX_HISTORY);
        }

        // Save back to SharedPreferences
        Set<String> updated = new HashSet<>(allEntries);
        sharedPreferences.edit().putStringSet(KEY_ENTRIES, updated).apply();
    }

    public List<String[]> getAllTranslations() {
        Set<String> stored = sharedPreferences.getStringSet(KEY_ENTRIES, new HashSet<>());
        List<String[]> result = new ArrayList<>();

        for (String entry : stored) {
            String[] parts = entry.split("\\|", 3);
            if (parts.length >= 2) {
                // Return [sourceText, translatedText]
                result.add(new String[]{parts[0], parts[1]});
            }
        }

        return result;
    }

    public void clear() {
        sharedPreferences.edit().remove(KEY_ENTRIES).apply();
    }
}

