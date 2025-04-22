package com.kapstranspvtltd.kaps_partner.jcb_crane_agent_activities.jcb_crane_main_documents;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.kapstranspvtltd.kaps_partner.R;

import com.kapstranspvtltd.kaps_partner.databinding.ActivityCabDriverVehicleDocumentsBinding;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityJcbCraneVehicleDocumentsVerificationBinding;
import com.kapstranspvtltd.kaps_partner.jcb_crane_agent_activities.documents.vehicle_documents.JcbCraneAgentPUCUploadActivity;
import com.kapstranspvtltd.kaps_partner.jcb_crane_agent_activities.documents.vehicle_documents.JcbCraneAgentVehicleImageUploadActivity;
import com.kapstranspvtltd.kaps_partner.jcb_crane_agent_activities.documents.vehicle_documents.JcbCraneAgentVehicleInsuranceUploadActivity;
import com.kapstranspvtltd.kaps_partner.jcb_crane_agent_activities.documents.vehicle_documents.JcbCraneAgentVehicleNOCUploadActivity;
import com.kapstranspvtltd.kaps_partner.jcb_crane_agent_activities.documents.vehicle_documents.JcbCraneAgentVehiclePlateNoUploadActivity;
import com.kapstranspvtltd.kaps_partner.jcb_crane_agent_activities.documents.vehicle_documents.JcbCraneAgentVehicleRCUploadActivity;
import com.kapstranspvtltd.kaps_partner.models.VehiclesModel;
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
import java.util.stream.Collectors;

public class JcbCraneVehicleDocumentsVerificationActivity extends AppCompatActivity {

    private static final String TAG = "CabDriverVehicleDocsVerification";

    private ActivityJcbCraneVehicleDocumentsVerificationBinding binding;
    private PreferenceManager preferenceManager;
    private List<VehiclesModel> vehicles;
    private List<String> fuelTypes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityJcbCraneVehicleDocumentsVerificationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        vehicles = new ArrayList<>();
        fuelTypes = Arrays.asList("Diesel", "Petrol", "CNG", "Electrical");

