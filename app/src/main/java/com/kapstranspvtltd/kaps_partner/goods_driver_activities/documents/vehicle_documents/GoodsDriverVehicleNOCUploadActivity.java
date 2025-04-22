package com.kapstranspvtltd.kaps_partner.goods_driver_activities.documents.vehicle_documents;

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
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.util.concurrent.ListenableFuture;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleyFileUploadRequest;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.utils.ImageCompressor;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityGoodsDriverVehicleNocuploadBinding;
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

public class GoodsDriverVehicleNOCUploadActivity extends AppCompatActivity {
    private static final String TAG = "GoodsDriverNOCUpload";
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    private ActivityGoodsDriverVehicleNocuploadBinding binding;
    private PreferenceManager preferenceManager;
    private ExecutorService cameraExecutor;
    private ImageCapture imageCapture;
    private Uri nocImageUri;
    private String previousNOC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGoodsDriverVehicleNocuploadBinding.inflate(getLayoutInflater());
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
    }

    private void setupUI() {
        binding.backButton.setOnClickListener(v -> finish());
        binding.nocContainer.setOnClickListener(v -> showCameraPreview(false));
        binding.updateButton.setOnClickListener(v -> saveNOCDetails());
    }

    private void loadSavedData() {
        previousNOC = preferenceManager.getStringValue("noc_photo_url");
        binding.nocNumberInput.setText(preferenceManager.getStringValue("noc_no"));
        loadImage(previousNOC, binding.nocImage, binding.nocPlaceholder);
    }

    private void loadImage(String url, ImageView imageView, View placeholder) {
        if (url != null && !url.isEmpty()) {
            Glide.with(this)
                    .load(url)
                    .into(imageView);
            placeholder.setVisibility(View.GONE);
        }
    }

    private void saveNOCDetails() {
        String nocNo = binding.nocNumberInput.getText().toString().trim();

        if (nocNo.isEmpty()) {
            showToast("Please provide your NOC number");
            return;
        }

        if (isNewEntry() && nocImageUri == null) {
            showToast("Please select and upload your NOC certificate image");
            return;
        }

        if (needsImageUpload() && nocImageUri != null) {
            uploadImage(nocImageUri);
        } else {
            if (!needsImageUpload() && nocNo.equals(preferenceManager.getStringValue("noc_no"))) {
                showToast("No changes made");
                return;
            }
            preferenceManager.saveStringValue("noc_no", nocNo);
            showToast("NOC details saved successfully");
            finish();
        }
    }

    private void uploadImage(Uri uri) {
        Dialog loadingDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Uploading NOC Certificate")
                .setMessage("Please wait...")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        try {
            VolleyFileUploadRequest request = new VolleyFileUploadRequest(
                    Request.Method.POST,
                    APIClient.baseImageUrl + "/upload",
                    response -> handleUploadSuccess(response, loadingDialog),
                    error -> handleUploadError(error, loadingDialog)
            ) {
                @Override
                public Map<String, DataPart> getByteData() {
                    Map<String, DataPart> params = new HashMap<>();
                    try {
                        ImageCompressor.CompressedImage compressed = ImageCompressor.compress(
                                getApplicationContext(),
                                uri,
                                1024 * 1024 // 1MB max
                        );

                        String fileName = createFileName();
                        params.put("image", new DataPart(fileName, compressed.bytes, "image/jpeg"));

                        Log.d(TAG, String.format("Compressed image: %dx%d, quality: %d%%, size: %.2fKB",
                                compressed.width,
                                compressed.height,
                                compressed.quality,
                                compressed.bytes.length / 1024f
                        ));

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

    private String createFileName() {
        String driverName = preferenceManager.getStringValue("goods_driver_name");
        if (driverName.isEmpty()) driverName = "NA";
        String driverId = preferenceManager.getStringValue("goods_driver_id");
        return "goods_driver_id_" + driverId + "_" + driverName + "_noc.jpg";
    }

    private void handleUploadSuccess(NetworkResponse response, Dialog dialog) {
        try {
            String jsonString = new String(response.data);
            JSONObject jsonObject = new JSONObject(jsonString);
            String imageUrl = jsonObject.getString("image_url");
            updatePreferencesAndUI(imageUrl);
            dialog.dismiss();

            String nocNo = binding.nocNumberInput.getText().toString().trim();
            preferenceManager.saveStringValue("noc_no", nocNo);

            showToast("NOC certificate uploaded successfully");
            finish();
        } catch (Exception e) {
            handleUploadError(e, dialog);
        }
    }

    private void handleUploadError(Exception error, Dialog dialog) {
        Log.e(TAG, "Upload failed", error);
        runOnUiThread(() -> {
            dialog.dismiss();
            showToast("Failed to upload image");
        });
    }

    private void updatePreferencesAndUI(String imageUrl) {
        preferenceManager.saveStringValue("noc_photo_url", imageUrl);
        previousNOC = imageUrl;
        loadImage(imageUrl, binding.nocImage, binding.nocPlaceholder);
    }

    private boolean isNewEntry() {
        return preferenceManager.getStringValue("noc_no").isEmpty() &&
                (previousNOC == null || previousNOC.isEmpty());
    }

    private boolean needsImageUpload() {
        return isNewEntry() ||
                (nocImageUri != null &&
                        (previousNOC == null ||
                                !previousNOC.equals(nocImageUri.toString())));
    }

    private void showCameraPreview(boolean isFront) {
        if (!checkCameraPermission()) {
            showPermissionDeniedDialog();
            return;
        }

        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        DialogCameraPreviewBinding dialogBinding = DialogCameraPreviewBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());

        startCamera(dialogBinding.previewView);

        dialogBinding.captureButton.setOnClickListener(v -> {
            takePhoto(isFront);
            dialog.dismiss();
        });

        dialog.show();
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
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                );
            } catch (Exception e) {
                Log.e(TAG, "Camera binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(System.currentTimeMillis());
        File storageDir = getExternalFilesDir(null);
        return File.createTempFile("VEHICLE_NOC_" + timeStamp + "_", ".jpg", storageDir);
    }

    private void takePhoto(boolean isFront) {
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
                        handleCapturedImage(uri, isFront);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed", exception);
                        showToast("Failed to capture image");
                    }
                }
        );
    }

    private void handleCapturedImage(Uri uri, boolean isFront) {
        nocImageUri = uri;
        loadImage(uri.toString(), binding.nocImage, binding.nocPlaceholder);
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