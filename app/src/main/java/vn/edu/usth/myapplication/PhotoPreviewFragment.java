package vn.edu.usth.myapplication;

import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.*;

public class PhotoPreviewFragment extends Fragment {

    private static final String ARG_PHOTO_URI = "photo_uri";
    private static final String ARG_IS_TEMP = "is_temp";
    private ImageView imgPreview;
    private TextView txtDetectedObjects;
    private FloatingActionButton btnSave;
    private ExtendedFloatingActionButton btnProceedTranslation;
    private String photoUri;
    private boolean isTemp = false;
    private Bitmap currentBitmap;
    private final List<String> detectedObjectsList = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            photoUri = getArguments().getString(ARG_PHOTO_URI);
            isTemp = getArguments().getBoolean(ARG_IS_TEMP, false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_preview, container, false);
        imgPreview = v.findViewById(R.id.img_preview);
        txtDetectedObjects = v.findViewById(R.id.txt_detected_objects);
        FloatingActionButton btnBack = v.findViewById(R.id.btn_back_to_camera);
        btnSave = v.findViewById(R.id.btn_save_photo);
        btnProceedTranslation = v.findViewById(R.id.btn_proceed_translation);

        btnBack.setOnClickListener(b -> Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigateUp());
        btnSave.setVisibility(isTemp ? View.VISIBLE : View.GONE);
        btnSave.setOnClickListener(v1 -> Toast.makeText(requireContext(), "Save disabled for demo", Toast.LENGTH_SHORT).show());
        btnProceedTranslation.setOnClickListener(v1 -> proceedToTranslation());

        if (photoUri != null) loadAndDetectObjects(photoUri);
        return v;
    }

    private void loadAndDetectObjects(String uriString) {
        try {
            Uri uri = Uri.parse(uriString);
            Bitmap bitmap = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    ? ImageDecoder.decodeBitmap(ImageDecoder.createSource(requireContext().getContentResolver(), uri))
                    : MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
            currentBitmap = bitmap;
            imgPreview.setImageBitmap(bitmap);
            detectObjects(bitmap);
        } catch (Exception ignored) {}
    }

    private void detectObjects(Bitmap bitmap) {
        txtDetectedObjects.setText("Analyzing...");
        new Thread(() -> {
            Bitmap processed = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            List<YOLOv5Classifier.Result> results = YOLOv5Classifier.getInstance(requireContext()).detect(processed);
            Set<String> labels = new LinkedHashSet<>();
            for (YOLOv5Classifier.Result r : results) labels.add(r.label);
            requireActivity().runOnUiThread(() -> {
                txtDetectedObjects.setText(labels.isEmpty() ? "No objects detected" :
                        "Detected: " + String.join(", ", labels));
                imgPreview.setImageBitmap(processed);
                detectedObjectsList.clear();
                detectedObjectsList.addAll(labels);
            });
        }).start();
    }

    private void proceedToTranslation() {
        Bundle b = new Bundle();
        b.putStringArray("detected_objects", detectedObjectsList.toArray(new String[0]));
        b.putString("photo_uri", photoUri);
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
        navController.navigate(R.id.action_photoPreviewFragment_to_translationFragment, b);
    }
}
