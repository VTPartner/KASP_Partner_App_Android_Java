package com.kapstranspvtltd.kaps_partner.cab_driver_activities.documents.driver_documents;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.util.concurrent.ListenableFuture;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleyFileUploadRequest;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.utils.ImageCompressor;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityCabDriverOwnerSelfieUploadBinding;
import com.kapstranspvtltd.kaps_partner.databinding.DialogCameraPreviewBinding;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CabDriverOwnerSelfieUploadActivity extends AppCompatActivity {

    private static final String TAG = "CabOwnerSelfieUploadActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    private ActivityCabDriverOwnerSelfieUploadBinding binding;
    private PreferenceManager preferenceManager;
    private ExecutorService cameraExecutor;
    private boolean isFrontCamera = true;
    private ImageCapture imageCapture;
    private Uri selfieUri;
    private String previousSelfie;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCabDriverOwnerSelfieUploadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!checkCameraPermission()) {
            requestCameraPermission();
        } else {
            initializeApp();
        }
    }

    private void initializeApp() {
        preferenceManager = new PreferenceManager(this);
        cameraExecutor = Executors.newSingleThreadExecutor();
        setupUI();
        loadSavedData();
        setupCamera();
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeApp();
            } else {
                showPermissionDeniedDialog();
            }
        }
    }

    private void showPermissionDeniedDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Camera Permission Required")
                .setMessage("This app requires camera permission to capture documents. Please grant the permission to continue.")
                .setCancelable(false)
                .setPositiveButton("Grant Permission", (dialog, which) -> requestCameraPermission())
                .setNegativeButton("Exit", (dialog, which) -> finish())
                .show();
    }

    private void setupUI() {
        binding.backButton.setOnClickListener(v -> finish());
        binding.selfieContainer.setOnClickListener(v -> showCameraPreview());
        binding.updateButton.setOnClickListener(v -> saveSelfie());
    }

    private void loadSavedData() {
        previousSelfie = preferenceManager.getStringValue("cab_driver_selfie_photo_url");
        if (previousSelfie != null && !previousSelfie.isEmpty()) {
            loadImage(previousSelfie, binding.selfieImage, binding.selfiePlaceholder);
        }
    }
    private void loadImage(String url, ImageView imageView, View placeholder) {
        if (url != null && !url.isEmpty()) {
            Glide.with(this)
                    .load(url)
                    .into(imageView);
            placeholder.setVisibility(View.GONE);
        }
    }

    private void setupCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        imageCapture
                );
            } catch (Exception e) {
                Log.e(TAG, "Camera setup failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void startCamera(PreviewView previewView) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        isFrontCamera ? CameraSelector.DEFAULT_FRONT_CAMERA
                                : CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                );
            } catch (Exception e) {
                Log.e(TAG, "Camera binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void showCameraPreview() {
        if (!checkCameraPermission()) {
            showPermissionDeniedDialog();
            return;
        }

        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        DialogCameraPreviewBinding dialogBinding = DialogCameraPreviewBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());

        startCamera(dialogBinding.previewView);
        dialogBinding.switchCameraButton.setVisibility(View.VISIBLE);

        dialogBinding.switchCameraButton.setOnClickListener(v -> {
            isFrontCamera = !isFrontCamera;
            startCamera(dialogBinding.previewView);
        });

        dialogBinding.captureButton.setOnClickListener(v -> {
            takePhoto();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void takePhoto() {
        ImageCapture imageCapture = this.imageCapture;
        if (imageCapture == null) return;

        File photoFile;
        try {
            photoFile = createImageFile();
        } catch (IOException e) {
            Log.e(TAG, "Error creating image file", e);
            showToast("Failed to create image file");
            return;
        }

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Uri uri = Uri.fromFile(photoFile);
                        selfieUri = uri;
                        loadImage(uri.toString(), binding.selfieImage, binding.selfiePlaceholder);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        showToast("Failed to capture selfie");
                    }
                }
        );
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(System.currentTimeMillis());
        File storageDir = getExternalFilesDir(null);
        return File.createTempFile("DRIVER_SELFIE_" + timeStamp + "_", ".jpg", storageDir);
    }

    private void saveSelfie() {
        if (selfieUri == null && previousSelfie != null) {
            showToast("This Selfie already exists");
            return;
        }
        if (selfieUri == null) {
            showToast("Please click on camera icon to upload your selfie");
            return;
        }
        uploadImage(selfieUri);
    }

    private void uploadImage(Uri uri) {
        AlertDialog loadingDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Uploading Selfie")
                .setMessage("Please wait...")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        try {
            VolleyFileUploadRequest request = new VolleyFileUploadRequest(
                    Request.Method.POST,
                    APIClient.baseImageUrl + "/upload",
                    response -> {
                        try {
                            JSONObject jsonResponse = new JSONObject(new String(response.data));
                            String imageUrl = jsonResponse.getString("image_url");

                            preferenceManager.saveStringValue("cab_driver_selfie_photo_url", imageUrl);

                            runOnUiThread(() -> {
                                loadingDialog.dismiss();
                                showToast("Selfie uploaded successfully");
                                finish();
                            });
                        } catch (Exception e) {
                            handleUploadError(e, loadingDialog);
                        }
                    },
                    error -> handleUploadError(error, loadingDialog)
            ) {
                @Override
                public Map<String, DataPart> getByteData() throws AuthFailureError {
                    Map<String, DataPart> params = new HashMap<>();
                    try {
                        ImageCompressor.CompressedImage compressed = ImageCompressor.compress(
                                getApplicationContext(),
                                uri,
                                1024 * 1024 // 1MB max
                        );

                        String driverName = preferenceManager.getStringValue("cab_agent_driver_name");
                        if (driverName.isEmpty()) driverName = "NA";
                        String driverId = preferenceManager.getStringValue("cab_driver_id");

                        String fileName = "goods_driver_id_" + driverId + "_" + driverName + "_selfie.jpg";
                        params.put("image", new DataPart(fileName, compressed.bytes, "image/jpeg"));
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading file", e);
                    }
                    return params;
                }
            };

            VolleySingleton.getInstance(this).addToRequestQueue(request);

        } catch (Exception e) {
            handleUploadError(e, loadingDialog);
        }
    }

    private void handleUploadError(Exception error, Dialog dialog) {
        Log.e(TAG, "Upload failed", error);
        runOnUiThread(() -> {
            dialog.dismiss();
            showToast("Failed to upload selfie");
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        binding = null;
    }
}