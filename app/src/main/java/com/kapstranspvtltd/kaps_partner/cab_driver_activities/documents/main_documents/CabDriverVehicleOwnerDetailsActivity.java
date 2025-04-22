package com.kapstranspvtltd.kaps_partner.cab_driver_activities.documents.main_documents;

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
import com.kapstranspvtltd.kaps_partner.cab_driver_activities.CabDriverHomeActivity;
import com.kapstranspvtltd.kaps_partner.cab_driver_activities.documents.owner_documents.CabDriverVehicleOwnerSelfieUploadActivity;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityCabDriverOwnerDetailsBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CabDriverVehicleOwnerDetailsActivity extends AppCompatActivity {

    private static final String TAG = "GoodsDriverOwnerDetails";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private ActivityCabDriverOwnerDetailsBinding binding;
    private PreferenceManager preferenceManager;
    private boolean isLoading = false;
    private FusedLocationProviderClient fusedLocationClient;
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCabDriverOwnerDetailsBinding.inflate(getLayoutInflater());
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
                startActivity(new Intent(this, CabDriverVehicleOwnerSelfieUploadActivity.class)));
    }

    private void loadDefaultValues() {
        binding.ownerNameInput.setText(preferenceManager.getStringValue("cab_driver_owner_name"));
        binding.ownerAddressInput.setText(preferenceManager.getStringValue("cab_driver_owner_address"));
        binding.ownerCityInput.setText(preferenceManager.getStringValue("cab_driver_owner_city_name"));
        binding.ownerPhoneInput.setText(preferenceManager.getStringValue("cab_driver_owner_mobile_no"));
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
        String ownerPhotoUrl = preferenceManager.getStringValue("cab_driver_owner_selfie_photo_url");

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
        preferenceManager.saveStringValue("cab_driver_owner_name", ownerName);
        preferenceManager.saveStringValue("cab_driver_owner_address", ownerAddress);
        preferenceManager.saveStringValue("cab_driver_owner_city_name", ownerCity);
        preferenceManager.saveStringValue("cab_driver_owner_mobile_no", ownerPhone);

        registerDriver();
    }
    private void registerDriver() {
        isLoading = true;
        updateLoadingState();

        JSONObject requestData = new JSONObject();
        String cabDriverId = preferenceManager.getStringValue("cab_driver_id");
        String cabDriverName = preferenceManager.getStringValue("cab_driver_name");
        String cabDriverMobileNo = preferenceManager.getStringValue("cab_driver_mobile_no");
        String cabDriverFullAddress = preferenceManager.getStringValue("cab_driver_full_address");
        String cabDriverGender = preferenceManager.getStringValue("cab_driver_gender");
        String cabDriverCityId = preferenceManager.getStringValue("cab_driver_city_id");
        String cabDriverSelfiePhotoUrl = preferenceManager.getStringValue("cab_driver_selfie_photo_url");
        String cabDriverLicenseFrontPhotoUrl = preferenceManager.getStringValue("cab_driver_license_front_photo_url");
        String cabDriverLicenseBackPhotoUrl = preferenceManager.getStringValue("cab_driver_license_back_photo_url");
        String cabDriverLicenseNo = preferenceManager.getStringValue("cab_driver_license_no");
        String cabDriverAadharFrontPhotoUrl = preferenceManager.getStringValue("cab_driver_aadhar_front_photo_url");
        String cabDriverAadharBackPhotoUrl = preferenceManager.getStringValue("cab_driver_aadhar_back_photo_url");
        String cabDriverAadharNo = preferenceManager.getStringValue("cab_driver_aadhar_no");
        String cabDriverPanNo = preferenceManager.getStringValue("cab_driver_pan_no");
        String cabDriverPanFrontPhotoUrl = preferenceManager.getStringValue("cab_driver_pan_front_photo_url");
        String cabDriverPanBackPhotoUrl = preferenceManager.getStringValue("cab_driver_pan_back_photo_url");
        String cabDriverVehicleNo = preferenceManager.getStringValue("cab_driver_vehicle_no");
        String cabDriverVehicleFuelType = preferenceManager.getStringValue("cab_driver_vehicle_fuel_type");
        String cabDriverVehicleId = preferenceManager.getStringValue("cab_driver_vehicle_id");
        String cabDriverVehicleFrontPhotoUrl = preferenceManager.getStringValue("cab_driver_vehicle_front_photo_url");
        String cabDriverVehicleBackPhotoUrl = preferenceManager.getStringValue("cab_driver_vehicle_back_photo_url");
        String cabDriverVehiclePlateFrontPhotoUrl = preferenceManager.getStringValue("cab_driver_vehicle_plate_front_photo_url");
        String cabDriverVehiclePlateBackPhotoUrl = preferenceManager.getStringValue("cab_driver_vehicle_plate_back_photo_url");
        String cabDriverRcPhotoUrl = preferenceManager.getStringValue("cab_driver_rc_photo_url");
        String cabDriverRcNo = preferenceManager.getStringValue("cab_driver_rc_no");
        String cabDriverInsurancePhotoUrl = preferenceManager.getStringValue("cab_driver_insurance_photo_url");
        String cabDriverInsuranceNo = preferenceManager.getStringValue("cab_driver_insurance_no");
        String cabDriverNocPhotoUrl = preferenceManager.getStringValue("cab_driver_noc_photo_url");
        String cabDriverNocNo = preferenceManager.getStringValue("cab_driver_noc_no");
        String cabDriverPucPhotoUrl = preferenceManager.getStringValue("cab_driver_puc_photo_url");
        String cabDriverPucNo = preferenceManager.getStringValue("cab_driver_puc_no");
        String cabDriverOwnerName = preferenceManager.getStringValue("cab_driver_owner_name");
        String cabDriverOwnerAddress = preferenceManager.getStringValue("cab_driver_owner_address");
        String cabDriverOwnerCityName = preferenceManager.getStringValue("cab_driver_owner_city_name");
        String cabDriverOwnerMobileNo = preferenceManager.getStringValue("cab_driver_owner_mobile_no");
        String cabDriverOwnerSelfiePhotoUrl = preferenceManager.getStringValue("cab_driver_owner_selfie_photo_url");


        try {
            requestData.put("cab_driver_id", cabDriverId);  // You'll add the value
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

// Log the data
        Log.d("RequestData", "data::" + requestData.toString());
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                APIClient.baseUrl + "cab_driver_registration",
                requestData,
                response -> {
                    isLoading = false;
                    updateLoadingState();

                    if (response.has("message")) {
                        // Driver Basic Info
                        preferenceManager.saveStringValue("cab_driver_id",
                                preferenceManager.getStringValue("cab_driver_id"));
                        preferenceManager.saveStringValue("cab_agent_driver_name",
                                preferenceManager.getStringValue("cab_agent_driver_name"));
                        preferenceManager.saveStringValue("cab_agent_selfie_photo_url",
                                preferenceManager.getStringValue("cab_agent_selfie_photo_url"));
                        preferenceManager.saveStringValue("cab_driver_mobile_no",
                                preferenceManager.getStringValue("cab_driver_mobile_no"));

                        // Location Info
                        preferenceManager.saveStringValue("cab_driver_r_lat", String.valueOf(currentLatitude));
                        preferenceManager.saveStringValue("cab_driver_r_lng", String.valueOf(currentLongitude));
                        preferenceManager.saveStringValue("cab_driver_current_lat", String.valueOf(currentLatitude));
                        preferenceManager.saveStringValue("cab_driver_current_lng", String.valueOf(currentLongitude));

                        // Recent Online Info
                        preferenceManager.saveStringValue("cab_agent_recent_selfie_photo_url",
                                preferenceManager.getStringValue("cab_agent_selfie_photo_url"));

                        // Vehicle and City Info
                        preferenceManager.saveStringValue("cab_agent_driver_vehicle_id",
                                preferenceManager.getStringValue("cab_agent_driver_vehicle_id"));
                        preferenceManager.saveStringValue("cab_agent_driver_city_id",
                                preferenceManager.getStringValue("cab_agent_driver_city_id"));

                        // Personal Documents
                        preferenceManager.saveStringValue("cab_agent_aadhar_no",
                                preferenceManager.getStringValue("cab_agent_aadhar_no"));
                        preferenceManager.saveStringValue("cab_agent_pan_no",
                                preferenceManager.getStringValue("cab_agent_pan_no"));
                        preferenceManager.saveStringValue("cab_agent_driver_address",
                                preferenceManager.getStringValue("cab_agent_driver_address"));
                        preferenceManager.saveStringValue("cab_agent_driver_gender",
                                preferenceManager.getStringValue("cab_agent_driver_gender"));

                        // Document Images
                        saveDocumentImages();

                        // Vehicle Documents
                        saveVehicleDocuments();

                        // Document Numbers
                        saveDocumentNumbers();

                        // Owner Details
                        saveOwnerInfo();

                        startActivity(new Intent(this, CabDriverHomeActivity.class));
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
        preferenceManager.saveStringValue("cab_driver_aadhar_card_front",
                preferenceManager.getStringValue("cab_driver_aadhar_front_photo_url"));
        preferenceManager.saveStringValue("cab_driver_aadhar_card_back",
                preferenceManager.getStringValue("cab_driver_aadhar_back_photo_url"));
        preferenceManager.saveStringValue("cab_driver_pan_card_front",
                preferenceManager.getStringValue("cab_driver_pan_front_photo_url"));
        preferenceManager.saveStringValue("cab_driver_pan_card_back",
                preferenceManager.getStringValue("cab_driver_pan_back_photo_url"));
        preferenceManager.saveStringValue("cab_driver_license_front",
                preferenceManager.getStringValue("cab_driver_license_front_photo_url"));
        preferenceManager.saveStringValue("cab_driver_license_back",
                preferenceManager.getStringValue("cab_driver_license_back_photo_url"));
    }

    private void saveVehicleDocuments() {
        preferenceManager.saveStringValue("cab_driver_insurance_image",
                preferenceManager.getStringValue("cab_driver_insurance_photo_url"));
        preferenceManager.saveStringValue("cab_driver_noc_image",
                preferenceManager.getStringValue("noc_photo_url"));
        preferenceManager.saveStringValue("cab_driver_pollution_certificate_image",
                preferenceManager.getStringValue("cab_driver_puc_photo_url"));
        preferenceManager.saveStringValue("cab_driver_rc_image",
                preferenceManager.getStringValue("cab_driver_rc_photo_url"));
        preferenceManager.saveStringValue("cab_driver_vehicle_image",
                preferenceManager.getStringValue("cab_driver_vehicle_front_photo_url"));
        preferenceManager.saveStringValue("cab_driver_vehicle_plate_image",
                preferenceManager.getStringValue("cab_driver_vehicle_plate_front_photo_url"));
    }

    private void saveDocumentNumbers() {
        preferenceManager.saveStringValue("cab_driver_driving_license_no",
                preferenceManager.getStringValue("cab_driver_license_no"));
        preferenceManager.saveStringValue("cab_driver_vehicle_plate_no",
                preferenceManager.getStringValue("cab_driver_vehicle_no"));
        preferenceManager.saveStringValue("cab_driver_rc_no",
                preferenceManager.getStringValue("cab_driver_rc_no"));
        preferenceManager.saveStringValue("cab_driver_insurance_no",
                preferenceManager.getStringValue("cab_driver_insurance_no"));
        preferenceManager.saveStringValue("cab_driver_noc_no",
                preferenceManager.getStringValue("cab_driver_noc_no"));
        preferenceManager.saveStringValue("cab_driver_vehicle_fuel_type",
                preferenceManager.getStringValue("cab_driver_vehicle_fuel_type"));
    }

    private void saveOwnerInfo() {
        preferenceManager.saveStringValue("cab_driver_owner_name",
                preferenceManager.getStringValue("cab_driver_owner_name"));
        preferenceManager.saveStringValue("cab_driver_owner_mobile_no",
                preferenceManager.getStringValue("cab_driver_owner_mobile_no"));
        preferenceManager.saveStringValue("cab_driver_owner_photo_url",
                preferenceManager.getStringValue("cab_driver_owner_selfie_photo_url"));
        preferenceManager.saveStringValue("cab_driver_owner_address",
                preferenceManager.getStringValue("cab_driver_owner_address"));
        preferenceManager.saveStringValue("cab_driver_owner_city_name",
                preferenceManager.getStringValue("cab_driver_owner_city_name"));
    }

    private void updateLoadingState() {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.submitButton.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}