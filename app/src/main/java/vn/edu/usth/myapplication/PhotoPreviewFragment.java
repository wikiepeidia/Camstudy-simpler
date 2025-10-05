/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: PhotoPreviewFragment.java
 * Last Modified: 5/10/2025 5:27
 */

package vn.edu.usth.myapplication;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PhotoPreviewFragment extends Fragment {

    private static final String TAG = "PhotoPreviewFragment";
    private static final String ARG_PHOTO_URI = "photo_uri";
    private static final String ARG_TIMESTAMP = "timestamp";
    private static final String ARG_IS_TEMP = "is_temp";

    private ImageView imgPreview;
    private TextView txtDetectedObjects;
    private FloatingActionButton btnSave;
    private YOLOv5Classifier yoloClassifier;
    private String photoUri;
    private boolean isTemp = false;
    private Bitmap currentBitmap;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    loadAndDetectObjects(uri.toString());
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            photoUri = getArguments().getString(ARG_PHOTO_URI);
            isTemp = getArguments().getBoolean(ARG_IS_TEMP, false);
        }

        // Initialize YOLO classifier
        try {
            yoloClassifier = new YOLOv5Classifier(requireContext().getAssets(), "yolov5s-fp16.tflite");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load YOLO model", e);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_preview, container, false);

        imgPreview = view.findViewById(R.id.img_preview);
        txtDetectedObjects = view.findViewById(R.id.txt_detected_objects);
        FloatingActionButton btnBack = view.findViewById(R.id.btn_back_to_camera);
        btnSave = view.findViewById(R.id.btn_save_photo);

        btnBack.setOnClickListener(v -> {
            // Delete temp file if exists
            if (isTemp && photoUri != null) {
                try {
                    File tempFile = new File(Uri.parse(photoUri).getPath());
                    if (tempFile.exists()) {
                        tempFile.delete();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to delete temp file", e);
                }
            }
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            navController.navigateUp();
        });

        // Show/hide save button based on whether this is a temp photo
        btnSave.setVisibility(isTemp ? View.VISIBLE : View.GONE);
        btnSave.setOnClickListener(v -> savePhoto());

        // Load and display the photo
        if (photoUri != null) {
            loadAndDetectObjects(photoUri);
        }

        return view;
    }

    private void loadAndDetectObjects(String uriString) {
        try {
            Uri uri = Uri.parse(uriString);
            Bitmap bitmap = loadBitmapFromUri(uri);

            if (bitmap == null) {
                Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                return;
            }

            currentBitmap = bitmap;

            // Display original image
            imgPreview.setImageBitmap(bitmap);

            // Detect objects
            if (yoloClassifier != null) {
                detectObjects(bitmap);
            } else {
                txtDetectedObjects.setText(R.string.object_detection_unavailable);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading image", e);
            Toast.makeText(requireContext(), "Error loading image", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap loadBitmapFromUri(Uri uri) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.Source source = ImageDecoder.createSource(requireContext().getContentResolver(), uri);
            return ImageDecoder.decodeBitmap(source);
        } else {
            return MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
        }
    }

    private void detectObjects(Bitmap bitmap) {
        txtDetectedObjects.setText(R.string.analyzing_image);

        new Thread(() -> {
            try {
                Log.d(TAG, "Starting object detection...");
                // Perform object detection
                List<YOLOv5Classifier.Result> results = yoloClassifier.detect(bitmap);
                Log.d(TAG, "Detection complete. Found " + results.size() + " objects");

                // Extract unique labels
                Set<String> uniqueLabels = new LinkedHashSet<>();
                for (YOLOv5Classifier.Result result : results) {
                    uniqueLabels.add(result.label);
                    Log.d(TAG, "Detected: " + result.label + " with confidence " + (result.conf * 100) + "%");
                }

                // Update UI on main thread
                requireActivity().runOnUiThread(() -> {
                    if (uniqueLabels.isEmpty()) {
                        txtDetectedObjects.setText(R.string.no_objects_detected);
                    } else {
                        String detectedText = "Detected: " + String.join(", ", uniqueLabels);
                        txtDetectedObjects.setText(detectedText);
                    }

                    // Draw bounding boxes on image
                    if (!results.isEmpty()) {
                        Bitmap annotatedBitmap = yoloClassifier.drawDetections(bitmap, results);
                        imgPreview.setImageBitmap(annotatedBitmap);
                        currentBitmap = annotatedBitmap;
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error during object detection", e);
                requireActivity().runOnUiThread(() ->
                        txtDetectedObjects.setText(R.string.detection_failed));
            }
        }).start();
    }

    private void savePhoto() {
        if (currentBitmap == null) {
            Toast.makeText(requireContext(), "No photo to save", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check storage permission for older Android versions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Storage permission required", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        new Thread(() -> {
            try {
                String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                        .format(System.currentTimeMillis());

                Uri savedUri = null;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Use scoped storage via MediaStore
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                    contentValues.put(MediaStore.Images.Media.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES + "/CamStudy");

                    savedUri = requireContext().getContentResolver().insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

                    if (savedUri != null) {
                        try (OutputStream out = requireContext().getContentResolver().openOutputStream(savedUri)) {
                            currentBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                        }
                    }
                } else {
                    // Legacy external storage
                    File dir = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES), "CamStudy");
                    if (!dir.exists() && !dir.mkdirs()) {
                        throw new IOException("Cannot create directory");
                    }
                    File photoFile = new File(dir, name + ".jpg");
                    try (FileOutputStream out = new FileOutputStream(photoFile)) {
                        currentBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    }
                    savedUri = Uri.fromFile(photoFile);

                    // Trigger media scan
                    android.media.MediaScannerConnection.scanFile(requireContext(),
                            new String[]{photoFile.getAbsolutePath()},
                            new String[]{"image/jpeg"}, null);
                }

                if (savedUri != null) {
                    long timestamp = System.currentTimeMillis();
                    new PhotoDatabase(requireContext()).savePhoto(savedUri.toString(), timestamp);

                    // Delete temp file
                    if (isTemp && photoUri != null) {
                        try {
                            File tempFile = new File(Uri.parse(photoUri).getPath());
                            if (tempFile.exists()) {
                                tempFile.delete();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to delete temp file", e);
                        }
                    }

                    Uri finalSavedUri = savedUri;
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Photo saved to gallery!", Toast.LENGTH_SHORT).show();
                        btnSave.setVisibility(View.GONE);
                        isTemp = false;
                        photoUri = finalSavedUri.toString();
                    });
                } else {
                    throw new IOException("Failed to create file");
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to save photo", e);
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Failed to save photo: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (yoloClassifier != null) {
            yoloClassifier.close();
        }
    }
}
