// app/src/main/java/vn/edu/usth/myapplication/EmbeddedCameraFragment.java
package vn.edu.usth.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmbeddedCameraFragment extends Fragment {
    private static final String TAG = "EmbeddedCameraFragment";
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private PreviewView previewView;
    private View permissionLayout, zoomControlLayout;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
    private CameraControl cameraControl;
    private CameraInfo cameraInfo;
    private SeekBar zoomSlider;
    private TextView txtZoomLevel;
    private Button btnZoom1x, btnZoom2x;
    private ScaleGestureDetector scaleGestureDetector;
    private float currentZoomRatio = 1.0f;
    private SharedPreferences sharedPreferences;

    private final ActivityResultLauncher<String[]> requestPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (result.values().stream().allMatch(Boolean::booleanValue)) startCamera();
                else { showPermissionLayout(); Toast.makeText(requireContext(), "Permissions denied", Toast.LENGTH_SHORT).show(); }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_camera, container, false);
        previewView = v.findViewById(R.id.preview_view);
        permissionLayout = v.findViewById(R.id.permission_layout);
        zoomSlider = v.findViewById(R.id.zoom_slider);
        txtZoomLevel = v.findViewById(R.id.txt_zoom_level);
        zoomControlLayout = v.findViewById(R.id.zoom_control);
        btnZoom1x = v.findViewById(R.id.btn_zoom_1x);
        btnZoom2x = v.findViewById(R.id.btn_zoom_2x);
        FloatingActionButton btnCapture = v.findViewById(R.id.btn_capture);
        FloatingActionButton btnSwitchCamera = v.findViewById(R.id.btn_switch_camera);
        FloatingActionButton btnFlash = v.findViewById(R.id.btn_flash);
        MaterialButton btnGrantPermission = v.findViewById(R.id.btn_grant_permission);
        sharedPreferences = requireContext().getSharedPreferences("PhotoMagicPrefs", Context.MODE_PRIVATE);

        btnCapture.setOnClickListener(x -> takePhoto());
        btnSwitchCamera.setOnClickListener(x -> switchCamera());
        btnGrantPermission.setOnClickListener(x -> requestAppPermissions());
        btnFlash.setOnClickListener(x -> toggleFlash(btnFlash));

        setupZoomControl();
        setupPinchToZoom();
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (allPermissionsGranted()) startCamera(); else showPermissionLayout();
        return v;
    }

    private void toggleFlash(FloatingActionButton btnFlash) {
        boolean newState = !sharedPreferences.getBoolean("flash_mode", false);
        sharedPreferences.edit().putBoolean("flash_mode", newState).apply();
        if (cameraControl != null) cameraControl.enableTorch(newState);
    }

    private void setupZoomControl() {
        zoomSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean f) {
                if (cameraControl != null && cameraInfo != null) {
                    ZoomState zs = cameraInfo.getZoomState().getValue();
                    float min = zs.getMinZoomRatio(), max = zs.getMaxZoomRatio();
                    currentZoomRatio = min + (p / 100f) * (max - min);
                    cameraControl.setZoomRatio(currentZoomRatio);
                    txtZoomLevel.setText(String.format(Locale.US, "%.1fx", currentZoomRatio));
                }
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        btnZoom1x.setOnClickListener(x -> setZoomRatio(1.0f));
        btnZoom2x.setOnClickListener(x -> setZoomRatio(2.0f));
    }

    private void setupPinchToZoom() {
        scaleGestureDetector = new ScaleGestureDetector(requireContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(@NonNull ScaleGestureDetector d) {
                if (cameraInfo != null && cameraControl != null) {
                    ZoomState zs = cameraInfo.getZoomState().getValue();
                    float min = zs.getMinZoomRatio(), max = zs.getMaxZoomRatio();
                    currentZoomRatio = Math.max(min, Math.min(currentZoomRatio * d.getScaleFactor(), max));
                    cameraControl.setZoomRatio(currentZoomRatio);
                    updateZoomUI(currentZoomRatio, min, max);
                }
                return true;
            }
        });
        previewView.setOnTouchListener((v, e) -> { scaleGestureDetector.onTouchEvent(e); return true; });
    }

    private void setZoomRatio(float targetZoom) {
        if (cameraControl != null && cameraInfo != null) {
            ZoomState zs = cameraInfo.getZoomState().getValue();
            float min = zs.getMinZoomRatio(), max = zs.getMaxZoomRatio();
            currentZoomRatio = Math.max(min, Math.min(targetZoom, max));
            cameraControl.setZoomRatio(currentZoomRatio);
            updateZoomUI(currentZoomRatio, min, max);
        }
    }

    private void updateZoomUI(float zoomRatio, float min, float max) {
        txtZoomLevel.setText(String.format(Locale.US, "%.1fx", zoomRatio));
        zoomSlider.setProgress((int) ((zoomRatio - min) / (max - min) * 100));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (allPermissionsGranted()) startCamera(); else showPermissionLayout();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cameraControl != null) cameraControl.enableTorch(false);
        sharedPreferences.edit().putBoolean("flash_mode", false).apply();
    }

    private void requestAppPermissions() {
        List<String> req = new ArrayList<>();
        req.add(Manifest.permission.CAMERA);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            req.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        requestPermissionsLauncher.launch(req.toArray(new String[0]));
    }

    private void startCamera() {
        permissionLayout.setVisibility(View.GONE);
        previewView.setVisibility(View.VISIBLE);
        ListenableFuture<ProcessCameraProvider> f = ProcessCameraProvider.getInstance(requireContext());
        f.addListener(() -> {
            try { bindCameraUseCases(f.get()); }
            catch (Exception e) {
                Log.e(TAG, "Use case binding failed", e);
                Toast.makeText(requireContext(), "Failed to start camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
                showPermissionLayout();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCameraUseCases(ProcessCameraProvider provider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetRotation(previewView.getDisplay().getRotation())
                .build();
        try {
            provider.unbindAll();
            Camera camera = provider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            cameraControl = camera.getCameraControl();
            cameraInfo = camera.getCameraInfo();
            boolean flashEnabled = sharedPreferences.getBoolean("flash_mode", false);
            cameraControl.enableTorch(flashEnabled);
            updateFlashMode();
            initializeZoomControl();
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
            Toast.makeText(requireContext(), "Camera bind failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeZoomControl() {
        if (cameraInfo != null) {
            ZoomState zs = cameraInfo.getZoomState().getValue();
            float min = zs.getMinZoomRatio(), max = zs.getMaxZoomRatio();
            currentZoomRatio = min;
            zoomSlider.setProgress(0);
            txtZoomLevel.setText(String.format(Locale.US, "%.1fx", min));
            zoomControlLayout.setVisibility(max > min ? View.VISIBLE : View.GONE);
            btnZoom2x.setEnabled(max >= 2.0f);
        }
    }

    private void updateFlashMode() {
        if (cameraControl != null && imageCapture != null) {
            imageCapture.setFlashMode(sharedPreferences.getBoolean("flash_mode", false) ?
                    ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF);
        }
    }

    private void takePhoto() {
        if (imageCapture == null) {
            Toast.makeText(requireContext(), "Camera is not ready", Toast.LENGTH_SHORT).show();
            return;
        }
        updateFlashMode();
        String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis());
        File photoFile = new File(requireContext().getCacheDir(), "temp_" + name + ".jpg");
        ImageCapture.OutputFileOptions opts = new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        imageCapture.takePicture(opts, ContextCompat.getMainExecutor(requireContext()), new ImageCapture.OnImageSavedCallback() {
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                Bundle args = new Bundle();
                args.putString("photo_uri", Uri.fromFile(photoFile).toString());
                args.putLong("timestamp", System.currentTimeMillis());
                args.putBoolean("is_temp", true);
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigate(R.id.nav_photo_preview, args);
            }
            public void onError(@NonNull ImageCaptureException e) {
                Log.e(TAG, "Photo capture failed: " + e.getMessage(), e);
                String msg = "Photo capture failed!";
                if (e.getMessage() != null && e.getMessage().contains("CAMERA_CLOSED")) {
                    msg = "Camera was closed. Please try again."; startCamera();
                } else if (e.getMessage() != null && e.getMessage().contains("FILE_IO_ERROR")) {
                    msg = "Storage error. Check storage permissions.";
                }
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void switchCamera() {
        cameraSelector = cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA ?
                CameraSelector.DEFAULT_FRONT_CAMERA : CameraSelector.DEFAULT_BACK_CAMERA;
        startCamera();
    }

    private boolean allPermissionsGranted() {
        boolean cam = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        if (!cam) return false;
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void showPermissionLayout() {
        permissionLayout.setVisibility(View.VISIBLE);
        previewView.setVisibility(View.GONE);
        zoomControlLayout.setVisibility(View.GONE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }
}
