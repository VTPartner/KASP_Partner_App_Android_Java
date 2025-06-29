package com.kapstranspvtltd.kaps_partner.goods_driver_activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.kapstranspvtltd.kaps_partner.common_activities.adapters.WalletHistoryAdapter;
import com.kapstranspvtltd.kaps_partner.common_activities.models.WalletTransaction;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityGoodsAgentWalletBinding;
import com.kapstranspvtltd.kaps_partner.goods_driver_activities.withdrawals.GoodsDriverWithdrawBottomSheet;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.utils.CustPrograssbar;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;
import com.razorpay.Checkout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GoodsAgentWalletActivity extends AppCompatActivity implements GoodsDriverWithdrawBottomSheet.WithdrawListener{

    private ActivityGoodsAgentWalletBinding binding;
    private PreferenceManager preferenceManager;
    private CustPrograssbar custPrograssbar;
    private WalletHistoryAdapter adapter;

    private double currentBalance = 0.0;
    private static final double MIN_WITHDRAWAL_AMOUNT = 5.0;
    private static final String RAZORPAY_KEY = APIClient.RAZORPAY_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGoodsAgentWalletBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Checkout.preload(getApplicationContext());
        initViews();
        setupClickListeners();
        fetchWalletDetails();
    }

    private void initViews() {
        custPrograssbar = new CustPrograssbar();
        preferenceManager = new PreferenceManager(this);

        // Setup RecyclerView
        binding.recycleviewHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WalletHistoryAdapter(this);
        binding.recycleviewHistory.setAdapter(adapter);


    }

    private void setupClickListeners() {
        binding.btnWithdraw.setOnClickListener(v -> showWithdrawBottomSheet());
    }

    private void showWithdrawBottomSheet() {
        GoodsDriverWithdrawBottomSheet bottomSheet = new GoodsDriverWithdrawBottomSheet(currentBalance);
        bottomSheet.setWithdrawListener(this);
        bottomSheet.show(getSupportFragmentManager(), "GoodsDriverWithdrawBottomSheet");
    }

    @Override
    public void onWithdrawRequested(JSONObject withdrawalData, GoodsDriverWithdrawBottomSheet.WithdrawCallback callback) {
        // Show loading
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Processing withdrawal request...");
        progressDialog.show();

        try {
            String customerId = preferenceManager.getStringValue("customer_id");
            String fcmToken = preferenceManager.getStringValue("fcm_token");
            String driverId = preferenceManager.getStringValue("goods_driver_id");
            String driverName = preferenceManager.getStringValue("goods_driver_name");
            String driverMobileNo = preferenceManager.getStringValue("goods_driver_mobile_no");

            withdrawalData.put("contact_no", driverMobileNo);
            withdrawalData.put("driver_name", driverName);
            withdrawalData.put("driver_id", driverId);
            withdrawalData.put("auth", fcmToken);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    APIClient.baseUrl + "initiate_goods_driver_withdrawal",
                    withdrawalData,
                    response -> {
                        progressDialog.dismiss();
                        handleWithdrawalResponse(response,callback);
                    },
                    error -> {
                        progressDialog.dismiss();
                        handleWithdrawalError(error,callback);
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            request.setRetryPolicy(new DefaultRetryPolicy(
                    30000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            VolleySingleton.getInstance(this).addToRequestQueue(request);

        } catch (JSONException e) {
            callback.onFailure("Failed to process withdrawal");
            progressDialog.dismiss();
            e.printStackTrace();
            Toast.makeText(this, "Error processing request", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleWithdrawalResponse(JSONObject response, GoodsDriverWithdrawBottomSheet.WithdrawCallback callback) {
        try {
            String status = response.getString("status");
            String message = response.getString("message");

            if ("success".equals(status)) {
                callback.onSuccess();
                showSuccessDialog(message);
                fetchWalletDetails(); // Refresh wallet balance
            } else {
                callback.onFailure(message);
                showErrorDialog(message);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            showErrorDialog("Error processing response");
            callback.onFailure("Failed to process withdrawal");
        }
    }

    private void handleWithdrawalError(VolleyError error, GoodsDriverWithdrawBottomSheet.WithdrawCallback callback) {
        String errorMessage = "Request failed";
        if (error.networkResponse != null) {
            try {
                String responseBody = new String(error.networkResponse.data, "utf-8");
                JSONObject data = new JSONObject(responseBody);
                errorMessage = data.optString("message", errorMessage);
                callback.onFailure(errorMessage);
            } catch (Exception e) {
                callback.onFailure("Failed to process withdrawal");
                e.printStackTrace();
            }
        }
        showErrorDialog(errorMessage);
    }

    private void showSuccessDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Success")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
    private void fetchWalletDetails() {
        showLoading(true);
        String token = preferenceManager.getStringValue("goods_driver_token");
        String driverId = preferenceManager.getStringValue("goods_driver_id");
        JSONObject params = new JSONObject();
        try {
            params.put("driver_id", driverId);
            params.put("driver_unique_id", driverId);
            params.put("auth", token);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = APIClient.baseUrl + "goods_driver_wallet_details";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
                url,
                params,
                response -> {
                    showLoading(false);
                    try {
                        if (response.has("message")) {
                            binding.lvlNotfound.setVisibility(View.VISIBLE);
                            binding.recycleviewHistory.setVisibility(View.GONE);
                            binding.txtWallet.setText("Balance ₹0.00");
                            return;
                        }

                        if (response.has("results")) {
                            JSONObject results = response.getJSONObject("results");
                            updateWalletUI(results);
                        } else {
                            binding.lvlNotfound.setVisibility(View.VISIBLE);
                            binding.recycleviewHistory.setVisibility(View.GONE);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        showError("Failed to parse response");
                        binding.lvlNotfound.setVisibility(View.VISIBLE);
                        binding.recycleviewHistory.setVisibility(View.GONE);
                    }
                },
                error -> handleNetworkError(error, url, params)) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void handleNetworkError(Exception error, String url, JSONObject params) {
        error.printStackTrace();
        showLoading(false);

        String errorMessage;
        if (error instanceof NetworkError) {
            errorMessage = "No internet connection";
        } else if (error instanceof TimeoutError) {
            errorMessage = "Request timed out";
        } else if (error instanceof ServerError) {
            errorMessage = "Server error";
        } else if (error instanceof ParseError) {
            errorMessage = "Data parsing error";
        } else {
            errorMessage = "Network request failed";
        }

        binding.lvlNotfound.setVisibility(View.VISIBLE);
        binding.recycleviewHistory.setVisibility(View.GONE);


    }

    private void updateWalletUI(JSONObject results) throws JSONException {
        // Update wallet balance
        JSONObject walletDetails = results.getJSONObject("wallet_details");
        double balance = walletDetails.getDouble("current_balance");
        currentBalance = balance;
        binding.txtWallet.setText(String.format("Balance ₹%.2f", balance));

        // Update transaction history
        JSONArray transactions = results.getJSONArray("transaction_history");
        List<WalletTransaction> transactionList = new ArrayList<>();

        for (int i = 0; i < transactions.length(); i++) {
            JSONObject transaction = transactions.getJSONObject(i);
            String formattedDateTime = formatEpochTime(transaction.getDouble("transaction_time"));

            transactionList.add(new WalletTransaction(
                    transaction.getString("transaction_id"),
                    transaction.getString("transaction_type"),
                    transaction.getDouble("amount"),
                    transaction.getString("status"),
                    formattedDateTime,
                    transaction.getString("remarks"),
                    transaction.getString("payment_mode"),
                    transaction.getString("reference_id")
            ));
        }

        if (transactionList.isEmpty()) {
            binding.lvlNotfound.setVisibility(View.VISIBLE);
            binding.recycleviewHistory.setVisibility(View.GONE);
        } else {
            binding.lvlNotfound.setVisibility(View.GONE);
            binding.recycleviewHistory.setVisibility(View.VISIBLE);
            adapter.setTransactions(transactionList);
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            custPrograssbar.prograssCreate(this);
        } else {
            custPrograssbar.closePrograssBar();
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private static String formatEpochTime(double epochSeconds) {
        try {
            long epochMillis = (long) (epochSeconds * 1000);
            Date date = new Date(epochMillis);
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            return sdf.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return "Invalid Time";
        }
    }
}