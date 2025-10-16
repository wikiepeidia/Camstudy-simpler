package vn.edu.usth.camstudy.network;

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
import vn.edu.usth.camstudy.BuildConfig;

/**
 * AzureTranslatorService
 * -----------------------
 * This class handles text translation using the Microsoft Azure Translator API.
 * It sends asynchronous HTTP requests through OkHttp and parses responses using Gson.
 */
public final class AzureTranslatorService {

    // Tag for logging in Logcat
    private static final String TAG = "AzureTranslator";

    // Azure Translator API base endpoint
    private static final String ENDPOINT = "https://api.cognitive.microsofttranslator.com";

    // API key and region from BuildConfig (loaded from local.properties)
    private static final String API_KEY = BuildConfig.AZURE_TRANSLATOR_KEY;
    private static final String REGION  = BuildConfig.AZURE_TRANSLATOR_REGION;

    // JSON media type definition
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // OkHttp client with custom timeout configuration
    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)   // Connection timeout
            .readTimeout(30, TimeUnit.SECONDS)      // Read timeout
            .build();

    // Gson instance for JSON serialization and deserialization
    private final Gson gson = new Gson();

    /**
     * Translates a given text asynchronously using the Azure Translator API.
     *
     * @param text   The text to translate.
     * @param target The target language code (e.g., "vi", "en", "fr").
     * @param cb     Callback for delivering the result or an error.
     */
    public void translate(@NonNull String text,
                          @NonNull String target,
                          @NonNull TranslationCallback cb) {

        // Build the full API URL with the target language parameter
        String url = ENDPOINT + "/translate?api-version=3.0&to=" + target;

        // Prepare the request body in JSON format
        String bodyJson = gson.toJson(new RequestItem[]{ new RequestItem(text) });
        RequestBody body = RequestBody.create(bodyJson, JSON);

        // Build HTTP POST request with headers
        Request req = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Ocp-Apim-Subscription-Key", API_KEY)
                .addHeader("Ocp-Apim-Subscription-Region", REGION)
                .addHeader("Content-Type", "application/json")
                .build();

        // Send the request asynchronously to avoid blocking the UI thread
        http.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Called when network or connection fails
                Log.e(TAG, "Network error", e);
                cb.onError("Network error: " + e.getMessage());
            }

            @Override public void onResponse(@NonNull Call call, @NonNull Response res) {
                try {
                    // Check if the HTTP response was successful
                    if (!res.isSuccessful()) {
                        cb.onError("HTTP " + res.code());
                        return;
                    }

                    // Read the response body as a string
                    String resBody = res.body() != null ? res.body().string() : "";

                    // Parse JSON into an array of TranslationResponse objects
                    TranslationResponse[] arr =
                            gson.fromJson(resBody, TranslationResponse[].class);

                    // Safely extract translated text
                    String translated = (arr != null && arr.length > 0
                            && arr[0].translations != null && !arr[0].translations.isEmpty())
                            ? arr[0].translations.get(0).text
                            : null;

                    // Deliver result or error depending on content
                    if (translated == null) {
                        cb.onError("No translation returned");
                    } else {
                        cb.onSuccess(translated);
                    }
                } catch (Exception ex) {
                    // Handle JSON parsing or runtime errors
                    Log.e(TAG, "Parse error", ex);
                    cb.onError("Parse error: " + ex.getMessage());
                } finally {
                    // Always close the response to free resources
                    res.close();
                }
            }
        });
    }

    /**
     * Represents a single text request item.
     * The Azure Translator API expects an array of {"Text": "..."} objects.
     */
    private static class RequestItem {
        @SerializedName("Text") String text;
        RequestItem(String t) { text = t; }
    }

    /**
     * Represents the overall translation response structure.
     * The API returns an array of these objects.
     */
    private static class TranslationResponse {
        @SerializedName("translations") List<Translation> translations;
    }

    /**
     * Represents a single translated result within the response.
     */
    private static class Translation {
        @SerializedName("text") String text;  // The translated text
        @SerializedName("to")   String to;    // Target language code
    }

    /**
     * Callback interface for delivering translation results asynchronously.
     */
    public interface TranslationCallback {
        void onSuccess(String translatedText); // Called when translation succeeds
        void onError(String error);            // Called when an error occurs
    }
}
