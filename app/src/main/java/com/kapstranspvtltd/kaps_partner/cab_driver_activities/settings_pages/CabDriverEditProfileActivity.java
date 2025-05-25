package com.kapstranspvtltd.kaps_partner.cab_driver_activities.settings_pages;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.NetworkError;
import com.android.volley.Request;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityCabDriverEditProfileBinding;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CabDriverEditProfileActivity extends AppCompatActivity {

    private ActivityCabDriverEditProfileBinding binding;
    private String driverId;
    private ProgressDialog progressDialog;
    PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCabDriverEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(this);
//        setSupportActionBar(binding.toolbar);
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//            getSupportActionBar().setTitle("Driver Profile");
//        }

        driverId = preferenceManager.getStringValue("cab_driver_id");
        if (driverId == null || driverId.isEmpty()) {
            Toast.makeText(this, "Invalid driver ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        loadDriverDetails();
        setupUpdateButton();
    }

    private void loadDriverDetails() {
        progressDialog.show();

        String driverId = preferenceManager.getStringValue("cab_driver_id");
        String token = preferenceManager.getStringValue("cab_driver_token");

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("driver_id", driverId);
            jsonBody.put("driver_unique_id", driverId);
            jsonBody.put("auth", token);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
                APIClient.baseUrl + "get_cab_driver_details",
                jsonBody,
                response -> {
                    progressDialog.dismiss();
                    try {
                        JSONObject driver = response.getJSONObject("driver");
                        populateFields(driver);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        System.out.println("Error parsing");
                        Toast.makeText(this, "Error parsing driver details", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressDialog.dismiss();
//                    Toast.makeText(this, "Error loading driver details", Toast.LENGTH_SHORT).show();
                    handleError(error);
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    private void handleError(VolleyError error) {
        String message;
        // Check if there's a network response
        if (error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;

            switch (statusCode) {

                case 404:
                    message = "No Profile details found";
                    break;
                case 400:
                    message = "Bad request";
                    break;
                case 500:
                    message = "Server error";
                    break;
                default:
                    message = "Error fetching plan details";
                    break;
            }
        } else {
            // Handle cases where there's no network response
            if (error instanceof NetworkError) {
                message = "No internet connection";
            } else if (error instanceof TimeoutError) {
                message = "Request timed out";
            } else if (error instanceof ServerError) {
                message = "Server error";
            } else {
                message = "Error fetching plan details";
            }
        }
        showError(message);

    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void populateFields(JSONObject driver) throws JSONException {
        // Load profile image
        Glide.with(this)
                .load(driver.getString("profile_pic"))
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(binding.imageViewProfile);

        // Set Driver ID
        binding.textViewDriverId.setText("Driver ID: " + driver.getString("driver_id"));

        // Set editable fields - Personal Information
        binding.editTextFirstName.setText(driver.getString("first_name"));
        binding.editTextLastName.setText(driver.getString("last_name"));

        // Set editable fields - Bank Details
        binding.editTextBankName.setText(driver.getString("bank_name"));
        binding.editTextIfscCode.setText(driver.getString("ifsc_code"));
        binding.editTextAccountNumber.setText(driver.getString("account_number"));
        binding.editTextAccountName.setText(driver.getString("account_name"));

        // Set read-only fields - Personal Information
        binding.textViewMobile.setText(driver.getString("mobile_no"));
        binding.textViewGender.setText(driver.getString("gender"));
        binding.textViewAddress.setText(driver.getString("full_address"));

        // Set read-only fields - Document Information
        binding.textViewAadharNo.setText(driver.getString("aadhar_no"));
        binding.textViewPanNo.setText(driver.getString("pan_no"));
        binding.textViewLicenseNo.setText(driver.getString("license_no"));

        // Set read-only fields - Vehicle Information
        binding.textViewVehiclePlateNo.setText(driver.getString("vehicle_plate_no"));
        binding.textViewRcNo.setText(driver.getString("rc_no"));
        binding.textViewInsuranceNo.setText(driver.getString("insurance_no"));
        binding.textViewNocNo.setText(driver.getString("noc_no"));
        binding.textViewFuelType.setText(driver.getString("vehicle_fuel_type"));
        binding.textViewVehicleName.setText(driver.getString("vehicle_name"));

        // Load Document Images - Aadhar Card
        Glide.with(this)
                .load(driver.getString("aadhar_card_front"))
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(binding.imageViewAadharFront);

        Glide.with(this)
                .load(driver.getString("aadhar_card_back"))
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(binding.imageViewAadharBack);

        // Load Document Images - PAN Card
        Glide.with(this)
                .load(driver.getString("pan_card_front"))
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(binding.imageViewPanFront);

        Glide.with(this)
                .load(driver.getString("pan_card_back"))
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(binding.imageViewPanBack);

        // Load Document Images - License
        Glide.with(this)
                .load(driver.getString("license_front"))
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(binding.imageViewLicenseFront);

        Glide.with(this)
                .load(driver.getString("license_back"))
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(binding.imageViewLicenseBack);

        // Load Vehicle Document Images
        Glide.with(this)
                .load(driver.getString("rc_image"))
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(binding.imageViewRC);

        Glide.with(this)
                .load(driver.getString("insurance_image"))
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(binding.imageViewInsurance);

        Glide.with(this)
                .load(driver.getString("pollution_certificate_image"))
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(binding.imageViewPollution);

        Glide.with(this)
                .load(driver.getString("noc_image"))
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(binding.imageViewNoc);

        // Load Vehicle Images
        Glide.with(this)
                .load(driver.getString("vehicle_image"))
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(binding.imageViewVehicle);

        Glide.with(this)
                .load(driver.getString("vehicle_plate_image"))
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(binding.imageViewVehiclePlateNo);

        // Set registration date
//        String registrationDate = driver.getString("registration_date");
//        if (registrationDate != null && !registrationDate.equals("NA")) {
//            binding.textViewRegistrationDate.setText("Registered on: " + registrationDate);
//        }

        // Set ratings if available
//        String ratings = driver.getString("ratings");
//        if (ratings != null && !ratings.equals("NA") && !ratings.equals("0")) {
//            binding.textViewRatings.setText("Rating: " + ratings);
//        }

        // Handle image click listeners for full-screen view
        setupImageClickListeners();
    }

    private void setupImageClickListeners() {
        View.OnClickListener imageClickListener = v -> {
            ImageView imageView = (ImageView) v;
            showFullScreenImage(imageView);
        };

        // Set click listeners for all document images
        binding.imageViewAadharFront.setOnClickListener(imageClickListener);
        binding.imageViewAadharBack.setOnClickListener(imageClickListener);
        binding.imageViewPanFront.setOnClickListener(imageClickListener);
        binding.imageViewPanBack.setOnClickListener(imageClickListener);
        binding.imageViewLicenseFront.setOnClickListener(imageClickListener);
        binding.imageViewLicenseBack.setOnClickListener(imageClickListener);
        binding.imageViewRC.setOnClickListener(imageClickListener);
        binding.imageViewInsurance.setOnClickListener(imageClickListener);
        binding.imageViewPollution.setOnClickListener(imageClickListener);
        binding.imageViewNoc.setOnClickListener(imageClickListener);
        binding.imageViewVehicle.setOnClickListener(imageClickListener);
        binding.textViewVehiclePlateNo.setOnClickListener(imageClickListener);
    }

    private void showFullScreenImage(ImageView imageView) {
        // Create a dialog to show the image in full screen
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView fullScreenImageView = new ImageView(this);
        fullScreenImageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        fullScreenImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        // Get the drawable from the clicked ImageView and set it to the full-screen ImageView
        fullScreenImageView.setImageDrawable(imageView.getDrawable());

        dialog.setContentView(fullScreenImageView);
        dialog.show();

        // Dismiss dialog when clicked
        fullScreenImageView.setOnClickListener(v -> dialog.dismiss());
    }

    private void setupUpdateButton() {
        binding.buttonUpdate.setOnClickListener(v -> updateProfile());
    }

    private void updateProfile() {
        progressDialog.show();


        String token = preferenceManager.getStringValue("cab_driver_token");

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("driver_id", driverId);
            jsonBody.put("first_name", binding.editTextFirstName.getText().toString());
            jsonBody.put("last_name", binding.editTextLastName.getText().toString());
            jsonBody.put("bank_name", binding.editTextBankName.getText().toString());
            jsonBody.put("ifsc_code", binding.editTextIfscCode.getText().toString());
            jsonBody.put("account_number", binding.editTextAccountNumber.getText().toString());
            jsonBody.put("account_name", binding.editTextAccountName.getText().toString());
            jsonBody.put("driver_unique_id", driverId);
            jsonBody.put("auth", token);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
                APIClient.baseUrl + "update_cab_driver_details",
                jsonBody,
                response -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                },
                error -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error updating profile", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}