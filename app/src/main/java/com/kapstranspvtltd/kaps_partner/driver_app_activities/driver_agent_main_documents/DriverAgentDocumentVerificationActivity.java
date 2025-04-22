package com.kapstranspvtltd.kaps_partner.driver_app_activities.driver_agent_main_documents;

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
import android.widget.ArrayAdapter;
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


import com.kapstranspvtltd.kaps_partner.databinding.ActivityDriverAgentDocumentVerificationBinding;
import com.kapstranspvtltd.kaps_partner.driver_app_activities.DriverAgentHomeActivity;
import com.kapstranspvtltd.kaps_partner.driver_app_activities.documents.DriverAgentAadharCardUploadActivity;
import com.kapstranspvtltd.kaps_partner.driver_app_activities.documents.DriverAgentDrivingLicenseUploadActivity;
import com.kapstranspvtltd.kaps_partner.driver_app_activities.documents.DriverAgentPanCardUploadActivity;
import com.kapstranspvtltd.kaps_partner.driver_app_activities.documents.DriverAgentSelfieUploadActivity;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverAgentDocumentVerificationActivity extends AppCompatActivity {

    private static final String TAG = "DriverAgentDocumentVerificationActivity";
    private ActivityDriverAgentDocumentVerificationBinding binding;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private PreferenceManager preferenceManager;
    private String selectedGender;
    private String selectedCityId;

    private String selectedSubCategoryId = "";
    private String selectedServiceId = "-1";
    private String selectedSubCategoryName = "";
    private String selectedServiceName = "NA";

    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;

    private FusedLocationProviderClient fusedLocationClient;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDriverAgentDocumentVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        checkLocationPermission();
        setupUI();
        fetchCities();
        fetchSubCategories();
    }

    private void fetchSubCategories() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("cat_id", 4);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    APIClient.baseUrl + "get_all_sub_categories",
                    jsonObject,
                    response -> {
                        try {
                            JSONArray subCategories = response.getJSONArray("results");
                            List<String> subCategoryNames = new ArrayList<>();
                            List<String> subCategoryIds = new ArrayList<>();

                            for (int i = 0; i < subCategories.length(); i++) {
                                JSONObject subCategory = subCategories.getJSONObject(i);
                                subCategoryNames.add(subCategory.getString("sub_cat_name"));
                                subCategoryIds.add(subCategory.getString("sub_cat_id"));
                            }

                            ArrayAdapter<String> subCategoryAdapter = new ArrayAdapter<>(this,
                                    R.layout.item_dropdown, subCategoryNames);
                            binding.subCategoryDropdown.setAdapter(subCategoryAdapter);
                            binding.subCategoryDropdown.setOnItemClickListener((parent, view, position, id) -> {
                                selectedSubCategoryId = subCategoryIds.get(position);
                                selectedSubCategoryName = subCategoryNames.get(position);
                                fetchOtherServices(selectedSubCategoryId);
                            });

                        } catch (Exception e) {
                            e.printStackTrace();
                            showError("Failed to load subcategories");
                        }
                    },
                    error -> {
                        error.printStackTrace();
                        showError("Failed to fetch subcategories");
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
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error preparing subcategories request");
        }
    }

    private void fetchOtherServices(String subCatId) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("sub_cat_id", subCatId);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    APIClient.baseUrl + "get_all_sub_services",
                    jsonObject,
                    response -> {
                        try {
                            if (response.has("results")) {
                                JSONArray services = response.getJSONArray("results");
                                List<String> serviceNames = new ArrayList<>();
                                List<String> serviceIds = new ArrayList<>();

                                for (int i = 0; i < services.length(); i++) {
                                    JSONObject service = services.getJSONObject(i);
                                    serviceNames.add(service.getString("service_name"));
                                    serviceIds.add(service.getString("service_id"));
                                }

                                if (!serviceNames.isEmpty()) {
                                    binding.otherServiceDropdown.setVisibility(View.VISIBLE);
                                    ArrayAdapter<String> serviceAdapter = new ArrayAdapter<>(this,
                                            R.layout.item_dropdown, serviceNames);
                                    binding.otherServiceDropdown.setAdapter(serviceAdapter);
                                    binding.otherServiceDropdown.setOnItemClickListener((parent, view, position, id) -> {
                                        selectedServiceId = serviceIds.get(position);
                                        selectedServiceName = serviceNames.get(position);
                                    });

                                    // Auto select first service
                                    selectedServiceId = serviceIds.get(0);
                                    selectedServiceName = serviceNames.get(0);
                                    binding.otherServiceDropdown.setText(serviceNames.get(0), false);
                                } else {
                                    binding.otherServiceDropdown.setVisibility(View.GONE);
                                    selectedServiceId = "-1";
                                    selectedServiceName = "NA";
                                }
                            } else {
                                binding.otherServiceDropdown.setVisibility(View.GONE);
                                selectedServiceId = "-1";
                                selectedServiceName = "NA";
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            binding.otherServiceDropdown.setVisibility(View.GONE);
                            selectedServiceId = "-1";
                            selectedServiceName = "NA";
                        }
                    },
                    error -> {
                        error.printStackTrace();
                        binding.otherServiceDropdown.setVisibility(View.GONE);
                        selectedServiceId = "-1";
                        selectedServiceName = "NA";
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
        } catch (Exception e) {
            e.printStackTrace();
            binding.otherServiceDropdown.setVisibility(View.GONE);
            selectedServiceId = "-1";
            selectedServiceName = "NA";
        }
    }

    private void setupUI() {
        // Setup Gender Dropdown
        List<String> genders = Arrays.asList("Male", "Female", "Other");
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, R.layout.item_dropdown, genders);
        binding.genderDropdown.setAdapter(genderAdapter);
        binding.genderDropdown.setOnItemClickListener((parent, view, position, id) ->
                selectedGender = genders.get(position));

        // Setup Click Listeners
        binding.backButton.setOnClickListener(v -> finish());

        // Document Upload Clicks
        binding.drivingLicenseItem.setOnClickListener(v ->
                startActivity(new Intent(this, DriverAgentDrivingLicenseUploadActivity.class)));

        binding.aadharCardItem.setOnClickListener(v ->
                startActivity(new Intent(this, DriverAgentAadharCardUploadActivity.class)));

        binding.panCardItem.setOnClickListener(v ->
                startActivity(new Intent(this, DriverAgentPanCardUploadActivity.class)));

        binding.selfieItem.setOnClickListener(v ->
                startActivity(new Intent(this, DriverAgentSelfieUploadActivity.class)));

        binding.continueButton.setOnClickListener(v -> saveDriverDetails());

        // Restore saved values if any
        binding.nameInput.setText(preferenceManager.getStringValue("other_driver_name"));
        binding.addressInput.setText(preferenceManager.getStringValue("other_driver_full_address"));
        String savedGender = preferenceManager.getStringValue("other_driver_gender");
        if (!savedGender.isEmpty()) {
            selectedGender = savedGender;
            binding.genderDropdown.setText(savedGender, false);
        }
    }

    private void fetchCities() {
        JSONObject jsonObject = new JSONObject();

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                APIClient.baseUrl + "all_cities",
                jsonObject,
                response -> {
                    try {
                        JSONArray cities = response.getJSONArray("results");
                        List<String> cityNames = new ArrayList<>();
                        List<String> cityIds = new ArrayList<>();

                        for (int i = 0; i < cities.length(); i++) {
                            JSONObject city = cities.getJSONObject(i);
                            cityNames.add(city.getString("city_name"));
                            cityIds.add(city.getString("city_id"));
                        }

                        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(this,
                                R.layout.item_dropdown, cityNames);
                        binding.cityDropdown.setAdapter(cityAdapter);
                        binding.cityDropdown.setOnItemClickListener((parent, view, position, id) ->
                                selectedCityId = cityIds.get(position));

                        // Restore selected city if any
                        String savedCityId = preferenceManager.getStringValue("other_driver_city_id");
                        if (!savedCityId.isEmpty()) {
                            int index = cityIds.indexOf(savedCityId);
                            if (index != -1) {
                                binding.cityDropdown.setText(cityNames.get(index), false);
                                selectedCityId = savedCityId;
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        showError("Failed to load cities");
                    }
                },
                error -> {
                    error.printStackTrace();
                    showError("Failed to fetch cities");
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

    private void saveDriverDetails() {
        String name = binding.nameInput.getText().toString().trim();
        String address = binding.addressInput.getText().toString().trim();

        if (name.isEmpty()) {
            showError("Please provide your full name");
            return;
        }
        if (selectedGender == null) {
            showError("Please select your gender");
            return;
        }
        if (address.isEmpty()) {
            showError("Please provide your full address");
            return;
        }
        if (selectedCityId == null) {
            showError("Please select your registration city");
            return;
        }
        if (!checkDocuments()) {
            return;
        }

        // Save to SharedPreferences
        preferenceManager.saveStringValue("other_driver_name", name.trim());
        preferenceManager.saveStringValue("other_driver_full_address", address.trim());
        preferenceManager.saveStringValue("other_driver_gender", selectedGender);
        preferenceManager.saveStringValue("other_driver_city_id", selectedCityId);
        preferenceManager.saveStringValue("other_driver_sub_category_id", selectedSubCategoryId);
        preferenceManager.saveStringValue("other_driver_other_service_id", selectedServiceId);

        // Save Details to registerDriverAsync() use current location as well to save
registerDriver();
    }

    private void registerDriver() {
        try {
            JSONObject jsonObject = new JSONObject();



            jsonObject.put("other_driver_id", preferenceManager.getStringValue("other_driver_id"));
            jsonObject.put("driver_first_name", preferenceManager.getStringValue("other_driver_name"));
            jsonObject.put("profile_pic", preferenceManager.getStringValue("other_driver_selfie_photo_url"));
            jsonObject.put("mobile_no", preferenceManager.getStringValue("other_driver_mobile_no"));
            jsonObject.put("r_lat", String.valueOf(currentLatitude));
            jsonObject.put("r_lng", String.valueOf(currentLongitude));
            jsonObject.put("current_lat",String.valueOf(currentLatitude));
            jsonObject.put("current_lng", String.valueOf(currentLongitude));
            jsonObject.put("recent_online_pic", preferenceManager.getStringValue("other_driver_selfie_photo_url"));
            jsonObject.put("city_id", selectedCityId);
            jsonObject.put("aadhar_no", preferenceManager.getStringValue("other_driver_aadhar_no"));
            jsonObject.put("pan_card_no", preferenceManager.getStringValue("other_driver_pan_no"));
            jsonObject.put("full_address", preferenceManager.getStringValue("other_driver_driver_address"));
            jsonObject.put("gender", selectedGender);
            jsonObject.put("aadhar_card_front", preferenceManager.getStringValue("other_driver_aadhar_front_photo_url"));
            jsonObject.put("aadhar_card_back", preferenceManager.getStringValue("other_driver_aadhar_back_photo_url"));
            jsonObject.put("pan_card_front", preferenceManager.getStringValue("other_driver_pan_front_photo_url"));
            jsonObject.put("pan_card_back", preferenceManager.getStringValue("other_driver_pan_back_photo_url"));
            jsonObject.put("license_front", preferenceManager.getStringValue("other_driver_license_front_photo_url"));
            jsonObject.put("license_back", preferenceManager.getStringValue("other_driver_license_back_photo_url"));
            jsonObject.put("sub_cat_id", selectedSubCategoryId);
            jsonObject.put("service_id", selectedServiceId);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    APIClient.baseUrl + "other_driver_registration",
                    jsonObject,
                    response -> {
                        if (response.has("message")) {
                            // Registration successful
                            Intent intent = new Intent(this, DriverAgentHomeActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }
                    },
                    error -> {
                        error.printStackTrace();
                        showError("Registration failed");
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
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error preparing registration request");
        }
    }


    private boolean checkDocuments() {
        if (preferenceManager.getStringValue("other_driver_license_front_photo_url").isEmpty()) {
            showError("Please upload Driving License Front Picture");
            return false;
        }
        if (preferenceManager.getStringValue("other_driver_license_back_photo_url").isEmpty()) {
            showError("Please upload Driving License Back Picture");
            return false;
        }
        if (preferenceManager.getStringValue("other_driver_license_no").isEmpty()) {
            showError("Please upload Driving License Number");
            return false;
        }
        if (preferenceManager.getStringValue("other_driver_aadhar_front_photo_url").isEmpty()) {
            showError("Please upload Aadhar Front Picture");
            return false;
        }
        if (preferenceManager.getStringValue("other_driver_aadhar_back_photo_url").isEmpty()) {
            showError("Please upload Aadhar Back Picture");
            return false;
        }
        if (preferenceManager.getStringValue("other_driver_aadhar_no").isEmpty()) {
            showError("Please provide your Aadhar Number");
            return false;
        }
        if (preferenceManager.getStringValue("other_driver_pan_no").isEmpty()) {
            showError("Please upload PAN Number");
            return false;
        }
        if (preferenceManager.getStringValue("other_driver_pan_front_photo_url").isEmpty()) {
            showError("Please upload PAN Front Picture");
            return false;
        }
        if (preferenceManager.getStringValue("other_driver_pan_back_photo_url").isEmpty()) {
            showError("Please upload PAN Back Picture");
            return false;
        }
        if (preferenceManager.getStringValue("other_driver_selfie_photo_url").isEmpty()) {
            showError("Please upload your selfie");
            return false;
        }
        return true;
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
                    showError("Location permission is required for registration");
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
                    showError("Failed to get location");
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
                showError("Location permission denied");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}