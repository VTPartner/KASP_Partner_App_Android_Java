package com.kapstranspvtltd.kaps_partner.goods_driver_activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.adapters.GoodsAgentReferralsAdapter;
import com.kapstranspvtltd.kaps_partner.models.GoodsAgentReferralModel;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GoodsAgentInviteEarnActivity extends AppCompatActivity {

    private static final String TAG = "GoodsAgentInviteEarn";
    private static final String API_GENERATE_REFERRAL = APIClient.baseUrl+"generate_goods_agent_referral_code";
    private static final String API_REFERRAL_DETAILS = APIClient.baseUrl+"get_goods_agent_referral_details";

    // Views
    private ImageView btnBack;
    private TextView tvReferralCode;
    private LinearLayout btnCopyCode;
    private Button btnShareEarn;
    private TextView tvTotalEarnings;
    private TextView tvReferralCount,tvEarnAmountTextView;
    private LinearLayout layoutReferrals;
    private ImageView ivReferralToggle;
    private ProgressBar progressReferrals;
    private RecyclerView recyclerReferrals;
    private LinearLayout layoutEmptyReferrals;

    // Data
    private String referralCode = "";
    private String shareMessage = "";
    private boolean isReferralsExpanded = false;
    private List<GoodsAgentReferralModel> referralsList = new ArrayList<>();
    private GoodsAgentReferralsAdapter referralsAdapter;

    // Session
    private PreferenceManager preferenceManager;
    private String goodsDriverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goods_agent_invite_earn);

        preferenceManager = new PreferenceManager(this);

        initializeViews();
        initializeData();
        setupListeners();
        setupRecyclerView();
        loadReferralData();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        tvReferralCode = findViewById(R.id.tvReferralCode);
        btnCopyCode = findViewById(R.id.btnCopyCode);
        btnShareEarn = findViewById(R.id.btnShareEarn);
        tvTotalEarnings = findViewById(R.id.tvTotalEarnings);
        tvReferralCount = findViewById(R.id.tvReferralCount);
        layoutReferrals = findViewById(R.id.layoutReferrals);
        ivReferralToggle = findViewById(R.id.ivReferralToggle);
        progressReferrals = findViewById(R.id.progressReferrals);
        recyclerReferrals = findViewById(R.id.recyclerReferrals);
        layoutEmptyReferrals = findViewById(R.id.layoutEmptyReferrals);
        tvEarnAmountTextView = findViewById(R.id.tvEarnAmount);

        String signUpBonusGoodsAgent = preferenceManager.getStringValue("SIGN_UP_BONUS_GOODS_AGENT");
        if(signUpBonusGoodsAgent == null || signUpBonusGoodsAgent.isEmpty()) signUpBonusGoodsAgent ="10";
        tvEarnAmountTextView.setText("₹"+signUpBonusGoodsAgent+"");
    }

    private void initializeData() {
        goodsDriverId = preferenceManager.getStringValue("goods_driver_id");

        referralsAdapter = new GoodsAgentReferralsAdapter(referralsList);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnCopyCode.setOnClickListener(v -> copyReferralCode());

        btnShareEarn.setOnClickListener(v -> shareReferralCode());

        layoutReferrals.setOnClickListener(v -> toggleReferralsList());
    }

    private void setupRecyclerView() {
        recyclerReferrals.setLayoutManager(new LinearLayoutManager(this));
        recyclerReferrals.setAdapter(referralsAdapter);
    }

    private void loadReferralData() {
        if (TextUtils.isEmpty(goodsDriverId)) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        generateReferralCode();
    }

    private void generateReferralCode() {
        try {
            JSONObject requestData = new JSONObject();
            requestData.put("goods_driver_id", goodsDriverId);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    API_GENERATE_REFERRAL,
                    requestData,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                if (response.getString("status").equals("success")) {
                                    referralCode = response.getString("referral_code");
                                    shareMessage = response.getString("share_message");

                                    tvReferralCode.setText(referralCode);

                                    // Update statistics
                                    JSONObject statistics = response.getJSONObject("statistics");
                                    updateStatistics(statistics);

                                    // Load detailed referral data
                                    loadReferralDetails();
                                } else {
                                    Toast.makeText(GoodsAgentInviteEarnActivity.this,
                                            response.getString("message"), Toast.LENGTH_SHORT).show();
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "JSON parsing error: " + e.getMessage());
                                Toast.makeText(GoodsAgentInviteEarnActivity.this,
                                        "Error processing data", Toast.LENGTH_SHORT).show();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, "Volley error: " + error.getMessage());
                            Toast.makeText(GoodsAgentInviteEarnActivity.this,
                                    "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

            VolleySingleton.getInstance(this).addToRequestQueue(request);

        } catch (JSONException e) {
            Log.e(TAG, "JSON creation error: " + e.getMessage());
        }
    }

    private void loadReferralDetails() {
        try {
            JSONObject requestData = new JSONObject();
            requestData.put("goods_driver_id", goodsDriverId);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    API_REFERRAL_DETAILS,
                    requestData,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                if (response.getString("status").equals("success")) {
                                    JSONArray referralsArray = response.getJSONArray("referrals");
                                    updateReferralsList(referralsArray);
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "JSON parsing error: " + e.getMessage());
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, "Volley error: " + error.getMessage());
                        }
                    }
            );

            VolleySingleton.getInstance(this).addToRequestQueue(request);

        } catch (JSONException e) {
            Log.e(TAG, "JSON creation error: " + e.getMessage());
        }
    }

    private void updateStatistics(JSONObject statistics) {
        try {
            double totalEarnings = statistics.getDouble("total_earnings");
            int totalReferrals = statistics.getInt("total_referrals");
            int completedReferrals = statistics.getInt("completed_referrals");

            tvTotalEarnings.setText("₹" + String.format("%.0f", totalEarnings));
            tvReferralCount.setText(completedReferrals + " of 10");

            // Update progress bar
            int progress = (int) ((completedReferrals / 10.0) * 100);
            progressReferrals.setProgress(progress);

        } catch (JSONException e) {
            Log.e(TAG, "Statistics parsing error: " + e.getMessage());
        }
    }

    private void updateReferralsList(JSONArray referralsArray) {
        referralsList.clear();

        for (int i = 0; i < referralsArray.length(); i++) {
            try {
                JSONObject referralObj = referralsArray.getJSONObject(i);

                GoodsAgentReferralModel referral = new GoodsAgentReferralModel();
                referral.setDriverName(referralObj.getString("driver_name"));
                referral.setUsedAt(referralObj.getString("used_at"));
                referral.setStatus(referralObj.getString("status"));
                referral.setAmount(referralObj.getDouble("amount"));
                referral.setPosition(i + 1);

                referralsList.add(referral);

            } catch (JSONException e) {
                Log.e(TAG, "Referral item parsing error: " + e.getMessage());
            }
        }

        referralsAdapter.notifyDataSetChanged();

        // Show empty state if no referrals
        if (referralsList.isEmpty()) {
            layoutEmptyReferrals.setVisibility(View.VISIBLE);
            recyclerReferrals.setVisibility(View.GONE);
        } else {
            layoutEmptyReferrals.setVisibility(View.GONE);
            recyclerReferrals.setVisibility(View.VISIBLE);
        }
    }

    private void copyReferralCode() {
        if (TextUtils.isEmpty(referralCode)) {
            Toast.makeText(this, "Referral code not available", Toast.LENGTH_SHORT).show();
            return;
        }

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Referral Code", referralCode);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "Referral code copied!", Toast.LENGTH_SHORT).show();
    }

    private void shareReferralCode() {
        if (TextUtils.isEmpty(shareMessage)) {
            Toast.makeText(this, "Share message not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Join KAPS as a Goods Agent with my referral code!");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);

        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void toggleReferralsList() {
        isReferralsExpanded = !isReferralsExpanded;

        if (isReferralsExpanded) {
            if (referralsList.isEmpty()) {
                layoutEmptyReferrals.setVisibility(View.VISIBLE);
                recyclerReferrals.setVisibility(View.GONE);
            } else {
                layoutEmptyReferrals.setVisibility(View.GONE);
                recyclerReferrals.setVisibility(View.VISIBLE);
            }
            ivReferralToggle.setRotation(180f);
        } else {
            layoutEmptyReferrals.setVisibility(View.GONE);
            recyclerReferrals.setVisibility(View.GONE);
            ivReferralToggle.setRotation(0f);
        }
    }

    // Updated sharing method to include referral code
    public void openToShareAppInviteLink() {
        if (TextUtils.isEmpty(referralCode)) {
            // Fallback to original sharing if no referral code
            String appUrl = "https://play.google.com/store/apps/details?id=com.kapstranspvtltd.kaps_partner&hl=en_IN";
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out KAPS Agent App!");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Join KAPS as a Goods Agent and start earning! \uD83D\uDE80 Whether you want to provide Goods Delivery, Cab Services, JCB & Crane Services, Professional Driving, or Handyman Services, KAPS has opportunities for you. Download now and start your journey! \uD83D\uDD17 Get the app here: " + appUrl);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        } else {
            shareReferralCode();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to activity
        if (!TextUtils.isEmpty(goodsDriverId)) {
            loadReferralDetails();
        }
    }
} 