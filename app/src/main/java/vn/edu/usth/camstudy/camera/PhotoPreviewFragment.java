/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: CamStudy
 * File: PhotoPreviewFragment.java
 * Last Modified: 5/10/2025 11:00
 */

package vn.edu.usth.camstudy.camera;

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

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import vn.edu.usth.camstudy.R;

/**
 * PhotoPreviewFragment
 * ---------------------
 * Displays the photo taken by CameraFragment,
 * performs object detection using YOLOv5,
 * and allows the user to save or proceed to translation.
 *
 * Hiển thị ảnh vừa chụp từ CameraFragment,
 * chạy nhận dạng vật thể bằng YOLOv5,
 * và cho phép lưu hoặc dịch sang văn bản.
 */
public class PhotoPreviewFragment extends Fragment {

    private static final String TAG = "PhotoPreviewFragment";

    // Argument keys from navigation
    // Khóa truyền tham số qua Navigation
    private static final String ARG_PHOTO_URI = "photo_uri";
    private static final String ARG_IS_TEMP = "is_temp";

    // UI components
    // Thành phần giao diện
    private ImageView imgPreview;
    private TextView txtDetectedObjects;
    private FloatingActionButton btnSave;
    private ExtendedFloatingActionButton btnProceedTranslation;

    // YOLO model and related data
    // Mô hình YOLO và dữ liệu liên quan
    private YOLOv5Classifier yoloClassifier;
    private String photoUri;
    private boolean isTemp = false;
    private Bitmap currentBitmap;
    private final List<String> detectedObjectsList = new ArrayList<>();

    // Launcher for selecting a photo manually (optional)
    // Bộ chọn ảnh thủ công (nếu cần)
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    loadAndDetectObjects(uri.toString());
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve arguments from bundle
        // Lấy dữ liệu từ bundle truyền sang
        if (getArguments() != null) {
            photoUri = getArguments().getString(ARG_PHOTO_URI);
            isTemp = getArguments().getBoolean(ARG_IS_TEMP, false);
        }

        // Initialize YOLOv5 classifier
        // Khởi tạo bộ phân loại YOLOv5
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

        // Initialize views
        // Ánh xạ các thành phần giao diện
        imgPreview = view.findViewById(R.id.img_preview);
        txtDetectedObjects = view.findViewById(R.id.txt_detected_objects);
        FloatingActionButton btnBack = view.findViewById(R.id.btn_back_to_camera);
        btnSave = view.findViewById(R.id.btn_save_photo);
        btnProceedTranslation = view.findViewById(R.id.btn_proceed_translation);

        // Back button: delete temp file and return to camera
        // Nút quay lại: xóa ảnh tạm (nếu có) và quay lại CameraFragment
        btnBack.setOnClickListener(v -> {
            if (isTemp && photoUri != null) {
                try {
                    File tempFile = new File(Uri.parse(photoUri).getPath());
                    if (tempFile.exists()) tempFile.delete();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to delete temp file", e);
                }
            }
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigateUp();
        });

        // Show save button only if this is a temporary photo
        // Hiển thị nút Lưu chỉ khi là ảnh tạm thời
        btnSave.setVisibility(isTemp ? View.VISIBLE : View.GONE);
        btnSave.setOnClickListener(v -> savePhoto());

        // Proceed to translation
        // Nút sang bước dịch thuật
        btnProceedTranslation.setOnClickListener(v -> proceedToTranslation());

        // Load and analyze the photo
        // Tải ảnh và chạy nhận dạng vật thể
        if (photoUri != null) {
            loadAndDetectObjects(photoUri);
        }

