/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: AzureTranslatorService.java
 * Last Modified: 17/10/2025 0:56
 */

package vn.edu.usth.myapplication;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class AzureTranslatorService {

    private static final String TAG = "AzureTranslator";
    private static final String ENDPOINT = "https://api.cognitive.microsofttranslator.com";
    private static final String API_KEY = BuildConfig.AZURE_TRANSLATOR_KEY;
    private static final String REGION  = BuildConfig.AZURE_TRANSLATOR_REGION;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private final Gson gson = new Gson();

    public void translate(@NonNull String text,
                          @NonNull String target,
                          @NonNull TranslationCallback cb) {

        String url = ENDPOINT + "/translate?api-version=3.0&to=" + target;
        String bodyJson = gson.toJson(new RequestItem[]{ new RequestItem(text) });
        RequestBody body = RequestBody.create(bodyJson, JSON);

        Request req = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Ocp-Apim-Subscription-Key", API_KEY)
                .addHeader("Ocp-Apim-Subscription-Region", REGION)
                .addHeader("Content-Type", "application/json")
                .build();

        http.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Network error", e);
                cb.onError("Network error: " + e.getMessage());
            }

            @Override public void onResponse(@NonNull Call call, @NonNull Response res) {
                try {
                    if (!res.isSuccessful()) {
                        cb.onError("HTTP " + res.code());
                        return;
                    }
                    String resBody = res.body() != null ? res.body().string() : "";
                    TranslationResponse[] arr =
                            gson.fromJson(resBody, TranslationResponse[].class);

                    String translated = (arr != null && arr.length > 0
                            && arr[0].translations != null && !arr[0].translations.isEmpty())
                            ? arr[0].translations.get(0).text
                            : null;

                    if (translated == null) {
                        cb.onError("No translation returned");
                    } else {
                        cb.onSuccess(translated);
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Parse error", ex);
                    cb.onError("Parse error: " + ex.getMessage());
                } finally {
                    res.close();
                }
            }
        });
    }


    private static class RequestItem {
        @SerializedName("Text") String text;
        RequestItem(String t) { text = t; }
    }

    private static class TranslationResponse {
        @SerializedName("translations") List<Translation> translations;
    }

    private static class Translation {
        @SerializedName("text") String text;
        @SerializedName("to")   String to;
    }

    public interface TranslationCallback {
        void onSuccess(String translatedText);
        void onError(String error);
    }
}
