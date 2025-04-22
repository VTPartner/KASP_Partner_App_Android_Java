package com.kapstranspvtltd.kaps_partner.goods_driver_activities.documents.main_document_screens;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kapstranspvtltd.kaps_partner.goods_driver_activities.HomeActivity;
import com.kapstranspvtltd.kaps_partner.goods_driver_activities.documents.owner_documents.GoodsDriverVehicleOwnerSelfieUploadActivity;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityGoodsDriverVehicleOwnerDetailsBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class GoodsDriverVehicleOwnerDetailsActivity extends AppCompatActivity {
    private static final String TAG = "GoodsDriverOwnerDetails";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private ActivityGoodsDriverVehicleOwnerDetailsBinding binding;
    private PreferenceManager preferenceManager;
    private boolean isLoading = false;
    private FusedLocationProviderClient fusedLocationClient;
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGoodsDriverVehicleOwnerDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        checkLocationPermission();
        setupUI();
        loadDefaultValues();
    }

    private void setupUI() {
        binding.backButton.setOnClickListener(v -> finish());
        binding.submitButton.setOnClickListener(v -> saveOwnerDetails());
        binding.ownerPhotoCard.setOnClickListener(v ->
                startActivity(new Intent(this, GoodsDriverVehicleOwnerSelfieUploadActivity.class)));
    }

    private void loadDefaultValues() {
        binding.ownerNameInput.setText(preferenceManager.getStringValue("owner_name"));
        binding.ownerAddressInput.setText(preferenceManager.getStringValue("owner_address"));
        binding.ownerCityInput.setText(preferenceManager.getStringValue("owner_city_name"));
        binding.ownerPhoneInput.setText(preferenceManager.getStringValue("owner_mobile_no"));
    }

    private void checkLocationPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            getLastLocation();
        } else if (shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            showLocationPermissionDialog();
        } else {
            requestLocationPermission();
        }
    }

    private void showLocationPermissionDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Location Permission Required")
                .setMessage("This app needs location permission to get your current location for registration.")
                .setPositiveButton("Grant", (dialog, which) -> requestLocationPermission())
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                    showToast("Location permission is required for registration");
                })
                .show();
    }

    private void requestLocationPermission() {
        requestPermissions(
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE
        );
    }
    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        currentLatitude = location.getLatitude();
                        currentLongitude = location.getLongitude();
                    } else {
                        requestNewLocation();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting location", e);
                    showToast("Failed to get location");
                });
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocation() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)  // 10 seconds
                .setFastestInterval(5000);  // 5 seconds

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                android.location.Location location = locationResult.getLastLocation();
                if (location != null) {
                    currentLatitude = location.getLatitude();
                    currentLongitude = location.getLongitude();
                }
                fusedLocationClient.removeLocationUpdates(this);
            }
        };

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                showToast("Location permission denied");
            }
        }
    }

    private void saveOwnerDetails() {
        String ownerName = binding.ownerNameInput.getText().toString().trim();
        String ownerAddress = binding.ownerAddressInput.getText().toString().trim();
        String ownerCity = binding.ownerCityInput.getText().toString().trim();
        String ownerPhone = binding.ownerPhoneInput.getText().toString().trim();
        String ownerPhotoUrl = preferenceManager.getStringValue("owner_selfie_photo_url");

        if (ownerName.isEmpty()) {
            showToast("Please provide owner full name");
            return;
        }
        if (ownerAddress.isEmpty()) {
            showToast("Please provide owner full address");
            return;
        }
        if (ownerCity.isEmpty()) {
            showToast("Please provide owner City Name");
            return;
        }
        if (ownerPhone.isEmpty()) {
            showToast("Please provide owner mobile number");
            return;
        }
        if (ownerPhone.length() != 10) {
            showToast("Please provide valid 10 digits owner mobile number without country code");
            return;
        }
        if (ownerPhotoUrl.isEmpty()) {
            showToast("Please upload Owner Photo");
            return;
        }

        // Save owner details
        preferenceManager.saveStringValue("owner_name", ownerName);
        preferenceManager.saveStringValue("owner_address", ownerAddress);
        preferenceManager.saveStringValue("owner_city_name", ownerCity);
        preferenceManager.saveStringValue("owner_mobile_no", ownerPhone);

        registerDriver();
    }
    private void registerDriver() {
        isLoading = true;
        updateLoadingState();

        JSONObject requestData = new JSONObject();
        String cabDriverId = preferenceManager.getStringValue("goods_driver_id");
        String cabDriverName = preferenceManager.getStringValue("driver_name");
        String cabDriverMobileNo = preferenceManager.getStringValue("goods_driver_mobile_no");
        String cabDriverFullAddress = preferenceManager.getStringValue("full_address");
        String cabDriverGender = preferenceManager.getStringValue("driver_gender");
        String cabDriverCityId = preferenceManager.getStringValue("driver_city_id");
        String cabDriverSelfiePhotoUrl = preferenceManager.getStringValue("selfie_photo_url");
        String cabDriverLicenseFrontPhotoUrl = preferenceManager.getStringValue("license_front_photo_url");
        String cabDriverLicenseBackPhotoUrl = preferenceManager.getStringValue("license_back_photo_url");
        String cabDriverLicenseNo = preferenceManager.getStringValue("license_no");
        String cabDriverAadharFrontPhotoUrl = preferenceManager.getStringValue("aadhar_front_photo_url");
        String cabDriverAadharBackPhotoUrl = preferenceManager.getStringValue("aadhar_back_photo_url");
        String cabDriverAadharNo = preferenceManager.getStringValue("aadhar_no");
        String cabDriverPanNo = preferenceManager.getStringValue("pan_no");
        String cabDriverPanFrontPhotoUrl = preferenceManager.getStringValue("pan_front_photo_url");
        String cabDriverPanBackPhotoUrl = preferenceManager.getStringValue("pan_back_photo_url");
        String cabDriverVehicleNo = preferenceManager.getStringValue("driver_vehicle_no");
        String cabDriverVehicleFuelType = preferenceManager.getStringValue("driver_vehicle_fuel_type");
        String cabDriverVehicleId = preferenceManager.getStringValue("driver_vehicle_id");
        String cabDriverVehicleFrontPhotoUrl = preferenceManager.getStringValue("vehicle_front_photo_url");
        String cabDriverVehicleBackPhotoUrl = preferenceManager.getStringValue("vehicle_back_photo_url");
        String cabDriverVehiclePlateFrontPhotoUrl = preferenceManager.getStringValue("vehicle_plate_front_photo_url");
        String cabDriverVehiclePlateBackPhotoUrl = preferenceManager.getStringValue("vehicle_plate_back_photo_url");
        String cabDriverRcPhotoUrl = preferenceManager.getStringValue("rc_photo_url");
        String cabDriverRcNo = preferenceManager.getStringValue("rc_no");
        String cabDriverInsurancePhotoUrl = preferenceManager.getStringValue("insurance_photo_url");
        String cabDriverInsuranceNo = preferenceManager.getStringValue("insurance_no");
        String cabDriverNocPhotoUrl = preferenceManager.getStringValue("noc_photo_url");
        String cabDriverNocNo = preferenceManager.getStringValue("noc_no");
        String cabDriverPucPhotoUrl = preferenceManager.getStringValue("puc_photo_url");
        String cabDriverPucNo = preferenceManager.getStringValue("puc_no");
        String cabDriverOwnerName = preferenceManager.getStringValue("owner_name");
        String cabDriverOwnerAddress = preferenceManager.getStringValue("owner_address");
        String cabDriverOwnerCityName = preferenceManager.getStringValue("owner_city_name");
        String cabDriverOwnerMobileNo = preferenceManager.getStringValue("owner_mobile_no");
        String cabDriverOwnerSelfiePhotoUrl = preferenceManager.getStringValue("owner_selfie_photo_url");


        try {
            requestData.put("goods_driver_id", cabDriverId);  // You'll add the value
            requestData.put("driver_first_name", cabDriverName);
            requestData.put("profile_pic", cabDriverSelfiePhotoUrl);
            requestData.put("mobile_no", cabDriverMobileNo);
            requestData.put("r_lat", String.valueOf(currentLatitude));
            requestData.put("r_lng", String.valueOf(currentLongitude));
            requestData.put("current_lat",String.valueOf(currentLatitude));
            requestData.put("current_lng", String.valueOf(currentLongitude));
            requestData.put("recent_online_pic", cabDriverSelfiePhotoUrl);
            requestData.put("vehicle_id", cabDriverVehicleId);
            requestData.put("city_id", cabDriverCityId);
            requestData.put("aadhar_no", cabDriverAadharNo);
            requestData.put("pan_card_no", cabDriverPanNo);
            requestData.put("full_address", cabDriverFullAddress);
            requestData.put("gender", cabDriverGender);
            requestData.put("aadhar_card_front", cabDriverAadharFrontPhotoUrl);
            requestData.put("aadhar_card_back", cabDriverAadharBackPhotoUrl);
            requestData.put("pan_card_front", cabDriverPanFrontPhotoUrl);
            requestData.put("pan_card_back", cabDriverPanBackPhotoUrl);
            requestData.put("license_front", cabDriverLicenseFrontPhotoUrl);
            requestData.put("license_back", cabDriverLicenseBackPhotoUrl);
            requestData.put("insurance_image", cabDriverInsurancePhotoUrl);
            requestData.put("noc_image", cabDriverNocPhotoUrl);
            requestData.put("pollution_certificate_image", cabDriverPucPhotoUrl);
            requestData.put("rc_image", cabDriverRcPhotoUrl);
            requestData.put("vehicle_image", cabDriverVehicleFrontPhotoUrl);
            requestData.put("vehicle_plate_image", cabDriverVehiclePlateFrontPhotoUrl);
            requestData.put("driving_license_no", cabDriverLicenseNo);
            requestData.put("vehicle_plate_no", cabDriverVehicleNo);
            requestData.put("rc_no", cabDriverRcNo);
            requestData.put("insurance_no", cabDriverInsuranceNo);
            requestData.put("noc_no", cabDriverNocNo);
            requestData.put("vehicle_fuel_type", cabDriverVehicleFuelType);
            requestData.put("owner_name", cabDriverOwnerName);
            requestData.put("owner_mobile_no", cabDriverOwnerMobileNo);
            requestData.put("owner_photo_url", cabDriverOwnerSelfiePhotoUrl);
            requestData.put("owner_address", cabDriverOwnerAddress);
            requestData.put("owner_city_name", cabDriverOwnerCityName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                APIClient.baseUrl + "goods_driver_registration",
                requestData,
                response -> {
                    isLoading = false;
                    updateLoadingState();

                    if (response.has("message")) {
                        // Driver Basic Info
                        preferenceManager.saveStringValue("goods_driver_id",
                                preferenceManager.getStringValue("goods_driver_id"));
                        preferenceManager.saveStringValue("goods_driver_name",
                                preferenceManager.getStringValue("driver_name"));
                        preferenceManager.saveStringValue("profile_pic",
                                preferenceManager.getStringValue("selfie_photo_url"));
                        preferenceManager.saveStringValue("mobile_no",
                                preferenceManager.getStringValue("goods_driver_mobno"));

                        // Location Info
                        preferenceManager.saveStringValue("r_lat", String.valueOf(currentLatitude));
                        preferenceManager.saveStringValue("r_lng", String.valueOf(currentLongitude));
                        preferenceManager.saveStringValue("current_lat", String.valueOf(currentLatitude));
                        preferenceManager.saveStringValue("current_lng", String.valueOf(currentLongitude));

                        // Recent Online Info
                        preferenceManager.saveStringValue("recent_online_pic",
                                preferenceManager.getStringValue("selfie_photo_url"));

                        // Vehicle and City Info
                        preferenceManager.saveStringValue("vehicle_id",
                                preferenceManager.getStringValue("driver_vehicle_id"));
                        preferenceManager.saveStringValue("city_id",
                                preferenceManager.getStringValue("driver_city_id"));

                        // Personal Documents
                        preferenceManager.saveStringValue("aadhar_no",
                                preferenceManager.getStringValue("aadhar_no"));
                        preferenceManager.saveStringValue("pan_card_no",
                                preferenceManager.getStringValue("pan_no"));
                        preferenceManager.saveStringValue("full_address",
                                preferenceManager.getStringValue("driver_address"));
                        preferenceManager.saveStringValue("gender",
                                preferenceManager.getStringValue("driver_gender"));

                        // Document Images
                        saveDocumentImages();

                        // Vehicle Documents
                        saveVehicleDocuments();

                        // Document Numbers
                        saveDocumentNumbers();

                        // Owner Details
                        saveOwnerInfo();

                        startActivity(new Intent(this, HomeActivity.class));
                        finishAffinity();
                    }
                },
                error -> {
                    Log.e(TAG, "Registration failed", error);
                    isLoading = false;
                    updateLoadingState();
                    showToast("Registration failed");
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void saveDocumentImages() {
        preferenceManager.saveStringValue("aadhar_card_front",
                preferenceManager.getStringValue("aadhar_front_photo_url"));
        preferenceManager.saveStringValue("aadhar_card_back",
                preferenceManager.getStringValue("aadhar_back_photo_url"));
        preferenceManager.saveStringValue("pan_card_front",
                preferenceManager.getStringValue("pan_front_photo_url"));
        preferenceManager.saveStringValue("pan_card_back",
                preferenceManager.getStringValue("pan_back_photo_url"));
        preferenceManager.saveStringValue("license_front",
                preferenceManager.getStringValue("license_front_photo_url"));
        preferenceManager.saveStringValue("license_back",
                preferenceManager.getStringValue("license_back_photo_url"));
    }

    private void saveVehicleDocuments() {
        preferenceManager.saveStringValue("insurance_image",
                preferenceManager.getStringValue("insurance_photo_url"));
        preferenceManager.saveStringValue("noc_image",
                preferenceManager.getStringValue("noc_photo_url"));
        preferenceManager.saveStringValue("pollution_certificate_image",
                preferenceManager.getStringValue("puc_photo_url"));
        preferenceManager.saveStringValue("rc_image",
                preferenceManager.getStringValue("rc_photo_url"));
        preferenceManager.saveStringValue("vehicle_image",
                preferenceManager.getStringValue("vehicle_front_photo_url"));
        preferenceManager.saveStringValue("vehicle_plate_image",
                preferenceManager.getStringValue("vehicle_plate_front_photo_url"));
    }

    private void saveDocumentNumbers() {
        preferenceManager.saveStringValue("driving_license_no",
                preferenceManager.getStringValue("license_no"));
        preferenceManager.saveStringValue("vehicle_plate_no",
                preferenceManager.getStringValue("driver_vehicle_no"));
        preferenceManager.saveStringValue("rc_no",
                preferenceManager.getStringValue("rc_no"));
        preferenceManager.saveStringValue("insurance_no",
                preferenceManager.getStringValue("insurance_no"));
        preferenceManager.saveStringValue("noc_no",
                preferenceManager.getStringValue("noc_no"));
        preferenceManager.saveStringValue("vehicle_fuel_type",
                preferenceManager.getStringValue("driver_vehicle_fuel_type"));
    }

    private void saveOwnerInfo() {
        preferenceManager.saveStringValue("owner_name",
                preferenceManager.getStringValue("owner_name"));
        preferenceManager.saveStringValue("owner_mobile_no",
                preferenceManager.getStringValue("owner_mobile_no"));
        preferenceManager.saveStringValue("owner_photo_url",
                preferenceManager.getStringValue("owner_selfie_photo_url"));
        preferenceManager.saveStringValue("owner_address",
                preferenceManager.getStringValue("owner_address"));
        preferenceManager.saveStringValue("owner_city_name",
                preferenceManager.getStringValue("owner_city_name"));
    }

    private void updateLoadingState() {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.submitButton.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}