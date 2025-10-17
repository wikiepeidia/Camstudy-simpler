package vn.edu.usth.myapplication;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.*;
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
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_preview, container, false);
        imgPreview = v.findViewById(R.id.img_preview);
        txtDetectedObjects = v.findViewById(R.id.txt_detected_objects);
        FloatingActionButton btnBack = v.findViewById(R.id.btn_back_to_camera);
        btnSave = v.findViewById(R.id.btn_save_photo);
        btnProceedTranslation = v.findViewById(R.id.btn_proceed_translation);

        // Back to camera
        btnBack.setOnClickListener(b ->
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigateUp());

        // Save button
        btnSave.setVisibility(isTemp ? View.VISIBLE : View.GONE);
        btnSave.setOnClickListener(v1 -> {
            if (currentBitmap != null) savePhotoToGallery(currentBitmap);
            else Toast.makeText(requireContext(), "No image to save", Toast.LENGTH_SHORT).show();
        });

        // Translation button
        btnProceedTranslation.setOnClickListener(v1 -> proceedToTranslation());

        // Load photo
        if (photoUri != null) loadAndDetectObjects(photoUri);
        return v;
    }

    private void loadAndDetectObjects(String uriString) {
        try {
            Uri uri = Uri.parse(uriString);
            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                bitmap = ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(requireContext().getContentResolver(), uri));
            } else {
                bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
            }
            currentBitmap = bitmap;
            imgPreview.setImageBitmap(bitmap);
            detectObjects(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void detectObjects(Bitmap bitmap) {
        txtDetectedObjects.setText("Analyzing...");
        new Thread(() -> {
            Bitmap processed = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            List<YOLOv5Classifier.Result> results =
                    YOLOv5Classifier.getInstance(requireContext()).detect(processed);

            Set<String> labels = new LinkedHashSet<>();
            for (YOLOv5Classifier.Result r : results) labels.add(r.label);

            requireActivity().runOnUiThread(() -> {
                txtDetectedObjects.setText(labels.isEmpty()
                        ? "No objects detected"
                        : "Detected: " + String.join(", ", labels));
                imgPreview.setImageBitmap(processed);
                detectedObjectsList.clear();
                detectedObjectsList.addAll(labels);
            });
        }).start();
    }

    private void savePhotoToGallery(Bitmap bitmap) {
        new Thread(() -> {
            String name = "CamStudy_" + System.currentTimeMillis() + ".jpg";
            OutputStream fos = null;

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Scoped storage for Android 10+
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.Images.Media.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES + "/CamStudy");
                    Uri imageUri = requireContext().getContentResolver()
                            .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    if (imageUri != null) {
                        fos = requireContext().getContentResolver().openOutputStream(imageUri);
                    }
                } else {
                    // Legacy external storage for Android 9 or lower
                    File dir = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES), "CamStudy");
                    if (!dir.exists()) dir.mkdirs();
                    File image = new File(dir, name);
                    fos = new FileOutputStream(image);

                    requireContext().sendBroadcast(
                            new android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                    Uri.fromFile(image))
                    );
                }

                if (fos != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.close();
                }

                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                                "Photo saved to gallery!", Toast.LENGTH_SHORT).show());

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                                "Failed to save photo", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void proceedToTranslation() {
        Bundle b = new Bundle();
        b.putStringArray("detected_objects", detectedObjectsList.toArray(new String[0]));
        b.putString("photo_uri", photoUri);
        NavController navController =
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
        navController.navigate(R.id.action_photoPreviewFragment_to_translationFragment, b);
    }
}