        preferenceManager = new PreferenceManager(this);
        setupUI();
//        fetchVehicles();
        setupFuelTypeSpinner();
        loadSavedData();
    }

    private void setupUI() {
        binding.backButton.setOnClickListener(v -> finish());
        binding.continueButton.setOnClickListener(v -> saveVehicleDetails());

        // Setup document click listeners
        binding.vehicleImagesCard.setOnClickListener(v ->
                startActivity(new Intent(this, JcbCraneAgentVehicleImageUploadActivity.class)));

        binding.vehiclePlateCard.setOnClickListener(v ->
                startActivity(new Intent(this, JcbCraneAgentVehiclePlateNoUploadActivity.class)));

        binding.rcCard.setOnClickListener(v ->
                startActivity(new Intent(this, JcbCraneAgentVehicleRCUploadActivity.class)));

        binding.insuranceCard.setOnClickListener(v ->
                startActivity(new Intent(this, JcbCraneAgentVehicleInsuranceUploadActivity.class)));

        binding.nocCard.setOnClickListener(v ->
                startActivity(new Intent(this, JcbCraneAgentVehicleNOCUploadActivity.class)));

        binding.pucCard.setOnClickListener(v ->
                startActivity(new Intent(this, JcbCraneAgentPUCUploadActivity.class)));
    }

    private void fetchVehicles() {
        JSONObject requestData = new JSONObject();
        try {
            requestData.put("category_id", 3);
        } catch (Exception e) {
            Log.e(TAG, "Error creating request data", e);
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                APIClient.baseUrl + "all_vehicles",
                requestData,
                response -> {
                    try {
                        if (response.has("results")) {
                            JSONArray results = response.getJSONArray("results");
                            vehicles.clear();

                            for (int i = 0; i < results.length(); i++) {
                                JSONObject vehicle = results.getJSONObject(i);
                                vehicles.add(new VehiclesModel(
                                        vehicle.getString("vehicle_id"),
                                        vehicle.getString("vehicle_name")
                                ));
                            }

                            setupVehicleSpinner();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing vehicles", e);
                        showToast("Error loading vehicles");
                    }
                },
                error -> {
                    Log.e(TAG, "Error fetching vehicles", error);
                    if (error.getMessage() != null &&
                            error.getMessage().contains("No Data Found")) {
                        showToast("No Vehicles Found");
                    }
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
    private void setupVehicleSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                vehicles.stream()
                        .map(VehiclesModel::getVehicleName)
                        .collect(Collectors.toList())
        );

        binding.vehicleSpinner.setAdapter(adapter);
        binding.vehicleSpinner.setOnItemClickListener((parent, view, position, id) -> {
            VehiclesModel selectedVehicle = vehicles.get(position);
            preferenceManager.saveStringValue("jcb_crane_agent_vehicle_id", selectedVehicle.getVehicleId());
        });
    }

    private void setupFuelTypeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                fuelTypes
        );

        binding.fuelTypeSpinner.setAdapter(adapter);
        binding.fuelTypeSpinner.setOnItemClickListener((parent, view, position, id) -> {
            String selectedFuel = fuelTypes.get(position);
            preferenceManager.saveStringValue("jcb_crane_agent_vehicle_fuel_type", selectedFuel);
        });
    }

    private void loadSavedData() {
        binding.vehicleNumberInput.setText(preferenceManager.getStringValue("jcb_crane_agent_vehicle_no"));

        // Load saved fuel type
        String savedFuelType = preferenceManager.getStringValue("jcb_crane_agent_vehicle_fuel_type");
        if (!savedFuelType.isEmpty()) {
            binding.fuelTypeSpinner.setText(savedFuelType, false);
        }

        // Load saved vehicle
        String savedVehicleId = preferenceManager.getStringValue("jcb_crane_agent_vehicle_id");
        if (!savedVehicleId.isEmpty()) {
            for (VehiclesModel vehicle : vehicles) {
                if (vehicle.getVehicleId().equals(savedVehicleId)) {
                    binding.vehicleSpinner.setText(vehicle.getVehicleName(), false);
                    break;
                }
            }
        }
    }

    private void saveVehicleDetails() {
        String vehicleNo = binding.vehicleNumberInput.getText().toString().trim();
        String selectedFuelType = binding.fuelTypeSpinner.getText().toString();
//        String selectedVehicleName = binding.vehicleSpinner.getText().toString();
//
//        VehiclesModel selectedVehicle = null;
//        for (VehiclesModel vehicle : vehicles) {
//            if (vehicle.getVehicleName().equals(selectedVehicleName)) {
//                selectedVehicle = vehicle;
//                break;
//            }
//        }

        if (vehicleNo.isEmpty()) {
            showToast("Please provide your valid vehicle number");
            return;
        }

        if (selectedFuelType.isEmpty() || !fuelTypes.contains(selectedFuelType)) {
            showToast("Please select vehicle fuel type");
            return;
        }

//        if (selectedVehicle == null) {
//            showToast("Please select your registration vehicle");
//            return;
//        }

        // Verify all required documents are uploaded
        if (!verifyDocuments()) {
            return;
        }

        // Save vehicle details
        preferenceManager.saveStringValue("jcb_crane_agent_vehicle_no", vehicleNo);
        preferenceManager.saveStringValue("jcb_crane_agent_vehicle_fuel_type", selectedFuelType);
//        preferenceManager.saveStringValue("jcb_crane_agent_vehicle_id", selectedVehicle.getVehicleId());

        // Navigate to next screen
        startActivity(new Intent(this, JcbCraneVehicleOwnerDetailsActivity.class));
        finish();
    }
    private boolean verifyDocuments() {
        String vehicleFrontPhoto = preferenceManager.getStringValue("jcb_crane_agent_vehicle_front_photo_url");
        String vehicleBackPhoto = preferenceManager.getStringValue("jcb_crane_agent_vehicle_back_photo_url");
        String plateFrontPhoto = preferenceManager.getStringValue("jcb_crane_agent_vehicle_plate_front_photo_url");
        String plateBackPhoto = preferenceManager.getStringValue("jcb_crane_agent_vehicle_plate_back_photo_url");
        String rcPhoto = preferenceManager.getStringValue("jcb_crane_agent_rc_photo_url");
        String rcNo = preferenceManager.getStringValue("jcb_crane_agent_rc_no");
        String insurancePhoto = preferenceManager.getStringValue("jcb_crane_agent_insurance_photo_url");
        String insuranceNo = preferenceManager.getStringValue("jcb_crane_agent_insurance_no");
        String nocPhoto = preferenceManager.getStringValue("jcb_crane_agent_noc_photo_url");
        String nocNo = preferenceManager.getStringValue("jcb_crane_agent_noc_no");
        String pucPhoto = preferenceManager.getStringValue("jcb_crane_agent_puc_photo_url");
        String pucNo = preferenceManager.getStringValue("jcb_crane_agent_puc_no");

        if (vehicleFrontPhoto.isEmpty() || vehicleBackPhoto.isEmpty()) {
            showToast("Please provide Vehicle Images");
            return false;
        }

        if (plateFrontPhoto.isEmpty() || plateBackPhoto.isEmpty()) {
            showToast("Please upload Vehicle Plate Images");
            return false;
        }

        if (rcPhoto.isEmpty() || rcNo.isEmpty()) {
            showToast("Please upload RC details");
            return false;
        }

        if (insurancePhoto.isEmpty() || insuranceNo.isEmpty()) {
            showToast("Please upload Insurance details");
            return false;
        }

        if (nocPhoto.isEmpty() || nocNo.isEmpty()) {
            showToast("Please upload NOC details");
            return false;
        }

        if (pucPhoto.isEmpty() || pucNo.isEmpty()) {
            showToast("Please upload PUC details");
            return false;
        }

        return true;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}