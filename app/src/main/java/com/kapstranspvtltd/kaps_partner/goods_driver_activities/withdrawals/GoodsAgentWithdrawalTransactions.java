package com.kapstranspvtltd.kaps_partner.goods_driver_activities.withdrawals;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.Request;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityGoodsAgentWithdrawalTransactionsBinding;
import com.kapstranspvtltd.kaps_partner.goods_driver_activities.withdrawals.adapters.WithdrawalTransactionsAdapter;
import com.kapstranspvtltd.kaps_partner.goods_driver_activities.withdrawals.models.WithdrawalTransaction;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.utils.CustPrograssbar;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoodsAgentWithdrawalTransactions extends AppCompatActivity {
    private ActivityGoodsAgentWithdrawalTransactionsBinding binding;
    private PreferenceManager preferenceManager;
    private CustPrograssbar custPrograssbar;
    private WithdrawalTransactionsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGoodsAgentWithdrawalTransactionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initViews();
        fetchPayoutDetails();
    }

    private void initViews() {
        preferenceManager = new PreferenceManager(this);
        custPrograssbar = new CustPrograssbar();

        // Setup RecyclerView
        binding.recycleviewHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WithdrawalTransactionsAdapter(this);
        binding.recycleviewHistory.setAdapter(adapter);
    }

    private void fetchPayoutDetails() {
        showLoading(true);

        String driverId = preferenceManager.getStringValue("goods_driver_id");
        JSONObject params = new JSONObject();
        try {
            params.put("driver_id", driverId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                APIClient.baseUrl + "get_goods_driver_payouts",
                params,
                response -> {
                    showLoading(false);
                    handlePayoutResponse(response);
                },
                error -> {
                    showLoading(false);
                    handleError(error);
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
    }

    private void handlePayoutResponse(JSONObject response) {
        try {
            if (response.getString("status").equals("success")) {
                double currentBalance = response.getDouble("current_balance");
                binding.txtWallet.setText(String.format("Balance ₹%.2f", currentBalance));

                JSONArray payouts = response.getJSONArray("payouts");
                List<WithdrawalTransaction> payoutList = new ArrayList<>();

                for (int i = 0; i < payouts.length(); i++) {
                    try {
                        JSONObject payout = payouts.getJSONObject(i);
                        WithdrawalTransaction transaction = WithdrawalTransaction.fromJson(payout);
                        payoutList.add(transaction);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        // Continue with next item if one fails
                    }
                }

                if (payoutList.isEmpty()) {
                    binding.lvlNotfound.setVisibility(View.VISIBLE);
                    binding.recycleviewHistory.setVisibility(View.GONE);
                } else {
                    binding.lvlNotfound.setVisibility(View.GONE);
                    binding.recycleviewHistory.setVisibility(View.VISIBLE);
                    adapter.setTransactions(payoutList);
                }
            } else {
                showError(response.optString("message", "Failed to fetch withdrawals"));
                binding.lvlNotfound.setVisibility(View.VISIBLE);
                binding.recycleviewHistory.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to parse response");
            binding.lvlNotfound.setVisibility(View.VISIBLE);
            binding.recycleviewHistory.setVisibility(View.GONE);
        }
    }

    private void handleError(VolleyError error) {
        String message;
        if (error instanceof NetworkError) {
            message = "No internet connection";
        } else if (error instanceof TimeoutError) {
            message = "Request timed out";
        } else if (error instanceof ServerError) {
            message = "Server error";
        } else {
            message = "Network request failed";
        }
        showError(message);
        binding.lvlNotfound.setVisibility(View.VISIBLE);
        binding.recycleviewHistory.setVisibility(View.GONE);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (custPrograssbar != null) {
            custPrograssbar.closePrograssBar();
        }
    }
}