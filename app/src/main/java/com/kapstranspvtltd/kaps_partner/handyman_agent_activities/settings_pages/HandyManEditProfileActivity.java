package com.kapstranspvtltd.kaps_partner.handyman_agent_activities.settings_pages;

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
import com.kapstranspvtltd.kaps_partner.databinding.ActivityHandyManEditProfileBinding;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class HandyManEditProfileActivity extends AppCompatActivity {
    private ActivityHandyManEditProfileBinding binding;
    private String handymanId;
    private ProgressDialog progressDialog;
    PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHandyManEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(this);

        handymanId = preferenceManager.getStringValue("handyman_agent_id");
        if (handymanId == null || handymanId.isEmpty()) {
            Toast.makeText(this, "Invalid handyman ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        loadHandymanDetails();
        setupUpdateButton();
    }

    private void loadHandymanDetails() {
        progressDialog.show();

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("handyman_id", handymanId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
                APIClient.baseUrl + "get_handyman_details",
                jsonBody,
                response -> {
                    progressDialog.dismiss();
                    try {
                        JSONObject handyman = response.getJSONObject("handyman");
                        populateFields(handyman);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing handyman details", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Error loading handyman details", Toast.LENGTH_SHORT).show();
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

    private void populateFields(JSONObject handyman) throws JSONException {
        // Load profile image
        Glide.with(this)
                .load(handyman.getString("profile_pic"))
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(binding.imageViewProfile);

        // Set Handyman ID
        binding.textViewHandymanId.setText("Handyman ID: " + handyman.getString("handyman_id"));

        // Set editable fields - Personal Information
        binding.editTextName.setText(handyman.getString("name"));

        // Set editable fields - Bank Details
        binding.editTextBankName.setText(handyman.getString("bank_name"));
        binding.editTextIfscCode.setText(handyman.getString("ifsc_code"));
        binding.editTextAccountNumber.setText(handyman.getString("account_number"));
        binding.editTextAccountName.setText(handyman.getString("account_name"));

        // Set read-only fields - Personal Information
        binding.textViewMobile.setText(handyman.getString("mobile_no"));
        binding.textViewGender.setText(handyman.getString("gender"));
        binding.textViewAddress.setText(handyman.getString("full_address"));

        // Set read-only fields - Document Information
        binding.textViewAadharNo.setText(handyman.getString("aadhar_no"));
        binding.textViewPanNo.setText(handyman.getString("pan_no"));

        // Set service information
        binding.textViewServiceName.setText(handyman.getString("service_name"));
        binding.textViewSubCategory.setText(handyman.getString("sub_cat_name"));

        // Load Document Images - Aadhar Card
        Glide.with(this)
                .load(handyman.getString("aadhar_card_front"))
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(binding.imageViewAadharFront);

        Glide.with(this)
                .load(handyman.getString("aadhar_card_back"))
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(binding.imageViewAadharBack);

        // Load Document Images - PAN Card
        Glide.with(this)
                .load(handyman.getString("pan_card_front"))
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(binding.imageViewPanFront);

        Glide.with(this)
                .load(handyman.getString("pan_card_back"))
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(binding.imageViewPanBack);

        // Load Document Images - License (if available)
        if (!handyman.getString("license_front").equals("NA")) {
            binding.licenseContainer.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(handyman.getString("license_front"))
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(binding.imageViewLicenseFront);

            Glide.with(this)
                    .load(handyman.getString("license_back"))
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(binding.imageViewLicenseBack);
        } else {
            binding.licenseContainer.setVisibility(View.GONE);
        }

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

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("handyman_id", handymanId);
            jsonBody.put("name", binding.editTextName.getText().toString());
            jsonBody.put("bank_name", binding.editTextBankName.getText().toString());
            jsonBody.put("ifsc_code", binding.editTextIfscCode.getText().toString());
            jsonBody.put("account_number", binding.editTextAccountNumber.getText().toString());
            jsonBody.put("account_name", binding.editTextAccountName.getText().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
                APIClient.baseUrl + "update_handyman_details",
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