        return view;
    }

    /**
     * Load image from URI and run object detection.
     * Tải ảnh từ URI và chạy mô hình nhận dạng.
     */
    private void loadAndDetectObjects(String uriString) {
        try {
            Uri uri = Uri.parse(uriString);
            Bitmap bitmap = loadBitmapFromUri(uri);
            if (bitmap == null) {
                Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                return;
            }

            currentBitmap = bitmap;
            imgPreview.setImageBitmap(bitmap); // show original

            // Run YOLO detection
            // Chạy mô hình YOLO
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

    /**
     * Decode bitmap safely from URI.
     * Giải mã ảnh từ URI (tương thích nhiều Android version).
     */
    private Bitmap loadBitmapFromUri(Uri uri) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.Source source = ImageDecoder.createSource(requireContext().getContentResolver(), uri);
            return ImageDecoder.decodeBitmap(source);
        } else {
            return MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
        }
    }

    /**
     * Run YOLOv5 detection on a background thread.
     * Thực hiện nhận dạng YOLOv5 trong luồng nền.
     */
    private void detectObjects(Bitmap bitmap) {
        txtDetectedObjects.setText(R.string.analyzing_image);
        new Thread(() -> {
            try {
                Log.d(TAG, "Starting object detection...");

                // Ensure bitmap is ARGB_8888 and mutable
                // Chuyển định dạng ảnh nếu cần (để mô hình đọc được)
                Bitmap processed = bitmap.copy(Bitmap.Config.ARGB_8888, true);

                // Detect objects
                // Chạy mô hình phát hiện vật thể
                List<YOLOv5Classifier.Result> results = yoloClassifier.detect(processed);

                // Collect unique labels
                // Gom các nhãn không trùng
                Set<String> labels = new LinkedHashSet<>();
                for (YOLOv5Classifier.Result r : results) labels.add(r.label);

                // Update UI
                // Cập nhật giao diện trên luồng chính
                requireActivity().runOnUiThread(() -> {
                    if (labels.isEmpty()) {
                        txtDetectedObjects.setText(R.string.no_objects_detected);
                        showNoDetectionDialog(); // offer manual translation
                    } else {
                        String text = "Detected: " + String.join(", ", labels);
                        txtDetectedObjects.setText(text);
                    }

                    // Draw detection boxes on image
                    // Vẽ khung phát hiện vật thể lên ảnh
                    if (!results.isEmpty()) {
                        Bitmap annotated = yoloClassifier.drawDetections(processed, results);
                        imgPreview.setImageBitmap(annotated);
                        currentBitmap = annotated;
                    }
                });

                // Save detected labels for later translation
                // Lưu danh sách vật thể để dịch sau
                detectedObjectsList.clear();
                detectedObjectsList.addAll(labels);

            } catch (Exception e) {
                Log.e(TAG, "Detection failed", e);
                requireActivity().runOnUiThread(() -> {
                    txtDetectedObjects.setText(R.string.detection_failed);
                    showNoDetectionDialog();
                });
            }
        }).start();
    }

    /**
     * Show dialog when no objects detected.
     * Hiển thị hộp thoại khi không phát hiện vật thể nào.
     */
    private void showNoDetectionDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("No Objects Detected")
                .setMessage("Would you like to translate your own word instead?")
                .setPositiveButton("Yes", (d, w) -> showManualInputDialog())
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .show();
    }

    /**
     * Ask user to input a custom word for translation.
     * Hộp thoại cho phép người dùng nhập từ cần dịch.
     */
    private void showManualInputDialog() {
        final android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("Enter word to translate");

        androidx.appcompat.app.AlertDialog dialog =
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Enter Text to Translate")
                        .setView(input)
                        .setPositiveButton("Translate", null)
                        .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                        .create();

        // Validate input before navigating
        // Kiểm tra dữ liệu trước khi chuyển sang màn hình dịch
        dialog.setOnShowListener(dlg -> {
            android.widget.Button btn = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            btn.setOnClickListener(v -> {
                String text = input.getText().toString().trim();
                if (text.isEmpty()) {
                    input.setError("Please enter a word");
                    return;
                }
                Bundle b = new Bundle();
                b.putStringArray("detected_objects", new String[0]);
                b.putString("photo_uri", photoUri);
                b.putString("user_input_text", text);
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                        .navigate(R.id.action_photoPreviewFragment_to_translationFragment, b);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    /**
     * Save the current photo to gallery (scoped storage or legacy).
     * Lưu ảnh hiện tại vào thư viện (theo chuẩn Android mới hoặc cũ).
     */
    private void savePhoto() {
        if (currentBitmap == null) {
            Toast.makeText(requireContext(), "No photo to save", Toast.LENGTH_SHORT).show();
            return;
        }

        // For old Android versions, check write permission
        // Android cũ cần quyền ghi bộ nhớ
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Storage permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                        .format(System.currentTimeMillis());
                Uri savedUri = null;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Scoped storage (modern Android)
                    // Lưu ảnh bằng MediaStore (Android 10+)
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.Images.Media.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES + "/CamStudy");

                    savedUri = requireContext().getContentResolver()
                            .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                    if (savedUri != null) {
                        try (OutputStream out =
                                     requireContext().getContentResolver().openOutputStream(savedUri)) {
                            currentBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                        }
                    }
                } else {
                    // Legacy external storage
                    // Lưu ảnh vào bộ nhớ ngoài cũ
                    File dir = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES), "CamStudy");
                    if (!dir.exists()) dir.mkdirs();
                    File photoFile = new File(dir, name + ".jpg");
                    try (FileOutputStream out = new FileOutputStream(photoFile)) {
                        currentBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    }
                    savedUri = Uri.fromFile(photoFile);
                }

                // Delete temp file and notify user
                // Xóa ảnh tạm và thông báo cho người dùng
                if (savedUri != null) {
                    if (isTemp && photoUri != null) {
                        File tmp = new File(Uri.parse(photoUri).getPath());
                        if (tmp.exists()) tmp.delete();
                    }

                    Uri finalUri = savedUri;
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Photo saved to gallery!", Toast.LENGTH_SHORT).show();
                        btnSave.setVisibility(View.GONE);
                        isTemp = false;
                        photoUri = finalUri.toString();
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed to save photo", e);
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Failed to save photo: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    /**
     * Proceed to TranslationFragment with detected objects.
     * Chuyển sang màn hình dịch với danh sách vật thể đã phát hiện.
     */
    private void proceedToTranslation() {
        Bundle b = new Bundle();
        String[] arr = detectedObjectsList.toArray(new String[0]);
        b.putStringArray("detected_objects", arr);
        b.putString("photo_uri", photoUri);
        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                .navigate(R.id.action_photoPreviewFragment_to_translationFragment, b);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (yoloClassifier != null) yoloClassifier.close();
    }
}
