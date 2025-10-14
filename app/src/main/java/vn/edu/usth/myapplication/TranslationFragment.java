package vn.edu.usth.myapplication;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ImageView;
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

    // ===== Ngôn ngữ: tên ↔ mã (gọn)
    private static final String[][] LANGS = {
            {"Arabic","ar"}, {"Chinese (Simplified)","zh-Hans"}, {"Chinese (Traditional)","zh-Hant"},
            {"Czech","cs"}, {"Danish","da"}, {"Dutch","nl"}, {"English","en"}, {"Filipino","fil"},
            {"Finnish","fi"}, {"French","fr"}, {"German","de"}, {"Greek","el"}, {"Hebrew","he"},
            {"Hindi","hi"}, {"Hungarian","hu"}, {"Indonesian","id"}, {"Italian","it"},
            {"Japanese","ja"}, {"Korean","ko"}, {"Malay","ms"}, {"Norwegian","no"},
            {"Polish","pl"}, {"Portuguese","pt"}, {"Romanian","ro"}, {"Russian","ru"},
            {"Spanish","es"}, {"Swedish","sv"}, {"Thai","th"}, {"Turkish","tr"}, {"Vietnamese","vi"}
    };
    private final Map<String,String> languageMap = new HashMap<>();
    private final List<String> languageNames = new ArrayList<>();

    private ImageView imgPreview;

    private TextView txtObjectDetected, txtSourceLanguage;
    private TextInputEditText etSourceText, etTranslatedText;
    private AutoCompleteTextView spinnerTargetLanguage;
    private MaterialButton btnTranslate, btnSpeak, btnStop, btnBack;
    private ProgressBar progressBar;

    private String[] detectedObjects;
    private String photoUri;
    private String userInputText;


    private AzureTranslatorService translatorService;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private String currentTargetCode = "vi";

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            detectedObjects = getArguments().getStringArray(ARG_DETECTED_OBJECTS);
            photoUri = getArguments().getString(ARG_PHOTO_URI);
            userInputText = getArguments().getString(ARG_USER_INPUT_TEXT);
        }

        for (String[] row : LANGS) { languageMap.put(row[0], row[1]); languageNames.add(row[0]); }
        languageNames.sort(String::compareTo);

        translatorService = new AzureTranslatorService();

        // Init TTS gọn
        tts = new TextToSpeech(getContext(), st -> {
            ttsReady = (st == TextToSpeech.SUCCESS);
            if (ttsReady) setTtsLanguage(currentTargetCode);
        });
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_translation, container, false);
        imgPreview = v.findViewById(R.id.img_preview_small);

        txtObjectDetected   = v.findViewById(R.id.txt_object_detected);
        txtSourceLanguage   = v.findViewById(R.id.txt_source_language);
        etSourceText        = v.findViewById(R.id.et_source_text);
        etTranslatedText    = v.findViewById(R.id.et_translated_text);
        spinnerTargetLanguage = v.findViewById(R.id.spinner_target_language);
        btnTranslate        = v.findViewById(R.id.btn_translate);
        btnSpeak            = v.findViewById(R.id.btn_speak);
        btnStop             = v.findViewById(R.id.btn_stop);
        btnBack             = v.findViewById(R.id.btn_back);
        progressBar         = v.findViewById(R.id.progress_bar);

        setupLanguageDropdown();
        bindSimpleUi();

        return v;
    }

    private void bindSimpleUi() {
        // Hiển thị object phát hiện (nếu có) + điền sẵn vào ô nguồn
        if (imgPreview != null && photoUri != null && !photoUri.isEmpty()) {
            try { imgPreview.setImageURI(android.net.Uri.parse(photoUri)); } catch (Exception ignored) {}
        }
        String first = (detectedObjects != null && detectedObjects.length > 0) ? detectedObjects[0] : null;
        if (first != null) {
            txtObjectDetected.setText("Object detected: " + first);
            etSourceText.setText(first);
        } else if (userInputText != null && !userInputText.isEmpty()) {
            txtObjectDetected.setText("Object detected: NONE");
            etSourceText.setText(userInputText);
        } else {
            txtObjectDetected.setText("Object detected: NONE");
        }
        txtSourceLanguage.setText("Source language: English");

        btnBack.setOnClickListener(v ->
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigateUp());

        btnTranslate.setOnClickListener(v -> translate());
        btnSpeak.setOnClickListener(v -> speak(1.0f));
        btnStop.setOnClickListener(v -> { if (tts != null) tts.stop(); });
        btnSpeak.setVisibility(View.GONE); btnStop.setVisibility(View.GONE);
    }

    private void setupLanguageDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, languageNames);
        spinnerTargetLanguage.setAdapter(adapter);
        spinnerTargetLanguage.setText("Vietnamese", false);
        currentTargetCode = "vi";
        spinnerTargetLanguage.setOnItemClickListener((p, v, pos, id) -> {
            currentTargetCode = languageMap.get(languageNames.get(pos));
            setTtsLanguage(currentTargetCode);
        });
    }

    // ===== Helpers ngắn gọn
    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (btnTranslate != null) {
            btnTranslate.setEnabled(!loading);
        }

    }


    private int setTtsLanguage(String code) {
        if (!ttsReady || tts == null) return TextToSpeech.ERROR;
        try {
            // Dùng BCP-47, ví dụ "vi", "en-US", "zh-Hans"
            Locale loc = Locale.forLanguageTag(code);
            int r = tts.setLanguage(loc);
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                r = tts.setLanguage(Locale.US);
            }
            return r;
        } catch (Exception e) { return TextToSpeech.ERROR; }
    }

    private void translate() {
        String src = safeText(etSourceText);
        if (src.isEmpty()) { toast("Please enter text to translate"); return; }
        setTTSforCurrentTarget(); // cập nhật TTS theo code hiện tại

        setLoading(true);
        translatorService.translate(src, currentTargetCode, new AzureTranslatorService.TranslationCallback() {
            @Override public void onSuccess(String out) {
                runOnUi(() -> {
                    etTranslatedText.setText(out);
                    setLoading(false);
                    if (ttsReady) { btnSpeak.setVisibility(View.VISIBLE); btnStop.setVisibility(View.VISIBLE); }
                    toast("Translation completed!");

                    TranslationHistoryDatabase db = new TranslationHistoryDatabase(getContext());
                    db.addTranslation(safeText(etSourceText), out);
                });
            }
            @Override public void onError(String err) {
                runOnUi(() -> { setLoading(false); toast("Azure error. Check key/region in local.properties"); });
            }
        });
    }

    private void setTTSforCurrentTarget() { setTtsLanguage(currentTargetCode); }

    private void speak(float speed) {
        if (!ttsReady || tts == null) { toast("Text-to-Speech unavailable"); return; }
        String text = safeText(etTranslatedText);
        if (text.isEmpty()) { toast("No text to speak"); return; }
        tts.stop(); tts.setSpeechRate(speed);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS");
    }

    private String safeText(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void toast(String s) { Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show(); }
    private void runOnUi(Runnable r) { if (getActivity()!=null) getActivity().runOnUiThread(r); }

    @Override public void onDestroy() {
        if (tts != null) { try { tts.stop(); tts.shutdown(); } catch (Exception ignored) {} }
        super.onDestroy();
    }
}
