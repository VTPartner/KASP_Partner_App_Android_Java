package com.kapstranspvtltd.kaps_partner.cab_driver_activities.settings_pages;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.common_activities.adapters.WalletHistoryAdapter;
import com.kapstranspvtltd.kaps_partner.common_activities.models.WalletTransaction;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityCabAgentWalletBinding;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.utils.CustPrograssbar;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;

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

public class CabAgentWalletActivity extends AppCompatActivity implements PaymentResultListener {

    private ActivityCabAgentWalletBinding binding;
    private PreferenceManager preferenceManager;
    private CustPrograssbar custPrograssbar;
    private WalletHistoryAdapter adapter;
    private static final double MIN_WITHDRAWAL_AMOUNT = 5.0;
    private static final String RAZORPAY_KEY = APIClient.RAZORPAY_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCabAgentWalletBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Checkout.preload(getApplicationContext());
        initViews();
        fetchWalletDetails();
    }

    private void initViews() {
        custPrograssbar = new CustPrograssbar();
        preferenceManager = new PreferenceManager(this);

        binding.recycleviewHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WalletHistoryAdapter(this);
        binding.recycleviewHistory.setAdapter(adapter);

        setupWithdrawButton();
    }

    private void setupWithdrawButton() {
        binding.btnWithdraw.setOnClickListener(v -> {
            double currentBalance = getCurrentBalance();
            if (currentBalance < MIN_WITHDRAWAL_AMOUNT) {
                showError("Minimum withdrawal amount is ₹" + MIN_WITHDRAWAL_AMOUNT);
                return;
            }
            showWithdrawalDialog(currentBalance);
        });
    }

    private double getCurrentBalance() {
        try {
            String balanceText = binding.txtWallet.getText().toString()
                    .replace("Balance ₹", "")
                    .trim();
            return Double.parseDouble(balanceText);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void fetchWalletDetails() {
        showLoading(true);
        String token = preferenceManager.getStringValue("cab_driver_token");
        String driverId = preferenceManager.getStringValue("cab_driver_id");

        JSONObject params = new JSONObject();
        try {
            params.put("driver_id", driverId);
            params.put("driver_unique_id", driverId);
            params.put("auth", token);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = APIClient.baseUrl + "cab_driver_wallet_details";

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

        request.setRetryPolicy(new DefaultRetryPolicy(30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void handleNetworkError(Exception error, String url, JSONObject params) {
        error.printStackTrace();
        showLoading(false);

        String errorMessage;
        if (error instanceof NetworkError) errorMessage = "No internet connection";
        else if (error instanceof TimeoutError) errorMessage = "Request timed out";
        else if (error instanceof ServerError) errorMessage = "Server error";
        else if (error instanceof ParseError) errorMessage = "Data parsing error";
        else errorMessage = "Network request failed";

        showError(errorMessage);
        binding.lvlNotfound.setVisibility(View.VISIBLE);
        binding.recycleviewHistory.setVisibility(View.GONE);


    }

    private void showWithdrawalDialog(double maxAmount) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_withdrawal_bottom_sheet, null);
        dialog.setContentView(view);

        EditText etAmount = view.findViewById(R.id.etAmount);
        TextView tvAvailableBalance = view.findViewById(R.id.tvAvailableBalance);
        TextView btnProceed = view.findViewById(R.id.btnProceed);

        tvAvailableBalance.setText(String.format("Available Balance: ₹%.2f", maxAmount));

        btnProceed.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString();
            if (amountStr.isEmpty()) {
                showError("Please enter amount");
                return;
            }

            double amount = Double.parseDouble(amountStr);
            if (amount < MIN_WITHDRAWAL_AMOUNT) {
                showError("Minimum withdrawal amount is ₹" + MIN_WITHDRAWAL_AMOUNT);
                return;
            }

            if (amount > maxAmount) {
                showError("Amount exceeds available balance");
                return;
            }

            dialog.dismiss();
            initiateWithdrawal(amount);
        });

        dialog.show();
    }

    private void initiateWithdrawal(double amount) {
        showLoading(true);
        String driverId = preferenceManager.getStringValue("cab_driver_id");
        String token = preferenceManager.getStringValue("cab_driver_token");

        JSONObject params = new JSONObject();
        try {
            params.put("driver_id", driverId);
            params.put("auth", token);
            params.put("driver_unique_id", driverId);
            params.put("amount", amount);
            params.put("transaction_type", "WITHDRAWAL");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
                APIClient.baseUrl + "initiate_cab_withdrawal",
                params,
                response -> {
                    showLoading(false);
                    try {
                        if (response.has("order_id")) {
                            String orderId = response.getString("order_id");
                            startRazorpayWithdrawal(orderId, amount);
                        } else {
                            showError("Failed to initiate withdrawal");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        showError("Failed to process withdrawal");
                    }
                },
                error -> {
                    showLoading(false);
                    showError("Withdrawal request failed");
                });

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void startRazorpayWithdrawal(String orderId, double amount) {
        Checkout checkout = new Checkout();
        checkout.setKeyID(RAZORPAY_KEY);

        try {
            JSONObject options = new JSONObject();
            options.put("name", "KAPS TRANS PRIVATE LIMITED");
            options.put("description", "Wallet Withdrawal");
            options.put("order_id", orderId);
            options.put("currency", "INR");
            options.put("amount", amount * 100);

            JSONObject prefill = new JSONObject();
            prefill.put("contact", preferenceManager.getStringValue("mobile"));
            options.put("prefill", prefill);

            checkout.open(this, options);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to start withdrawal process");
        }
    }

    @Override
    public void onPaymentSuccess(String razorpayPaymentId) {
        updateWithdrawalStatus(razorpayPaymentId, "SUCCESS");
    }

    @Override
    public void onPaymentError(int code, String description) {
        updateWithdrawalStatus(null, "FAILED");
    }

    private void updateWithdrawalStatus(String paymentId, String status) {
        // TODO: Make API call to update withdrawal status
        fetchWalletDetails(); // Refresh wallet details
    }

    private void updateWalletUI(JSONObject results) throws JSONException {
        JSONObject walletDetails = results.getJSONObject("wallet_details");
        double balance = walletDetails.getDouble("current_balance");
        binding.txtWallet.setText(String.format("Balance ₹%.2f", balance));

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
        if (show) custPrograssbar.prograssCreate(this);
        else custPrograssbar.closePrograssBar();
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