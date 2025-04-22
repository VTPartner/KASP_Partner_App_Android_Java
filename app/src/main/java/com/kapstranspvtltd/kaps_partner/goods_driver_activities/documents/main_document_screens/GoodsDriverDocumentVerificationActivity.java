package com.kapstranspvtltd.kaps_partner.goods_driver_activities.documents.main_document_screens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.kapstranspvtltd.kaps_partner.goods_driver_activities.documents.driver_documents.GoodsDriverAadharCardUploadActivity;
import com.kapstranspvtltd.kaps_partner.goods_driver_activities.documents.driver_documents.GoodsDriverDrivingLicenseUploadActivity;
import com.kapstranspvtltd.kaps_partner.goods_driver_activities.documents.driver_documents.GoodsDriverOwnerSelfieUploadActivity;
import com.kapstranspvtltd.kaps_partner.goods_driver_activities.documents.driver_documents.GoodsDriverPanCardUploadActivity;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityGoodsDriverDocumentVerificationBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoodsDriverDocumentVerificationActivity extends AppCompatActivity {

    private ActivityGoodsDriverDocumentVerificationBinding binding;
    private PreferenceManager preferenceManager;
    private String selectedGender;
    private String selectedCityId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGoodsDriverDocumentVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(this);
        setupUI();
        fetchCities();
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
                startActivity(new Intent(this, GoodsDriverDrivingLicenseUploadActivity.class)));

        binding.aadharCardItem.setOnClickListener(v ->
                startActivity(new Intent(this, GoodsDriverAadharCardUploadActivity.class)));

        binding.panCardItem.setOnClickListener(v ->
                startActivity(new Intent(this, GoodsDriverPanCardUploadActivity.class)));

        binding.selfieItem.setOnClickListener(v ->
                startActivity(new Intent(this, GoodsDriverOwnerSelfieUploadActivity.class)));

        binding.continueButton.setOnClickListener(v -> saveDriverDetails());

        // Restore saved values if any
        binding.nameInput.setText(preferenceManager.getStringValue("driver_name"));
        binding.addressInput.setText(preferenceManager.getStringValue("full_address"));
        String savedGender = preferenceManager.getStringValue("driver_gender");
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
                        String savedCityId = preferenceManager.getStringValue("driver_city_id");
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
        preferenceManager.saveStringValue("driver_name", name.trim());
        preferenceManager.saveStringValue("full_address", address.trim());
        preferenceManager.saveStringValue("driver_gender", selectedGender);
        preferenceManager.saveStringValue("driver_city_id", selectedCityId);

        // Navigate to next screen
        startActivity(new Intent(this, GoodsDriverVehicleDocumentsVerificationActivity.class));
    }

    private boolean checkDocuments() {
        if (preferenceManager.getStringValue("license_front_photo_url").isEmpty()) {
            showError("Please upload Driving License Front Picture");
            return false;
        }
        if (preferenceManager.getStringValue("license_back_photo_url").isEmpty()) {
            showError("Please upload Driving License Back Picture");
            return false;
        }
        if (preferenceManager.getStringValue("license_no").isEmpty()) {
            showError("Please upload Driving License Number");
            return false;
        }
        if (preferenceManager.getStringValue("aadhar_front_photo_url").isEmpty()) {
            showError("Please upload Aadhar Front Picture");
            return false;
        }
        if (preferenceManager.getStringValue("aadhar_back_photo_url").isEmpty()) {
            showError("Please upload Aadhar Back Picture");
            return false;
        }
        if (preferenceManager.getStringValue("aadhar_no").isEmpty()) {
            showError("Please provide your Aadhar Number");
            return false;
        }
        if (preferenceManager.getStringValue("pan_no").isEmpty()) {
            showError("Please upload PAN Number");
            return false;
        }
        if (preferenceManager.getStringValue("pan_front_photo_url").isEmpty()) {
            showError("Please upload PAN Front Picture");
            return false;
        }
        if (preferenceManager.getStringValue("pan_back_photo_url").isEmpty()) {
            showError("Please upload PAN Back Picture");
            return false;
        }
        if (preferenceManager.getStringValue("selfie_photo_url").isEmpty()) {
            showError("Please upload your selfie");
            return false;
        }
        return true;
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}