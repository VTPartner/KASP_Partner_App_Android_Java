package com.kapstranspvtltd.kaps_partner.driver_app_activities.settings_pages;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityDriverAgentEditProfileBinding;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class DriverAgentEditProfileActivity extends AppCompatActivity {
    private ActivityDriverAgentEditProfileBinding binding;
    private String driverId;
    private ProgressDialog progressDialog;
    PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDriverAgentEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(this);

        driverId = preferenceManager.getStringValue("other_driver_id");
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


        String token = preferenceManager.getStringValue("other_driver_token");

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("driver_id", driverId);
            jsonBody.put("driver_unique_id", driverId);
            jsonBody.put("auth", token);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
                APIClient.baseUrl + "get_other_driver_details",
                jsonBody,
                response -> {
                    progressDialog.dismiss();
                    try {
                        JSONObject driver = response.getJSONObject("driver");
                        populateFields(driver);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing driver details", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error loading driver details", Toast.LENGTH_SHORT).show();
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
    }

    private void showFullScreenImage(ImageView imageView) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView fullScreenImageView = new ImageView(this);
        fullScreenImageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        fullScreenImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        fullScreenImageView.setImageDrawable(imageView.getDrawable());
        dialog.setContentView(fullScreenImageView);
        dialog.show();
        fullScreenImageView.setOnClickListener(v -> dialog.dismiss());
    }

    private void setupUpdateButton() {
        binding.buttonUpdate.setOnClickListener(v -> updateProfile());
    }

    private void updateProfile() {
        progressDialog.show();


        String token = preferenceManager.getStringValue("other_driver_token");

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
                APIClient.baseUrl + "update_other_driver_details",
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