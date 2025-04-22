package com.kapstranspvtltd.kaps_partner.jcb_crane_agent_activities.jcb_crane_main_documents;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.cab_driver_activities.CabDriverHomeActivity;
import com.kapstranspvtltd.kaps_partner.cab_driver_activities.documents.owner_documents.CabDriverVehicleOwnerSelfieUploadActivity;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityCabDriverOwnerDetailsBinding;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityJcbCraneVehicleOwnerDetailsBinding;
import com.kapstranspvtltd.kaps_partner.jcb_crane_agent_activities.JcbCraneHomeActivity;
import com.kapstranspvtltd.kaps_partner.jcb_crane_agent_activities.documents.owner_documents.JcbCraneAgentVehicleOwnerSelfieUploadActivity;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class JcbCraneVehicleOwnerDetailsActivity extends AppCompatActivity {

    private static final String TAG = "GoodsDriverOwnerDetails";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private ActivityJcbCraneVehicleOwnerDetailsBinding binding;
    private PreferenceManager preferenceManager;
    private boolean isLoading = false;
    private FusedLocationProviderClient fusedLocationClient;
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityJcbCraneVehicleOwnerDetailsBinding.inflate(getLayoutInflater());
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
                startActivity(new Intent(this, JcbCraneAgentVehicleOwnerSelfieUploadActivity.class)));
    }

    private void loadDefaultValues() {
        binding.ownerNameInput.setText(preferenceManager.getStringValue("jcb_crane_agent_owner_name"));
        binding.ownerAddressInput.setText(preferenceManager.getStringValue("jcb_crane_agent_owner_address"));
        binding.ownerCityInput.setText(preferenceManager.getStringValue("jcb_crane_agent_owner_city_name"));
        binding.ownerPhoneInput.setText(preferenceManager.getStringValue("jcb_crane_agent_owner_mobile_no"));
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
        String ownerPhotoUrl = preferenceManager.getStringValue("jcb_crane_agent_owner_selfie_photo_url");

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
        preferenceManager.saveStringValue("jcb_crane_agent_owner_name", ownerName);
        preferenceManager.saveStringValue("jcb_crane_agent_owner_address", ownerAddress);
        preferenceManager.saveStringValue("jcb_crane_agent_owner_city_name", ownerCity);
        preferenceManager.saveStringValue("jcb_crane_agent_owner_mobile_no", ownerPhone);

        registerDriver();
    }
    private void registerDriver() {
        isLoading = true;
        updateLoadingState();

        JSONObject requestData = new JSONObject();
        String jcbCraneId = preferenceManager.getStringValue("jcb_crane_agent_id");
        String jcbCraneName = preferenceManager.getStringValue("jcb_crane_agent_name");
        String jcbCraneMobileNo = preferenceManager.getStringValue("jcb_crane_agent_mobile_no");
        String jcbCraneFullAddress = preferenceManager.getStringValue("jcb_crane_agent_full_address");
        String jcbCraneGender = preferenceManager.getStringValue("jcb_crane_agent_gender");
        String jcbCraneCityId = preferenceManager.getStringValue("jcb_crane_agent_city_id");
        String jcbCraneSelfiePhotoUrl = preferenceManager.getStringValue("jcb_crane_agent_selfie_photo_url");
        String jcbCraneLicenseFrontPhotoUrl = preferenceManager.getStringValue("jcb_crane_agent_license_front_photo_url");
        String jcbCraneLicenseBackPhotoUrl = preferenceManager.getStringValue("jcb_crane_agent_license_back_photo_url");
        String jcbCraneLicenseNo = preferenceManager.getStringValue("jcb_crane_agent_license_no");
        String jcbCraneAadharFrontPhotoUrl = preferenceManager.getStringValue("jcb_crane_agent_aadhar_front_photo_url");
        String jcbCraneAadharBackPhotoUrl = preferenceManager.getStringValue("jcb_crane_agent_aadhar_back_photo_url");
        String jcbCraneAadharNo = preferenceManager.getStringValue("jcb_crane_agent_aadhar_no");
        String jcbCranePanNo = preferenceManager.getStringValue("jcb_crane_agent_pan_no");
        String jcbCranePanFrontPhotoUrl = preferenceManager.getStringValue("jcb_crane_agent_pan_front_photo_url");
        String jcbCranePanBackPhotoUrl = preferenceManager.getStringValue("jcb_crane_agent_pan_back_photo_url");
        String jcbCraneVehicleNo = preferenceManager.getStringValue("jcb_crane_agent_vehicle_no");
        String jcbCraneVehicleFuelType = preferenceManager.getStringValue("jcb_crane_agent_vehicle_fuel_type");
//        String jcbCraneVehicleId = preferenceManager.getStringValue("jcb_crane_agent_vehicle_id");
        String jcbCraneVehicleFrontPhotoUrl = preferenceManager.getStringValue("jcb_crane_agent_vehicle_front_photo_url");
        String jcbCraneVehicleBackPhotoUrl = preferenceManager.getStringValue("jcb_crane_agent_vehicle_back_photo_url");
        String jcbCraneVehiclePlateFrontPhotoUrl = preferenceManager.getStringValue("jcb_crane_agent_vehicle_plate_front_photo_url");
        String jcbCraneVehiclePlateBackPhotoUrl = preferenceManager.getStringValue("jcb_crane_agent_vehicle_plate_back_photo_url");
        String jcbCraneRcPhotoUrl = preferenceManager.getStringValue("jcb_crane_agent_rc_photo_url");
        String jcbCraneRcNo = preferenceManager.getStringValue("jcb_crane_agent_rc_no");
        String jcbCraneInsurancePhotoUrl = preferenceManager.getStringValue("jcb_crane_agent_insurance_photo_url");
        String jcbCraneInsuranceNo = preferenceManager.getStringValue("jcb_crane_agent_insurance_no");
        String jcbCraneNocPhotoUrl = preferenceManager.getStringValue("jcb_crane_agent_noc_photo_url");
        String jcbCraneNocNo = preferenceManager.getStringValue("jcb_crane_agent_noc_no");
        String jcbCranePucPhotoUrl = preferenceManager.getStringValue("jcb_crane_agent_puc_photo_url");
        String jcbCranePucNo = preferenceManager.getStringValue("jcb_crane_agent_puc_no");
        String jcbCraneOwnerName = preferenceManager.getStringValue("jcb_crane_agent_owner_name");
        String jcbCraneOwnerAddress = preferenceManager.getStringValue("jcb_crane_agent_owner_address");
        String jcbCraneOwnerCityName = preferenceManager.getStringValue("jcb_crane_agent_owner_city_name");
        String jcbCraneOwnerMobileNo = preferenceManager.getStringValue("jcb_crane_agent_owner_mobile_no");
        String jcbCraneOwnerSelfiePhotoUrl = preferenceManager.getStringValue("jcb_crane_agent_owner_selfie_photo_url");
        String jcbCraneSubCategoryId = preferenceManager.getStringValue("jcb_crane_agent_sub_category_id");
        String jcbCraneServiceId = preferenceManager.getStringValue("jcb_crane_agent_other_service_id");


        try {
            requestData.put("jcb_crane_driver_id", jcbCraneId);  // You'll add the value
            requestData.put("driver_first_name", jcbCraneName);
            requestData.put("profile_pic", jcbCraneSelfiePhotoUrl);
            requestData.put("mobile_no", jcbCraneMobileNo);
            requestData.put("r_lat", String.valueOf(currentLatitude));
            requestData.put("r_lng", String.valueOf(currentLongitude));
            requestData.put("current_lat",String.valueOf(currentLatitude));
            requestData.put("current_lng", String.valueOf(currentLongitude));
            requestData.put("recent_online_pic", jcbCraneSelfiePhotoUrl);
//            requestData.put("vehicle_id", jcbCraneVehicleId);
            requestData.put("city_id", jcbCraneCityId);
            requestData.put("aadhar_no", jcbCraneAadharNo);
            requestData.put("pan_card_no", jcbCranePanNo);
            requestData.put("full_address", jcbCraneFullAddress);
            requestData.put("gender", jcbCraneGender);
            requestData.put("aadhar_card_front", jcbCraneAadharFrontPhotoUrl);
            requestData.put("aadhar_card_back", jcbCraneAadharBackPhotoUrl);
            requestData.put("pan_card_front", jcbCranePanFrontPhotoUrl);
            requestData.put("pan_card_back", jcbCranePanBackPhotoUrl);
            requestData.put("license_front", jcbCraneLicenseFrontPhotoUrl);
            requestData.put("license_back", jcbCraneLicenseBackPhotoUrl);
            requestData.put("insurance_image", jcbCraneInsurancePhotoUrl);
            requestData.put("noc_image", jcbCraneNocPhotoUrl);
            requestData.put("pollution_certificate_image", jcbCranePucPhotoUrl);
            requestData.put("rc_image", jcbCraneRcPhotoUrl);
            requestData.put("vehicle_image", jcbCraneVehicleFrontPhotoUrl);
            requestData.put("vehicle_plate_image", jcbCraneVehiclePlateFrontPhotoUrl);
            requestData.put("driving_license_no", jcbCraneLicenseNo);
            requestData.put("vehicle_plate_no", jcbCraneVehicleNo);
            requestData.put("rc_no", jcbCraneRcNo);
            requestData.put("insurance_no", jcbCraneInsuranceNo);
            requestData.put("noc_no", jcbCraneNocNo);
            requestData.put("vehicle_fuel_type", jcbCraneVehicleFuelType);
            requestData.put("owner_name", jcbCraneOwnerName);
            requestData.put("owner_mobile_no", jcbCraneOwnerMobileNo);
            requestData.put("owner_photo_url", jcbCraneOwnerSelfiePhotoUrl);
            requestData.put("owner_address", jcbCraneOwnerAddress);
            requestData.put("owner_city_name", jcbCraneOwnerCityName);
            requestData.put("sub_cat_id", jcbCraneSubCategoryId);
            requestData.put("service_id", jcbCraneServiceId);

        } catch (JSONException e) {
            e.printStackTrace();
        }

// Log the data
        Log.d("RequestData", "data::" + requestData.toString());
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                APIClient.baseUrl + "jcb_crane_driver_registration",
                requestData,
                response -> {
                    isLoading = false;
                    updateLoadingState();

                    if (response.has("message")) {
                        // Driver Basic Info
                        preferenceManager.saveStringValue("jcb_crane_agent_id",
                                preferenceManager.getStringValue("jcb_crane_agent_id"));
                        preferenceManager.saveStringValue("jcb_crane_agent_name",
                                preferenceManager.getStringValue("jcb_crane_agent_name"));
                        preferenceManager.saveStringValue("jcb_crane_agent_selfie_photo_url",
                                preferenceManager.getStringValue("jcb_crane_agent_selfie_photo_url"));
                        preferenceManager.saveStringValue("jcb_crane_agent_mobile_no",
                                preferenceManager.getStringValue("jcb_crane_agent_mobile_no"));

                        // Location Info
                        preferenceManager.saveStringValue("jcb_crane_agent_r_lat", String.valueOf(currentLatitude));
                        preferenceManager.saveStringValue("jcb_crane_agent_r_lng", String.valueOf(currentLongitude));
                        preferenceManager.saveStringValue("jcb_crane_agent_current_lat", String.valueOf(currentLatitude));
                        preferenceManager.saveStringValue("jcb_crane_agent_current_lng", String.valueOf(currentLongitude));

                        // Recent Online Info
                        preferenceManager.saveStringValue("jcb_crane_agent_recent_selfie_photo_url",
                                preferenceManager.getStringValue("jcb_crane_agent_selfie_photo_url"));

                        // Vehicle and City Info
                        preferenceManager.saveStringValue("jcb_crane_agent_vehicle_id",
                                preferenceManager.getStringValue("jcb_crane_agent_vehicle_id"));
                        preferenceManager.saveStringValue("jcb_crane_agent_city_id",
                                preferenceManager.getStringValue("jcb_crane_agent_city_id"));

                        // Personal Documents
                        preferenceManager.saveStringValue("cab_agent_aadhar_no",
                                preferenceManager.getStringValue("cab_agent_aadhar_no"));
                        preferenceManager.saveStringValue("cab_agent_pan_no",
                                preferenceManager.getStringValue("cab_agent_pan_no"));
                        preferenceManager.saveStringValue("jcb_crane_agent_address",
                                preferenceManager.getStringValue("jcb_crane_agent_address"));
                        preferenceManager.saveStringValue("jcb_crane_agent_gender",
                                preferenceManager.getStringValue("jcb_crane_agent_gender"));

                        // Document Images
                        saveDocumentImages();

                        // Vehicle Documents
                        saveVehicleDocuments();

                        // Document Numbers
                        saveDocumentNumbers();

                        // Owner Details
                        saveOwnerInfo();

                        startActivity(new Intent(this, JcbCraneHomeActivity.class));
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
        preferenceManager.saveStringValue("jcb_crane_agent_aadhar_card_front",
                preferenceManager.getStringValue("jcb_crane_agent_aadhar_front_photo_url"));
        preferenceManager.saveStringValue("jcb_crane_agent_aadhar_card_back",
                preferenceManager.getStringValue("jcb_crane_agent_aadhar_back_photo_url"));
        preferenceManager.saveStringValue("jcb_crane_agent_pan_card_front",
                preferenceManager.getStringValue("jcb_crane_agent_pan_front_photo_url"));
        preferenceManager.saveStringValue("jcb_crane_agent_pan_card_back",
                preferenceManager.getStringValue("jcb_crane_agent_pan_back_photo_url"));
        preferenceManager.saveStringValue("jcb_crane_agent_license_front",
                preferenceManager.getStringValue("jcb_crane_agent_license_front_photo_url"));
        preferenceManager.saveStringValue("jcb_crane_agent_license_back",
                preferenceManager.getStringValue("jcb_crane_agent_license_back_photo_url"));
    }

    private void saveVehicleDocuments() {
        preferenceManager.saveStringValue("jcb_crane_agent_insurance_image",
                preferenceManager.getStringValue("jcb_crane_agent_insurance_photo_url"));
        preferenceManager.saveStringValue("jcb_crane_agent_noc_image",
                preferenceManager.getStringValue("noc_photo_url"));
        preferenceManager.saveStringValue("jcb_crane_agent_pollution_certificate_image",
                preferenceManager.getStringValue("jcb_crane_agent_puc_photo_url"));
        preferenceManager.saveStringValue("jcb_crane_agent_rc_image",
                preferenceManager.getStringValue("jcb_crane_agent_rc_photo_url"));
        preferenceManager.saveStringValue("jcb_crane_agent_vehicle_image",
                preferenceManager.getStringValue("jcb_crane_agent_vehicle_front_photo_url"));
        preferenceManager.saveStringValue("jcb_crane_agent_vehicle_plate_image",
                preferenceManager.getStringValue("jcb_crane_agent_vehicle_plate_front_photo_url"));
    }

    private void saveDocumentNumbers() {
        preferenceManager.saveStringValue("jcb_crane_agent_driving_license_no",
                preferenceManager.getStringValue("jcb_crane_agent_license_no"));
        preferenceManager.saveStringValue("jcb_crane_agent_vehicle_plate_no",
                preferenceManager.getStringValue("jcb_crane_agent_vehicle_no"));
        preferenceManager.saveStringValue("jcb_crane_agent_rc_no",
                preferenceManager.getStringValue("jcb_crane_agent_rc_no"));
        preferenceManager.saveStringValue("jcb_crane_agent_insurance_no",
                preferenceManager.getStringValue("jcb_crane_agent_insurance_no"));
        preferenceManager.saveStringValue("jcb_crane_agent_noc_no",
                preferenceManager.getStringValue("jcb_crane_agent_noc_no"));
        preferenceManager.saveStringValue("jcb_crane_agent_vehicle_fuel_type",
                preferenceManager.getStringValue("jcb_crane_agent_vehicle_fuel_type"));
    }

    private void saveOwnerInfo() {
        preferenceManager.saveStringValue("jcb_crane_agent_owner_name",
                preferenceManager.getStringValue("jcb_crane_agent_owner_name"));
        preferenceManager.saveStringValue("jcb_crane_agent_owner_mobile_no",
                preferenceManager.getStringValue("jcb_crane_agent_owner_mobile_no"));
        preferenceManager.saveStringValue("jcb_crane_agent_owner_photo_url",
                preferenceManager.getStringValue("jcb_crane_agent_owner_selfie_photo_url"));
        preferenceManager.saveStringValue("jcb_crane_agent_owner_address",
                preferenceManager.getStringValue("jcb_crane_agent_owner_address"));
        preferenceManager.saveStringValue("jcb_crane_agent_owner_city_name",
                preferenceManager.getStringValue("jcb_crane_agent_owner_city_name"));
    }

    private void updateLoadingState() {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.submitButton.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}