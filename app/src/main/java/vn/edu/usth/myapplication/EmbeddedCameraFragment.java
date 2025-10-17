package vn.edu.usth.myapplication;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
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
import java.util.concurrent.*;

public class EmbeddedCameraFragment extends Fragment {
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private PreviewView previewView;
    private View permissionLayout;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_camera, container, false);
        previewView = v.findViewById(R.id.preview_view);
        permissionLayout = v.findViewById(R.id.permission_layout);
        FloatingActionButton btnCapture = v.findViewById(R.id.btn_capture);
        MaterialButton btnGrantPermission = v.findViewById(R.id.btn_grant_permission);

        btnCapture.setOnClickListener(x -> takePhoto());
        btnGrantPermission.setOnClickListener(x -> requestPermissions(new String[]{Manifest.permission.CAMERA}, 100));

        cameraExecutor = Executors.newSingleThreadExecutor();
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) startCamera();
        else showPermissionLayout();
        return v;
    }

    private void startCamera() {
        permissionLayout.setVisibility(View.GONE);
        previewView.setVisibility(View.VISIBLE);
        ListenableFuture<ProcessCameraProvider> f = ProcessCameraProvider.getInstance(requireContext());
        f.addListener(() -> {
            try {
                ProcessCameraProvider provider = f.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setTargetRotation(previewView.getDisplay().getRotation())
                        .build();
                provider.unbindAll();
                provider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (Exception ignored) {}
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void takePhoto() {
        if (imageCapture == null) return;
        File photoFile = new File(requireContext().getCacheDir(),
                "temp_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".jpg");
        ImageCapture.OutputFileOptions opts = new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        imageCapture.takePicture(opts, ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Bundle b = new Bundle();
                        b.putString("photo_uri", Uri.fromFile(photoFile).toString());
                        b.putBoolean("is_temp", true);
                        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                                .navigate(R.id.nav_photo_preview, b);
                    }
                    public void onError(@NonNull ImageCaptureException e) {
                        Toast.makeText(requireContext(), "Capture failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showPermissionLayout() {
        permissionLayout.setVisibility(View.VISIBLE);
        previewView.setVisibility(View.GONE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }
}