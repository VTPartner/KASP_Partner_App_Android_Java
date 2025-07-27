package com.kapstranspvtltd.kaps_partner.goods_driver_activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

public class GoodsAgentEnterReferralActivity extends AppCompatActivity {

    private static final String TAG = "GoodsAgentEnterReferral";
    private static final String API_VALIDATE_REFERRAL = APIClient.baseUrl+"validate_goods_agent_referral_code";
    private static final String API_APPLY_REFERRAL = APIClient.baseUrl+"apply_goods_agent_referral_code";

    // Views
    private ImageView btnBack;
    private TextInputLayout tilReferralCode;
    private TextInputEditText etReferralCode;
    private TextView tvValidationMessage;
    private LinearLayout layoutSuccessMessage;
    private TextView tvReferrerName;
    private Button btnApplyCode;
    private Button btnSkip;
    private FrameLayout loadingOverlay;
    private TextView tvLoadingMessage,getBonusTextView,subTitleTextView;

    // Data
    private String referralCode = "";
    private String referrerName = "";
    private boolean isCodeValid = false;
    private PreferenceManager preferenceManager;
    private String goodsDriverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goods_agent_enter_referral);

        preferenceManager = new PreferenceManager(this);

        initializeViews();
        initializeData();
        setupListeners();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        tilReferralCode = findViewById(R.id.tilReferralCode);
        etReferralCode = findViewById(R.id.etReferralCode);
        tvValidationMessage = findViewById(R.id.tvValidationMessage);
        layoutSuccessMessage = findViewById(R.id.layoutSuccessMessage);
        tvReferrerName = findViewById(R.id.tvReferrerName);
        btnApplyCode = findViewById(R.id.btnApplyCode);
        btnSkip = findViewById(R.id.btnSkip);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        tvLoadingMessage = findViewById(R.id.tvLoadingMessage);
        getBonusTextView = findViewById(R.id.getBonusTxt);
        subTitleTextView = findViewById(R.id.subTitle);

        String signUpBonusGoodsAgent = preferenceManager.getStringValue("SIGN_UP_BONUS_GOODS_AGENT");
        if(signUpBonusGoodsAgent == null || signUpBonusGoodsAgent.isEmpty()) signUpBonusGoodsAgent ="10";
        getBonusTextView.setText("Get ₹"+signUpBonusGoodsAgent+" bonus in your wallet instantly");
        subTitleTextView.setText("Enter your friend's referral code to get ₹"+signUpBonusGoodsAgent+" bonus!");
    }

    private void initializeData() {
        goodsDriverId = preferenceManager.getStringValue("goods_driver_id");
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSkip.setOnClickListener(v -> {
            // Navigate to main activity or finish
            finish();
        });

        btnApplyCode.setOnClickListener(v -> applyReferralCode());

        etReferralCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String code = s.toString().trim().toUpperCase();
                if (code.length() == 6) {
                    validateReferralCode(code);
                } else {
                    resetValidationState();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void validateReferralCode(String code) {
        if (TextUtils.isEmpty(goodsDriverId)) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject requestData = new JSONObject();
            requestData.put("referral_code", code);
            requestData.put("goods_driver_id", goodsDriverId);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    API_VALIDATE_REFERRAL,
                    requestData,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                if (response.getString("status").equals("success")) {
                                    isCodeValid = response.getBoolean("valid");
                                    if (isCodeValid) {
                                        referrerName = response.getString("referrer_name");
                                        showSuccessMessage();
                                    } else {
                                        showErrorMessage(response.getString("message"));
                                    }
                                } else {
                                    showErrorMessage(response.getString("message"));
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "JSON parsing error: " + e.getMessage());
                                showErrorMessage("Error validating code");
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, "Volley error: " + error.getMessage());
                            showErrorMessage("Network error. Please try again.");
                        }
                    }
            );

            VolleySingleton.getInstance(this).addToRequestQueue(request);

        } catch (JSONException e) {
            Log.e(TAG, "JSON creation error: " + e.getMessage());
        }
    }

    private void applyReferralCode() {
        if (!isCodeValid || TextUtils.isEmpty(referralCode)) {
            Toast.makeText(this, "Please enter a valid referral code", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true, "Applying referral code...");

        try {
            JSONObject requestData = new JSONObject();
            requestData.put("goods_driver_id", goodsDriverId);
            requestData.put("referral_code", referralCode);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    API_APPLY_REFERRAL,
                    requestData,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            showLoading(false, "");
                            try {
                                if (response.getString("status").equals("success")) {
                                    String message = response.getString("message");
                                    double bonusAmount = response.getDouble("bonus_amount");

                                    // Show success dialog
                                    showSuccessDialog(message, bonusAmount);

                                } else {
                                    String errorMessage = response.getString("message");
                                    showErrorMessage(errorMessage);
                                    Toast.makeText(GoodsAgentEnterReferralActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "JSON parsing error: " + e.getMessage());
                                Toast.makeText(GoodsAgentEnterReferralActivity.this, "Error processing response", Toast.LENGTH_SHORT).show();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            showLoading(false, "");
                            Log.e(TAG, "Volley error: " + error.getMessage());
                            Toast.makeText(GoodsAgentEnterReferralActivity.this, "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

            VolleySingleton.getInstance(this).addToRequestQueue(request);

        } catch (JSONException e) {
            showLoading(false, "");
            Log.e(TAG, "JSON creation error: " + e.getMessage());
        }
    }

    private void showSuccessMessage() {
        referralCode = etReferralCode.getText().toString().trim().toUpperCase();

        tvValidationMessage.setVisibility(View.GONE);
        layoutSuccessMessage.setVisibility(View.VISIBLE);
        tvReferrerName.setText("Referred by: " + referrerName);
        btnApplyCode.setEnabled(true);

        // Clear any error state
        tilReferralCode.setError(null);
    }

    private void showErrorMessage(String message) {
        isCodeValid = false;
        referralCode = "";

        layoutSuccessMessage.setVisibility(View.GONE);
        tvValidationMessage.setText(message);
        tvValidationMessage.setVisibility(View.VISIBLE);
        btnApplyCode.setEnabled(false);

        // Set error state
        tilReferralCode.setError(message);
    }

    private void resetValidationState() {
        isCodeValid = false;
        referralCode = "";

        layoutSuccessMessage.setVisibility(View.GONE);
        tvValidationMessage.setVisibility(View.GONE);
        btnApplyCode.setEnabled(false);

        // Clear error state
        tilReferralCode.setError(null);
    }

    private void showLoading(boolean show, String message) {
        if (show) {
            tvLoadingMessage.setText(message);
            loadingOverlay.setVisibility(View.VISIBLE);
        } else {
            loadingOverlay.setVisibility(View.GONE);
        }
    }

    private void showSuccessDialog(String message, double bonusAmount) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Success! 🎉")
                .setMessage(message + "\n\nYour wallet has been credited with ₹" + String.format("%.0f", bonusAmount))
                .setPositiveButton("Great!", (dialog, which) -> {
                    dialog.dismiss();
                    finish(); // Close the activity
                })
                .setCancelable(false)
                .show();
    }

    public static void startActivity(android.content.Context context) {
        Intent intent = new Intent(context, GoodsAgentEnterReferralActivity.class);
        context.startActivity(intent);
    }
} 