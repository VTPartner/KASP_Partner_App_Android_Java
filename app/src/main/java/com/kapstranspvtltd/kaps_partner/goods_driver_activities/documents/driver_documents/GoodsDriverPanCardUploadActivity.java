package com.kapstranspvtltd.kaps_partner.goods_driver_activities.documents.driver_documents;

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
import com.kapstranspvtltd.kaps_partner.databinding.ActivityGoodsDriverPanCardUploadBinding;
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

public class GoodsDriverPanCardUploadActivity extends AppCompatActivity {
    private static final String TAG = "GoodsDriverPanCardUploadActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    private ActivityGoodsDriverPanCardUploadBinding binding;
    private PreferenceManager preferenceManager;
    private ExecutorService cameraExecutor;

    private ImageCapture imageCapture;
    private Uri panFrontUri;
    private Uri panBackUri;
    private String previousPanFront;
    private String previousPanBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGoodsDriverPanCardUploadBinding.inflate(getLayoutInflater());
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
        binding.panFrontContainer.setOnClickListener(v -> showCameraPreview(true));
        binding.panBackContainer.setOnClickListener(v -> showCameraPreview(false));
        binding.updateButton.setOnClickListener(v -> savePanDetails());
    }

    private void loadSavedData() {
        previousPanFront = preferenceManager.getStringValue("pan_front_photo_url");
        previousPanBack = preferenceManager.getStringValue("pan_back_photo_url");
        binding.panNumberInput.setText(preferenceManager.getStringValue("pan_no"));

        loadImage(previousPanFront, binding.panFrontImage, binding.panFrontPlaceholder);
        loadImage(previousPanBack, binding.panBackImage, binding.panBackPlaceholder);
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

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(System.currentTimeMillis());
        File storageDir = getExternalFilesDir(null);
        return File.createTempFile("PAN_" + timeStamp + "_", ".jpg", storageDir);
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
                        showToast("Failed to capture image");
                    }
                }
        );
    }

    private void handleCapturedImage(Uri uri, boolean isFront) {
        if (isFront) {
            panFrontUri = uri;
            loadImage(uri.toString(), binding.panFrontImage, binding.panFrontPlaceholder);
        } else {
            panBackUri = uri;
            loadImage(uri.toString(), binding.panBackImage, binding.panBackPlaceholder);
        }
    }

    private void savePanDetails() {
        String panNo = binding.panNumberInput.getText().toString().trim();

        if (panNo.isEmpty()) {
            showToast("Please provide your PAN number");
            return;
        }

        if (isNewEntry()) {
            if (panFrontUri == null) {
                showToast("Please select and upload your PAN Card front image");
                return;
            }
            if (panBackUri == null) {
                showToast("Please select and upload your PAN Card back image");
                return;
            }
        }

        if (needsFrontUpload() && panFrontUri != null) {
            uploadImage(panFrontUri, true);
        }

        if (needsBackUpload() && panBackUri != null) {
            uploadImage(panBackUri, false);
        }

        preferenceManager.saveStringValue("pan_no", panNo);

        if (!needsFrontUpload() && !needsBackUpload()) {
            showToast("PAN Card details saved successfully");
            finish();
        }
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
            handleUploadError(e, loadingDialog);
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
        String driverName = preferenceManager.getStringValue("goods_driver_name");
        if (driverName.isEmpty()) driverName = "NA";
        String driverId = preferenceManager.getStringValue("goods_driver_id");
        return "goods_driver_id_" + driverId + "_" + driverName + "_pan_" +
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

    private void updatePreferencesAndUI(String imageUrl, boolean isFront) {
        if (isFront) {
            preferenceManager.saveStringValue("pan_front_photo_url", imageUrl);
            previousPanFront = imageUrl;
            loadImage(imageUrl, binding.panFrontImage, binding.panFrontPlaceholder);
        } else {
            preferenceManager.saveStringValue("pan_back_photo_url", imageUrl);
            previousPanBack = imageUrl;
            loadImage(imageUrl, binding.panBackImage, binding.panBackPlaceholder);
        }
    }

    private boolean isNewEntry() {
        return preferenceManager.getStringValue("pan_no").isEmpty() &&
                (previousPanFront == null || previousPanFront.isEmpty() ||
                        previousPanBack == null || previousPanBack.isEmpty());
    }

    private boolean needsFrontUpload() {
        return isNewEntry() ||
                (panFrontUri != null &&
                        (previousPanFront == null ||
                                !previousPanFront.equals(panFrontUri.toString())));
    }

    private boolean needsBackUpload() {
        return isNewEntry() ||
                (panBackUri != null &&
                        (previousPanBack == null ||
                                !previousPanBack.equals(panBackUri.toString())));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        binding = null;
    }
}