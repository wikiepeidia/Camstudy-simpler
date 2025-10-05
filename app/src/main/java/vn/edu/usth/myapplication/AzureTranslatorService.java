/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: AzureTranslatorService.java
 * Last Modified: 5/10/2025 10:22
 */

package vn.edu.usth.myapplication;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AzureTranslatorService {
    private static final String TAG = "AzureTranslator";

    // Azure Translator endpoint
    private static final String ENDPOINT = "https://api.cognitive.microsofttranslator.com";

    // Read from BuildConfig (loaded from local.properties)
    private static final String API_KEY = BuildConfig.AZURE_TRANSLATOR_KEY;
    private static final String LOCATION = BuildConfig.AZURE_TRANSLATOR_REGION;

    private final OkHttpClient client;
    private final Gson gson;

    public AzureTranslatorService() {
        client = new OkHttpClient();
        gson = new Gson();
    }

    public void translate(String text, String targetLanguage, TranslationCallback callback) {
        new Thread(() -> {
            try {
                String result = translateSync(text, targetLanguage);
                callback.onSuccess(result);
            } catch (Exception e) {
                Log.e(TAG, "Translation error", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void detectLanguage(String text, LanguageDetectionCallback callback) {
        new Thread(() -> {
            try {
                String[] result = detectLanguageSync(text);
                callback.onSuccess(result[0], result[1]);
            } catch (Exception e) {
                Log.e(TAG, "Language detection error", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    public void translateToMultipleLanguages(String text, String[] targetLanguages, TranslationCallback callback) {
        new Thread(() -> {
            try {
                StringBuilder result = new StringBuilder();
                for (String lang : targetLanguages) {
                    String translation = translateSync(text, lang);
                    String langName = getLanguageName(lang);
                    result.append(langName).append(": ").append(translation).append("\n");
                }
                callback.onSuccess(result.toString().trim());
            } catch (Exception e) {
                Log.e(TAG, "Translation error", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    private String[] detectLanguageSync(String text) throws IOException {
        // Build URL for language detection
        String url = ENDPOINT + "/detect?api-version=3.0";

        // Create request body
        RequestItem[] body = new RequestItem[]{new RequestItem(text)};
        String jsonBody = gson.toJson(body);

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(jsonBody, JSON);

        // Build request
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Ocp-Apim-Subscription-Key", API_KEY)
                .addHeader("Ocp-Apim-Subscription-Region", LOCATION)
                .addHeader("Content-Type", "application/json")
                .build();

        // Execute request
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Language detection failed: " + response.code());
            }

            String responseBody = response.body().string();
            DetectionResponse[] detections = gson.fromJson(responseBody, DetectionResponse[].class);

            if (detections != null && detections.length > 0 && detections[0].language != null) {
                String langCode = detections[0].language;
                String langName = getLanguageName(langCode);
                return new String[]{langCode, langName};
            }

            throw new IOException("No language detected");
        }
    }

    private String translateSync(String text, String targetLanguage) throws IOException {
        // Build URL with query parameters
        String url = ENDPOINT + "/translate?api-version=3.0&to=" + targetLanguage;

        // Create request body
        RequestItem[] body = new RequestItem[]{new RequestItem(text)};
        String jsonBody = gson.toJson(body);

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(jsonBody, JSON);

        // Build request
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Ocp-Apim-Subscription-Key", API_KEY)
                .addHeader("Ocp-Apim-Subscription-Region", LOCATION)
                .addHeader("Content-Type", "application/json")
                .build();

        // Execute request
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Translation failed: " + response.code());
            }

            String responseBody = response.body().string();
            TranslationResponse[] translations = gson.fromJson(responseBody, TranslationResponse[].class);

            if (translations != null && translations.length > 0 &&
                    translations[0].translations != null && translations[0].translations.size() > 0) {
                return translations[0].translations.get(0).text;
            }

            throw new IOException("No translation returned");
        }
    }

    private String getLanguageName(String code) {
        switch (code) {
            case "vi":
                return "Vietnamese";
            case "ja":
                return "Japanese";
            case "fr":
                return "French";
            case "es":
                return "Spanish";
            case "de":
                return "German";
            case "zh-Hans":
                return "Chinese (Simplified)";
            case "ko":
                return "Korean";
            case "th":
                return "Thai";
            case "en":
                return "English";
            default:
                return code;
        }
    }

    public interface TranslationCallback {
        void onSuccess(String translatedText);

        void onError(String error);
    }

    public interface LanguageDetectionCallback {
        void onSuccess(String languageCode, String languageName);

        void onError(String error);
    }

    // Request/Response classes
    private static class RequestItem {
        @SerializedName("Text")
        String text;

        RequestItem(String text) {
            this.text = text;
        }
    }

    private static class TranslationResponse {
        @SerializedName("translations")
        List<Translation> translations;
    }

    private static class Translation {
        @SerializedName("text")
        String text;

        @SerializedName("to")
        String to;
    }

    private static class DetectionResponse {
        @SerializedName("language")
        String language;

        @SerializedName("score")
        double score;
    }
}
