/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: TranslationFragment.java
 * Last Modified: 5/10/2025 10:22
 */

package vn.edu.usth.myapplication;

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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TranslationFragment extends Fragment {

    private static final String ARG_DETECTED_OBJECTS = "detected_objects";
    private static final String ARG_PHOTO_URI = "photo_uri";
    private static final String ARG_USER_INPUT_TEXT = "user_input_text";
    // Language map with common languages (will be sorted alphabetically)
    private final Map<String, String> languageMap = new LinkedHashMap<String, String>() {{
        put("Arabic", "ar");
        put("Chinese (Simplified)", "zh-Hans");
        put("Chinese (Traditional)", "zh-Hant");
        put("Czech", "cs");
        put("Danish", "da");
        put("Dutch", "nl");
        put("English", "en");
        put("Filipino", "fil");
        put("Finnish", "fi");
        put("French", "fr");
        put("German", "de");
        put("Greek", "el");
        put("Hebrew", "he");
        put("Hindi", "hi");
        put("Hungarian", "hu");
        put("Indonesian", "id");
        put("Italian", "it");
        put("Japanese", "ja");
        put("Korean", "ko");
        put("Malay", "ms");
        put("Norwegian", "no");
        put("Polish", "pl");
        put("Portuguese", "pt");
        put("Romanian", "ro");
        put("Russian", "ru");
        put("Spanish", "es");
        put("Swedish", "sv");
        put("Thai", "th");
        put("Turkish", "tr");
        put("Vietnamese", "vi");
    }};
    private ImageView imgPreview;
    private TextView txtDetectedLabel;
    private TextView txtObjectDetected;
    private TextView txtSourceLanguage;
    private TextInputEditText etSourceText;
    private TextInputEditText etTranslatedText;
    private AutoCompleteTextView spinnerTargetLanguage;
    private MaterialButton btnDetectLanguage;
    private MaterialButton btnTranslate;
    private MaterialButton btnSpeak;
    private MaterialButton btnStop;
    private MaterialButton btnBack;
    private ProgressBar progressBar;
    private String[] detectedObjects;
    private String photoUri;
    private String userInputText;
    private AzureTranslatorService translatorService;
    private String detectedLanguageCode = "en"; // Default to English
    private String detectedLanguageName = "English"; // Default name
    private String currentTargetLanguageCode = "vi"; // Track current target language
    // TTS variables
    private TextToSpeech textToSpeech;
    private int speakClickCount = 0;
    private String lastTranslatedText = "";
    // Store initial detected object to keep it hardcoded
    private String initialDetectedObject = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            detectedObjects = getArguments().getStringArray(ARG_DETECTED_OBJECTS);
            photoUri = getArguments().getString(ARG_PHOTO_URI);
            userInputText = getArguments().getString(ARG_USER_INPUT_TEXT);

            // Store initial detected object (hardcoded)
            if (detectedObjects != null && detectedObjects.length > 0) {
                initialDetectedObject = detectedObjects[0];
            }
        }

        translatorService = new AzureTranslatorService();

        // Initialize Text-to-Speech
        textToSpeech = new TextToSpeech(getContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                // Set default language
                setTTSLanguage(currentTargetLanguageCode);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_translation, container, false);

        imgPreview = view.findViewById(R.id.img_preview_small);
        txtDetectedLabel = view.findViewById(R.id.txt_detected_label);
        txtObjectDetected = view.findViewById(R.id.txt_object_detected);
        txtSourceLanguage = view.findViewById(R.id.txt_source_language);
        etSourceText = view.findViewById(R.id.et_source_text);
        etTranslatedText = view.findViewById(R.id.et_translated_text);
        spinnerTargetLanguage = view.findViewById(R.id.spinner_target_language);
        btnDetectLanguage = view.findViewById(R.id.btn_detect_language);
        btnTranslate = view.findViewById(R.id.btn_translate);
        btnSpeak = view.findViewById(R.id.btn_speak);
        btnStop = view.findViewById(R.id.btn_stop);
        btnBack = view.findViewById(R.id.btn_back);
        progressBar = view.findViewById(R.id.progress_bar);

        setupUI();

        return view;
    }

    private void setupUI() {
        // Load preview image if available
        if (photoUri != null && !photoUri.isEmpty()) {
            try {
                android.net.Uri uri = android.net.Uri.parse(photoUri);
                if (imgPreview != null) {
                    imgPreview.setImageURI(uri);
                }
            } catch (Exception e) {
                android.util.Log.e("TranslationFragment", "Error loading image", e);
            }
        }

        // Setup language dropdown (alphabetically sorted)
        setupLanguageDropdown();

        // Set default source language text
        if (txtSourceLanguage != null) {
            txtSourceLanguage.setText("Source language: English");
        }

        // Handle detected objects - HARDCODED display (never changes)
        if (initialDetectedObject == null) {
            // No objects detected - HARDCODED to NONE
            if (txtObjectDetected != null) {
                txtObjectDetected.setText("Object detected: NONE");
                txtObjectDetected.setTextColor(getResources().getColor(R.color.secondary_text, null));
            }
            if (txtDetectedLabel != null) {
                txtDetectedLabel.setText("Proceed to custom translation:");
            }
            if (etSourceText != null) {
                // Check if user provided custom input text
                if (userInputText != null && !userInputText.isEmpty()) {
                    // Populate with user's custom word but keep "Object detected: NONE"
                    etSourceText.setText(userInputText);
                } else {
                    etSourceText.setText("");
                    etSourceText.setHint("Enter word to translate");
                }
            }
        } else {
            // Objects detected - HARDCODED display (uses initialDetectedObject, never changes)
            if (txtObjectDetected != null) {
                // HARDCODED - always shows initial detected object, never changes
                txtObjectDetected.setText("Object detected: " + initialDetectedObject);
                txtObjectDetected.setTextColor(getResources().getColor(R.color.primary_color, null));
            }
            if (txtDetectedLabel != null) {
                if (detectedObjects.length > 1) {
                    txtDetectedLabel.setText("Detected " + detectedObjects.length + " objects:");
                } else {
                    txtDetectedLabel.setVisibility(View.GONE); // Hide label for single object
                }
            }
            if (etSourceText != null) {
                // Auto-populate with detected object
                if (detectedObjects.length == 1) {
                    etSourceText.setText(initialDetectedObject);
                } else {
                    etSourceText.setText(String.join(", ", detectedObjects));
                }
            }
        }

        // Hide TTS buttons initially
        if (btnSpeak != null) btnSpeak.setVisibility(View.GONE);
        if (btnStop != null) btnStop.setVisibility(View.GONE);

        // Detect Language button
        if (btnDetectLanguage != null) {
            btnDetectLanguage.setOnClickListener(v -> detectSourceLanguage());
        }

        // Translate button
        if (btnTranslate != null) {
            btnTranslate.setOnClickListener(v -> performTranslation());
        }

        // Speak button - with speed control
        if (btnSpeak != null) {
            btnSpeak.setOnClickListener(v -> {
                speakClickCount++;
                float speed = (speakClickCount % 2 == 1) ? 1.0f : 0.5f;
                speakText(speed);
                btnSpeak.setText(speakClickCount % 2 == 1 ? "Speak (1.0x)" : "Speak (0.5x)");
            });
        }

        // Stop button - resets speed counter
        if (btnStop != null) {
            btnStop.setOnClickListener(v -> {
                stopSpeaking();
                speakClickCount = 0;
                btnSpeak.setText("Speak");
            });
        }

        // Back button
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                try {
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
                    navController.navigateUp();
                } catch (Exception e) {
                    android.util.Log.e("TranslationFragment", "Navigation error", e);
                    if (getActivity() != null) {
                        getActivity().onBackPressed();
                    }
                }
            });
        }
    }

    private void setupLanguageDropdown() {
        if (spinnerTargetLanguage == null) return;

        // Get language names and sort alphabetically
        List<String> languageNames = new ArrayList<>(languageMap.keySet());
        Collections.sort(languageNames); // Alphabetically sorted

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                languageNames
        );

        spinnerTargetLanguage.setAdapter(adapter);
        spinnerTargetLanguage.setText("Vietnamese", false); // Default to Vietnamese
        currentTargetLanguageCode = "vi";

        // Listen for language changes to update TTS
        spinnerTargetLanguage.setOnItemClickListener((parent, view, position, id) -> {
            String selectedLanguageName = languageNames.get(position);
            String languageCode = languageMap.get(selectedLanguageName);
            if (languageCode != null) {
                currentTargetLanguageCode = languageCode;
                setTTSLanguage(languageCode);
            }
        });
    }

    private void setTTSLanguage(String languageCode) {
        if (textToSpeech == null) return;

        Locale locale;
        switch (languageCode) {
            case "vi":
                locale = new Locale("vi", "VN");
                break;
            case "ja":
                locale = Locale.JAPANESE;
                break;
            case "ko":
                locale = Locale.KOREAN;
                break;
            case "zh-Hans":
                locale = Locale.SIMPLIFIED_CHINESE;
                break;
            case "zh-Hant":
                locale = Locale.TRADITIONAL_CHINESE;
                break;
            case "fr":
                locale = Locale.FRENCH;
                break;
            case "es":
                locale = new Locale("es", "ES");
                break;
            case "de":
                locale = Locale.GERMAN;
                break;
            case "it":
                locale = Locale.ITALIAN;
                break;
            case "pt":
                locale = new Locale("pt", "PT");
                break;
            case "ru":
                locale = new Locale("ru", "RU");
                break;
            case "th":
                locale = new Locale("th", "TH");
                break;
            case "ar":
                locale = new Locale("ar", "SA");
                break;
            case "nl":
                locale = new Locale("nl", "NL");
                break;
            case "tr":
                locale = new Locale("tr", "TR");
                break;
            case "pl":
                locale = new Locale("pl", "PL");
                break;
            case "sv":
                locale = new Locale("sv", "SE");
                break;
            case "da":
                locale = new Locale("da", "DK");
                break;
            case "fi":
                locale = new Locale("fi", "FI");
                break;
            case "no":
                locale = new Locale("no", "NO");
                break;
            case "cs":
                locale = new Locale("cs", "CZ");
                break;
            case "hu":
                locale = new Locale("hu", "HU");
                break;
            case "ro":
                locale = new Locale("ro", "RO");
                break;
            case "el":
                locale = new Locale("el", "GR");
                break;
            case "he":
                locale = new Locale("he", "IL");
                break;
            case "hi":
                locale = new Locale("hi", "IN");
                break;
            case "id":
                locale = new Locale("id", "ID");
                break;
            case "ms":
                locale = new Locale("ms", "MY");
                break;
            case "fil":
                locale = new Locale("fil", "PH");
                break;
            case "en":
            default:
                locale = Locale.US;
                break;
        }

        int result = textToSpeech.setLanguage(locale);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            android.util.Log.w("TranslationFragment", "TTS language not supported: " + languageCode);
            // Fallback to English
            textToSpeech.setLanguage(Locale.US);
        }
    }

    private void detectSourceLanguage() {
        if (etSourceText == null) return;

        String sourceText = etSourceText.getText() != null ?
                etSourceText.getText().toString().trim() : "";

        if (sourceText.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter text to detect language", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (btnDetectLanguage != null) {
            btnDetectLanguage.setEnabled(false);
        }

        // Use Azure to detect language
        translatorService.detectLanguage(sourceText, new AzureTranslatorService.LanguageDetectionCallback() {
            @Override
            public void onSuccess(String languageCode, String languageName) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        detectedLanguageCode = languageCode;
                        detectedLanguageName = languageName;

                        // Update source language text
                        if (txtSourceLanguage != null) {
                            txtSourceLanguage.setText("Source language: " + languageName);
                        }

                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        if (btnDetectLanguage != null) {
                            btnDetectLanguage.setEnabled(true);
                        }
                        Toast.makeText(getContext(),
                                "Detected language: " + languageName + " (" + languageCode + ")",
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        if (btnDetectLanguage != null) {
                            btnDetectLanguage.setEnabled(true);
                        }
                        Toast.makeText(getContext(),
                                "Language detection unavailable. Using English as source.",
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void performTranslation() {
        try {
            if (etSourceText == null || spinnerTargetLanguage == null) {
                Toast.makeText(getContext(), "Error: Input fields not found", Toast.LENGTH_SHORT).show();
                return;
            }

            String sourceText = etSourceText.getText() != null ?
                    etSourceText.getText().toString().trim() : "";

            if (sourceText.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter text to translate", Toast.LENGTH_SHORT).show();
                return;
            }

            String selectedLanguageName = spinnerTargetLanguage.getText().toString();
            String targetLanguageCode = languageMap.get(selectedLanguageName);

            if (targetLanguageCode == null) {
                targetLanguageCode = "vi"; // Default to Vietnamese
            }

            // Update current target language and TTS
            currentTargetLanguageCode = targetLanguageCode;
            setTTSLanguage(targetLanguageCode);

            // Show progress
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
            if (btnTranslate != null) {
                btnTranslate.setEnabled(false);
            }

            final String finalTargetCode = targetLanguageCode;

            // Use Azure Translator
            translatorService.translate(sourceText, finalTargetCode,
                    new AzureTranslatorService.TranslationCallback() {
                        @Override
                        public void onSuccess(String translatedText) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    if (etTranslatedText != null) {
                                        etTranslatedText.setText(translatedText);
                                    }

                                    // Check if text changed - reset speed counter
                                    if (!translatedText.equals(lastTranslatedText)) {
                                        speakClickCount = 0;
                                        if (btnSpeak != null) {
                                            btnSpeak.setText("Speak");
                                        }
                                        lastTranslatedText = translatedText;
                                    }

                                    // Show TTS buttons on successful translation
                                    if (btnSpeak != null) btnSpeak.setVisibility(View.VISIBLE);
                                    if (btnStop != null) btnStop.setVisibility(View.VISIBLE);

                                    if (progressBar != null) {
                                        progressBar.setVisibility(View.GONE);
                                    }
                                    if (btnTranslate != null) {
                                        btnTranslate.setEnabled(true);
                                    }
                                    Toast.makeText(getContext(), "Translation completed!", Toast.LENGTH_SHORT).show();
                                });
                            }
                        }

                        @Override
                        public void onError(String error) {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    if (progressBar != null) {
                                        progressBar.setVisibility(View.GONE);
                                    }
                                    if (btnTranslate != null) {
                                        btnTranslate.setEnabled(true);
                                    }

                                    // Fallback to offline translation if Azure fails
                                    String fallbackTranslation = getFallbackTranslation(sourceText, finalTargetCode);
                                    if (etTranslatedText != null) {
                                        etTranslatedText.setText(fallbackTranslation);
                                    }

                                    Toast.makeText(getContext(),
                                            "Using offline translation (check Azure API config)",
                                            Toast.LENGTH_LONG).show();
                                });
                            }
                        }
                    });

        } catch (Exception e) {
            android.util.Log.e("TranslationFragment", "Translation error", e);
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            if (btnTranslate != null) {
                btnTranslate.setEnabled(true);
            }
            Toast.makeText(getContext(), "Translation error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void speakText(float speed) {
        if (textToSpeech != null && etTranslatedText != null) {
            String text = etTranslatedText.getText() != null ?
                    etTranslatedText.getText().toString() : "";

            if (!text.isEmpty()) {
                textToSpeech.setSpeechRate(speed);
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
                Toast.makeText(getContext(), "Speaking at " + speed + "x speed", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "No text to speak", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void stopSpeaking() {
        if (textToSpeech != null && textToSpeech.isSpeaking()) {
            textToSpeech.stop();
            Toast.makeText(getContext(), "Stopped speaking", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFallbackTranslation(String word, String targetLang) {
        // Fallback offline translation for common words
        Map<String, Map<String, String>> translations = new LinkedHashMap<>();

        // Car translations
        Map<String, String> carTrans = new LinkedHashMap<>();
        carTrans.put("vi", "xe hơi");
        carTrans.put("ja", "車");
        carTrans.put("fr", "voiture");
        carTrans.put("es", "coche");
        carTrans.put("de", "Auto");
        carTrans.put("ko", "자동차");
        carTrans.put("zh-Hans", "汽车");
        translations.put("car", carTrans);

        // Person translations
        Map<String, String> personTrans = new LinkedHashMap<>();
        personTrans.put("vi", "người");
        personTrans.put("ja", "人");
        personTrans.put("fr", "personne");
        personTrans.put("es", "persona");
        personTrans.put("de", "Person");
        personTrans.put("ko", "사람");
        personTrans.put("zh-Hans", "人");
        translations.put("person", personTrans);

        String wordLower = word.toLowerCase();
        if (translations.containsKey(wordLower)) {
            Map<String, String> wordTrans = translations.get(wordLower);
            if (wordTrans.containsKey(targetLang)) {
                return wordTrans.get(targetLang);
            }
        }

        return "Translation unavailable offline.\n\nPlease check your Azure API configuration in local.properties:\n" +
                "- AZURE_TRANSLATOR_KEY\n" +
                "- AZURE_TRANSLATOR_REGION";
    }

    @Override
    public void onDestroy() {
        // Shutdown TTS when fragment is destroyed
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
