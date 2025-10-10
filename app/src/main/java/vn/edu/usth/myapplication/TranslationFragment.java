/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: TranslationFragment.java
 * Last Modified: 6/10/2025 10:33
 */

package vn.edu.usth.myapplication;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TranslationFragment extends Fragment {

    private static final String ARG_DETECTED_OBJECTS = "detected_objects";
    private static final String ARG_PHOTO_URI = "photo_uri";
    private static final String ARG_USER_INPUT_TEXT = "user_input_text";

    // ===== Languages: name ↔ code (compact)
    private static final String[][] LANGS = {
            {"Arabic", "ar"}, {"Chinese (Simplified)", "zh-Hans"}, {"Chinese (Traditional)", "zh-Hant"},
            {"Czech", "cs"}, {"Danish", "da"}, {"Dutch", "nl"}, {"English", "en"}, {"Filipino", "fil"},
            {"Finnish", "fi"}, {"French", "fr"}, {"German", "de"}, {"Greek", "el"}, {"Hebrew", "he"},
            {"Hindi", "hi"}, {"Hungarian", "hu"}, {"Indonesian", "id"}, {"Italian", "it"},
            {"Japanese", "ja"}, {"Korean", "ko"}, {"Malay", "ms"}, {"Norwegian", "no"},
            {"Polish", "pl"}, {"Portuguese", "pt"}, {"Romanian", "ro"}, {"Russian", "ru"},
            {"Spanish", "es"}, {"Swedish", "sv"}, {"Thai", "th"}, {"Turkish", "tr"}, {"Vietnamese", "vi"}
    };
    private final Map<String, String> languageMap = new HashMap<>();
    private final List<String> languageNames = new ArrayList<>();

    private ImageView imgPreview;
    private TextView txtObjectDetected, txtSourceLanguage;
    private TextInputEditText etSourceText, etTranslatedText;
    private AutoCompleteTextView spinnerTargetLanguage;
    private MaterialButton btnDetectLanguage, btnTranslate, btnSpeak, btnStop, btnBack;
    private ProgressBar progressBar;

    private String[] detectedObjects;
    private String photoUri;
    private String userInputText;
    private String initialDetectedObject = null;

    private AzureTranslatorService translatorService;
    private TranslationHistoryDatabase historyDatabase;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private String currentTargetCode = "vi";
    private int speakClickCount = 0;
    private String lastTranslatedText = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            detectedObjects = getArguments().getStringArray(ARG_DETECTED_OBJECTS);
            photoUri = getArguments().getString(ARG_PHOTO_URI);
            userInputText = getArguments().getString(ARG_USER_INPUT_TEXT);

            // Store initial detected object (hardcoded display)
            if (detectedObjects != null && detectedObjects.length > 0) {
                initialDetectedObject = detectedObjects[0];
            }
        }

        // Build language list quickly
        for (String[] row : LANGS) {
            languageMap.put(row[0], row[1]);
            languageNames.add(row[0]);
        }
        languageNames.sort(String::compareTo);

        translatorService = new AzureTranslatorService();

        // Init TTS compact
        tts = new TextToSpeech(getContext(), st -> {
            ttsReady = (st == TextToSpeech.SUCCESS);
            if (ttsReady) setTtsLanguage(currentTargetCode);
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_translation, container, false);

        // Initialize database
        historyDatabase = new TranslationHistoryDatabase(getContext());

        imgPreview = v.findViewById(R.id.img_preview_small);
        txtObjectDetected = v.findViewById(R.id.txt_object_detected);
        txtSourceLanguage = v.findViewById(R.id.txt_source_language);
        etSourceText = v.findViewById(R.id.et_source_text);
        etTranslatedText = v.findViewById(R.id.et_translated_text);
        spinnerTargetLanguage = v.findViewById(R.id.spinner_target_language);
        btnDetectLanguage = v.findViewById(R.id.btn_detect_language);
        btnTranslate = v.findViewById(R.id.btn_translate);
        btnSpeak = v.findViewById(R.id.btn_speak);
        btnStop = v.findViewById(R.id.btn_stop);
        btnBack = v.findViewById(R.id.btn_back);
        progressBar = v.findViewById(R.id.progress_bar);

        setupLanguageDropdown();
        bindSimpleUi();

        return v;
    }

    private void bindSimpleUi() {
        // Display preview image if available
        if (imgPreview != null && photoUri != null && !photoUri.isEmpty()) {
            try {
                imgPreview.setImageURI(android.net.Uri.parse(photoUri));
            } catch (Exception ignored) {
            }
        }

        // Display detected object (HARDCODED - never changes)
        if (initialDetectedObject != null) {
            txtObjectDetected.setText("Object detected: " + initialDetectedObject);
            txtObjectDetected.setTextColor(getResources().getColor(R.color.primary_color, null));
            etSourceText.setText(initialDetectedObject);
        } else if (userInputText != null && !userInputText.isEmpty()) {
            txtObjectDetected.setText("Object detected: NONE");
            txtObjectDetected.setTextColor(getResources().getColor(R.color.secondary_text, null));
            etSourceText.setText(userInputText);
        } else {
            txtObjectDetected.setText("Object detected: NONE");
            txtObjectDetected.setTextColor(getResources().getColor(R.color.secondary_text, null));
        }

        txtSourceLanguage.setText("Source language: English");

        // Hide TTS buttons initially
        btnSpeak.setVisibility(View.GONE);
        btnStop.setVisibility(View.GONE);

        // Back button
        btnBack.setOnClickListener(v -> {
            try {
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigateUp();
            } catch (Exception e) {
                if (getActivity() != null) getActivity().onBackPressed();
            }
        });

        // Detect language button
        btnDetectLanguage.setOnClickListener(v -> detectLanguage());

        // Translate button
        btnTranslate.setOnClickListener(v -> translate());

        // Speak button with speed control (1.0x -> 0.5x)
        btnSpeak.setOnClickListener(v -> {
            if (!ttsReady) {
                toast("Text-to-Speech unavailable on this device");
                return;
            }
            speakClickCount++;
            float speed = (speakClickCount % 2 == 1) ? 1.0f : 0.5f;
            speak(speed);
            btnSpeak.setText(speakClickCount % 2 == 1 ? "Speak (1.0x)" : "Speak (0.5x)");
        });

        // Stop button - resets speed counter
        btnStop.setOnClickListener(v -> {
            if (tts != null) tts.stop();
            speakClickCount = 0;
            btnSpeak.setText("Speak");
        });
    }

    private void setupLanguageDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, languageNames);
        spinnerTargetLanguage.setAdapter(adapter);
        spinnerTargetLanguage.setText("Vietnamese", false);
        currentTargetCode = "vi";
        spinnerTargetLanguage.setOnItemClickListener((p, v, pos, id) -> {
            String selectedLang = languageNames.get(pos);
            currentTargetCode = languageMap.get(selectedLang);
            setTtsLanguage(currentTargetCode);
        });
    }

    // ===== Check internet connectivity
    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) requireContext()
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo netInfo = cm.getActiveNetworkInfo();
                return netInfo != null && netInfo.isConnected();
            }
        } catch (Exception e) {
            android.util.Log.e("TranslationFragment", "Error checking network", e);
        }
        return false;
    }

    private void showNoInternetDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("No Internet Connection")
                .setMessage("Translation requires an active internet connection.\n\nPlease connect to the internet and try again.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    // ===== Detect source language
    private void detectLanguage() {
        String src = safeText(etSourceText);
        if (src.isEmpty()) {
            toast("Please enter text to detect language");
            return;
        }

        if (!isNetworkAvailable()) {
            showNoInternetDialog();
            return;
        }

        setLoading(true);
        translatorService.detectLanguage(src, new AzureTranslatorService.LanguageDetectionCallback() {
            @Override
            public void onSuccess(String languageCode, String languageName) {
                runOnUi(() -> {
                    txtSourceLanguage.setText("Source language: " + languageName);
                    setLoading(false);
                    toast("Detected: " + languageName);
                });
            }

            @Override
            public void onError(String error) {
                runOnUi(() -> {
                    setLoading(false);
                    toast("Detection failed. Using English as default.");
                });
            }
        });
    }

    // ===== Translate
    private void translate() {
        String src = safeText(etSourceText);
        if (src.isEmpty()) {
            toast("Please enter text to translate");
            return;
        }

        if (!isNetworkAvailable()) {
            showNoInternetDialog();
            return;
        }

        setTtsLanguage(currentTargetCode); // Update TTS for current target

        setLoading(true);
        translatorService.translate(src, currentTargetCode, new AzureTranslatorService.TranslationCallback() {
            @Override
            public void onSuccess(String out) {
                runOnUi(() -> {
                    etTranslatedText.setText(out);
                    setLoading(false);

                    // Save translation to database
                    if (historyDatabase != null) {
                        historyDatabase.saveTranslation(src, out);
                    }

                    // Reset speed counter if text changed
                    if (!out.equals(lastTranslatedText)) {
                        speakClickCount = 0;
                        btnSpeak.setText("Speak");
                        lastTranslatedText = out;
                    }

                    // Show TTS buttons if available
                    if (ttsReady) {
                        btnSpeak.setVisibility(View.VISIBLE);
                        btnStop.setVisibility(View.VISIBLE);
                    }

                    toast("Translation completed!");
                });
            }

            @Override
            public void onError(String err) {
                runOnUi(() -> {
                    setLoading(false);
                    toast("Translation failed. Check Azure API config in local.properties");
                });
            }
        });
    }

    // ===== Helpers
    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (btnTranslate != null) {
            btnTranslate.setEnabled(!loading);
        }
        if (btnDetectLanguage != null) {
            btnDetectLanguage.setEnabled(!loading);
        }
    }

    private int setTtsLanguage(String code) {
        if (!ttsReady || tts == null) return TextToSpeech.ERROR;
        try {
            // Use BCP-47, e.g. "vi", "en-US", "zh-Hans"
            Locale loc = Locale.forLanguageTag(code);
            int r = tts.setLanguage(loc);
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                r = tts.setLanguage(Locale.US); // Fallback to English
            }
            return r;
        } catch (Exception e) {
            return TextToSpeech.ERROR;
        }
    }

    private void speak(float speed) {
        if (!ttsReady || tts == null) {
            toast("Text-to-Speech unavailable");
            return;
        }
        String text = safeText(etTranslatedText);
        if (text.isEmpty()) {
            toast("No text to speak");
            return;
        }
        tts.stop();
        tts.setSpeechRate(speed);
        int result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS");
        if (result == TextToSpeech.SUCCESS) {
            toast("Speaking at " + speed + "x speed");
        }
    }

    private String safeText(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void toast(String s) {
        Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show();
    }

    private void runOnUi(Runnable r) {
        if (getActivity() != null) getActivity().runOnUiThread(r);
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception ignored) {
            }
        }
        super.onDestroy();
    }
}
