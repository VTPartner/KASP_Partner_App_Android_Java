package com.kapstranspvtltd.kaps_partner.common_activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityPermissionsBinding;

public class PermissionsActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final int CAMERA_PERMISSION_REQUEST = 1002;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1003;
    private static final int OVERLAY_PERMISSION_REQUEST = 1004;
    private static final int BATTERY_OPTIMIZATION_REQUEST = 1005;

    private ActivityPermissionsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPermissionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupPermissionCards();
        updatePermissionStatuses();

        binding.btnContinue.setOnClickListener(v -> {
            if (checkAllPermissions()) {
                proceedToNextScreen();
            } else {
                showMissingPermissionsDialog();
            }
        });
    }

    private void setupPermissionCards() {
        // Location Permission
        binding.btnLocationPermission.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, LOCATION_PERMISSION_REQUEST);
            }
        });

        // Battery Optimization
        binding.btnBatteryOptimization.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, BATTERY_OPTIMIZATION_REQUEST);
            }
        });

        // Overlay Permission
        binding.btnOverlay.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST);
            }
        });

        // Camera Permission
        binding.btnCamera.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.CAMERA},
                        CAMERA_PERMISSION_REQUEST);
            }
        });

        // Notification Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            binding.btnNotification.setOnClickListener(v -> {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST);
            });
        } else {
            binding.notificationCard.setVisibility(View.GONE);
        }
    }

    private boolean checkAllPermissions() {
        boolean locationGranted = checkLocationPermissions();
        boolean batteryOptimizationDisabled = isIgnoringBatteryOptimizations();
        boolean overlayPermissionGranted = Settings.canDrawOverlays(this);
        boolean cameraGranted = checkSelfPermission(Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        boolean notificationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED;

        return locationGranted && batteryOptimizationDisabled &&
                overlayPermissionGranted && cameraGranted && notificationGranted;
    }

    private boolean checkLocationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED ;
        }
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isIgnoringBatteryOptimizations() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    private void updatePermissionStatuses() {
        // Update UI based on current permission status
        updateLocationStatus(checkLocationPermissions());
        updateBatteryOptimizationStatus(isIgnoringBatteryOptimizations());
        updateOverlayStatus(Settings.canDrawOverlays(this));
        updateCameraStatus(checkSelfPermission(Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            updateNotificationStatus(checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED);
        }
    }

    private void updateLocationStatus(boolean isGranted) {
        if (isGranted) {
            binding.locationStatusIcon.setImageResource(R.drawable.ic_check);
            binding.btnLocationPermission.setText("Location Access Enabled");
            binding.btnLocationPermission.setEnabled(false);
            binding.btnLocationPermission.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.green_500)));
        } else {
            binding.locationStatusIcon.setImageResource(R.drawable.ic_error);
            binding.btnLocationPermission.setText("Enable Location Access");
            binding.btnLocationPermission.setEnabled(true);
            binding.btnLocationPermission.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorPrimary)));
        }
    }

    private void updateBatteryOptimizationStatus(boolean isIgnored) {
        if (isIgnored) {
            binding.batteryStatusIcon.setImageResource(R.drawable.ic_check);
            binding.btnBatteryOptimization.setText("Battery Optimization Disabled");
            binding.btnBatteryOptimization.setEnabled(false);
            binding.btnBatteryOptimization.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.green_500)));
        } else {
            binding.batteryStatusIcon.setImageResource(R.drawable.ic_error);
            binding.btnBatteryOptimization.setText("Disable Battery Optimization");
            binding.btnBatteryOptimization.setEnabled(true);
            binding.btnBatteryOptimization.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorPrimary)));
        }
    }

    private void updateOverlayStatus(boolean isGranted) {
        if (isGranted) {
            binding.overlayStatusIcon.setImageResource(R.drawable.ic_check);
            binding.btnOverlay.setText("Overlay Permission Enabled");
            binding.btnOverlay.setEnabled(false);
            binding.btnOverlay.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.green_500)));
        } else {
            binding.overlayStatusIcon.setImageResource(R.drawable.ic_error);
            binding.btnOverlay.setText("Enable Overlay Permission");
            binding.btnOverlay.setEnabled(true);
            binding.btnOverlay.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorPrimary)));
        }
    }

    private void updateCameraStatus(boolean isGranted) {
        if (isGranted) {
            binding.cameraStatusIcon.setImageResource(R.drawable.ic_check);
            binding.btnCamera.setText("Camera Access Enabled");
            binding.btnCamera.setEnabled(false);
            binding.btnCamera.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.green_500)));
        } else {
            binding.cameraStatusIcon.setImageResource(R.drawable.ic_error);
            binding.btnCamera.setText("Enable Camera Access");
            binding.btnCamera.setEnabled(true);
            binding.btnCamera.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorPrimary)));
        }
    }

    private void updateNotificationStatus(boolean isGranted) {
        if (isGranted) {
            binding.notificationStatusIcon.setImageResource(R.drawable.ic_check);
            binding.btnNotification.setText("Notifications Enabled");
            binding.btnNotification.setEnabled(false);
            binding.btnNotification.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.green_500)));
        } else {
            binding.notificationStatusIcon.setImageResource(R.drawable.ic_error);
            binding.btnNotification.setText("Enable Notifications");
            binding.btnNotification.setEnabled(true);
            binding.btnNotification.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.colorPrimary)));
        }
    }



    private void showMissingPermissionsDialog() {
        StringBuilder missingPermissions = new StringBuilder();
        if (!checkLocationPermissions())
            missingPermissions.append("• Location\n");
        if (!isIgnoringBatteryOptimizations())
            missingPermissions.append("• Battery Optimization\n");
        if (!Settings.canDrawOverlays(this))
            missingPermissions.append("• Overlay Permission\n");
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            missingPermissions.append("• Camera\n");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            missingPermissions.append("• Notifications\n");

        new MaterialAlertDialogBuilder(this)
                .setTitle("Missing Permissions")
                .setMessage("Please enable the following permissions:\n\n" + missingPermissions)
                .setPositiveButton("OK", null)
                .show();
    }

    private void proceedToNextScreen() {
        // Navigate to next screen
        startActivity(new Intent(this, DriverTypeActivity.class));
        finish();
    }

    // Handle permission results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        updatePermissionStatuses();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        updatePermissionStatuses();
    }
}