package com.kapstranspvtltd.kaps_partner.handyman_agent_activities.documents.handyman_documents;

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

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.util.concurrent.ListenableFuture;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityDriverAgentDrivingLicenseUploadBinding;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityHandyManDrivingLicenseUploadBinding;
import com.kapstranspvtltd.kaps_partner.databinding.DialogCameraPreviewBinding;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleyFileUploadRequest;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.utils.ImageCompressor;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HandyManDrivingLicenseUploadActivity extends AppCompatActivity {

    private static final String TAG = "HandyManDrivingLicenseUploadActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    private ActivityHandyManDrivingLicenseUploadBinding binding;
    private PreferenceManager preferenceManager;
    private ExecutorService cameraExecutor;

    private ImageCapture imageCapture;
    private Uri licenseFrontUri;
    private Uri licenseBackUri;
    private String previousLicenseFront;
    private String previousLicenseBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHandyManDrivingLicenseUploadBinding.inflate(getLayoutInflater());
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
        binding.licenseFrontContainer.setOnClickListener(v -> showCameraPreview(true));
        binding.licenseBackContainer.setOnClickListener(v -> showCameraPreview(false));
        binding.updateButton.setOnClickListener(v -> saveLicenseDetails());
    }

    private void loadSavedData() {
        previousLicenseFront = preferenceManager.getStringValue("handyman_agent_license_front_photo_url");
        previousLicenseBack = preferenceManager.getStringValue("handyman_agent_license_back_photo_url");
        binding.licenseNumberInput.setText(preferenceManager.getStringValue("handyman_agent_license_no"));

        loadImage(previousLicenseFront, binding.licenseFrontImage, binding.licenseFrontPlaceholder);
        loadImage(previousLicenseBack, binding.licenseBackImage, binding.licenseBackPlaceholder);
    }

    private void loadImage(String url, ImageView imageView, View placeholder) {
        if (url != null && !url.isEmpty()) {
            Glide.with(this)
                    .load(url)
                    .into(imageView);
            placeholder.setVisibility(View.GONE);
        }
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
        if (isFront) {
            licenseFrontUri = uri;
            loadImage(uri.toString(), binding.licenseFrontImage, binding.licenseFrontPlaceholder);
        } else {
            licenseBackUri = uri;
            loadImage(uri.toString(), binding.licenseBackImage, binding.licenseBackPlaceholder);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(System.currentTimeMillis());
        File storageDir = getExternalFilesDir(null);
        return File.createTempFile("LICENSE_" + timeStamp + "_", ".jpg", storageDir);
    }
    private void uploadImage(Uri uri, boolean isFront) {
        AlertDialog loadingDialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Uploading " + (isFront ? "Front" : "Back") + " Image")
                .setMessage("Please wait...")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        try {
            VolleyFileUploadRequest request = createUploadRequest(uri, isFront, loadingDialog);
            VolleySingleton.getInstance(this).addToRequestQueue(request);
        } catch (Exception e) {
            Log.e(TAG, "Upload preparation failed", e);
            loadingDialog.dismiss();
            showToast("Error preparing image");
        }
    }

    private VolleyFileUploadRequest createUploadRequest(Uri uri, boolean isFront, Dialog loadingDialog) {
        return new VolleyFileUploadRequest(
                Request.Method.POST,
                APIClient.baseImageUrl + "/upload",
                response -> handleUploadSuccess(response, isFront, loadingDialog),
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

                    String fileName = createFileName(isFront);
                    params.put("image", new DataPart(fileName, compressed.bytes, "image/jpeg"));

                    Log.d(TAG, String.format("Compressed image: %dx%d, quality: %d%%, size: %.2fKB",
                            compressed.width,
                            compressed.height,
                            compressed.quality,
                            compressed.bytes.length / 1024f
                    ));

                } catch (IOException e) {
                    Log.e(TAG, "Error compressing image", e);
                }
                return params;
            }
        };
    }

    private String createFileName(boolean isFront) {
        String driverName = preferenceManager.getStringValue("handyman_agent_name");
        if (driverName.isEmpty()) driverName = "NA";
        String driverId = preferenceManager.getStringValue("handyman_agent_id");
        return "handyman_agent_id_" + driverId + "_" + driverName + "_license_" +
                (isFront ? "front" : "back") + ".jpg";
    }

    private void handleUploadSuccess(NetworkResponse response, boolean isFront, Dialog dialog) {
        try {
            String jsonString = new String(response.data);
            JSONObject jsonObject = new JSONObject(jsonString);
            String imageUrl = jsonObject.getString("image_url");
            updatePreferencesAndUI(imageUrl, isFront);
            dialog.dismiss();
            showToast((isFront ? "Front" : "Back") + " image uploaded successfully");
            if(isFront == false){
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Upload response processing failed", e);
            dialog.dismiss();
            showToast("Failed to process response");
        }
    }

    private void handleUploadError(VolleyError error, Dialog dialog) {
        Log.e(TAG, "Upload failed", error);
        dialog.dismiss();
        showToast("Failed to upload image");
    }

    private void updatePreferencesAndUI(String imageUrl, boolean isFront) {
        if (isFront) {
            preferenceManager.saveStringValue("handyman_agent_license_front_photo_url", imageUrl);
            previousLicenseFront = imageUrl;
            loadImage(imageUrl, binding.licenseFrontImage, binding.licenseFrontPlaceholder);
        } else {
            preferenceManager.saveStringValue("handyman_agent_license_back_photo_url", imageUrl);
            previousLicenseBack = imageUrl;
            loadImage(imageUrl, binding.licenseBackImage, binding.licenseBackPlaceholder);
        }
    }

    private void saveLicenseDetails() {
        String licenseNo = binding.licenseNumberInput.getText().toString().trim();

        if (licenseNo.isEmpty()) {
            showToast("Please provide your Driving License number");
            return;
        }

        if (isNewEntry()) {
            if (licenseFrontUri == null) {
                showToast("Please select and upload your Driving License front image");
                return;
            }
            if (licenseBackUri == null) {
                showToast("Please select and upload your Driving License back image");
                return;
            }
        }

        if (needsFrontUpload() && licenseFrontUri != null) {
            uploadImage(licenseFrontUri, true);
        }

        if (needsBackUpload() && licenseBackUri != null) {
            uploadImage(licenseBackUri, false);
        }

        preferenceManager.saveStringValue("handyman_agent_license_no", licenseNo);

        if (!needsFrontUpload() && !needsBackUpload()) {
            showToast("License details saved successfully");
            finish();
        }
    }

    private boolean isNewEntry() {
        return preferenceManager.getStringValue("handyman_agent_license_no").isEmpty() &&
                (previousLicenseFront == null || previousLicenseFront.isEmpty() ||
                        previousLicenseBack == null || previousLicenseBack.isEmpty());
    }

    private boolean needsFrontUpload() {
        return isNewEntry() ||
                (licenseFrontUri != null &&
                        (previousLicenseFront == null ||
                                !previousLicenseFront.equals(licenseFrontUri.toString())));
    }

    private boolean needsBackUpload() {
        return isNewEntry() ||
                (licenseBackUri != null &&
                        (previousLicenseBack == null ||
                                !previousLicenseBack.equals(licenseBackUri.toString())));
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