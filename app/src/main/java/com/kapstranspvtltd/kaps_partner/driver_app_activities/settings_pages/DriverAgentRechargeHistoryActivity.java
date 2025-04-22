package com.kapstranspvtltd.kaps_partner.driver_app_activities.settings_pages;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.view.View;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.Request;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.snackbar.Snackbar;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.adapters.RechargeHistoryAdapter;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityDriverAgentRechargeHistoryBinding;
import com.kapstranspvtltd.kaps_partner.models.RechargeHistory;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverAgentRechargeHistoryActivity extends AppCompatActivity {

    private ActivityDriverAgentRechargeHistoryBinding binding;
    private RechargeHistoryAdapter adapter;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDriverAgentRechargeHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeViews();
        fetchRechargeHistory();
    }

    private void initializeViews() {
        preferenceManager = new PreferenceManager(this);

        // Setup toolbar
//        setSupportActionBar(binding.toolbar);
//        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Setup RecyclerView
        adapter = new RechargeHistoryAdapter(this);
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Setup SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener(this::fetchRechargeHistory);

        // Set color scheme for SwipeRefreshLayout
        binding.swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.colorAccent
        );
    }

    private void fetchRechargeHistory() {
        showLoading(true);

        try {
            JSONObject params = new JSONObject();
            params.put("driver_id", preferenceManager.getStringValue("other_driver_id"));

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    APIClient.baseUrl + "get_other_driver_new_recharge_plan_history_list",
                    params,
                    response -> {
                        showLoading(false);
                        handleResponse(response);
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

        } catch (Exception e) {
            showLoading(false);
            showError("Error fetching recharge history");
        }
    }

    private void handleResponse(JSONObject response) {
        try {
            JSONArray results = response.optJSONArray("results");
            List<RechargeHistory> historyList = new ArrayList<>();

            if (results != null && results.length() > 0) {
                for (int i = 0; i < results.length(); i++) {
                    JSONObject historyJson = results.getJSONObject(i);
                    historyList.add(new RechargeHistory(historyJson));
                }
            }

            updateUI(historyList);

        } catch (JSONException e) {
            showError("Error parsing response");
        }
    }

    private void updateUI(List<RechargeHistory> historyList) {
        if (historyList.isEmpty()) {
            binding.emptyStateLayout.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.GONE);
        } else {
            binding.emptyStateLayout.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.VISIBLE);
            adapter.setHistoryList(historyList);
        }
    }

    private void handleError(VolleyError error) {
        String message;
        // Check if there's a network response
        if (error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;

            switch (statusCode) {

                case 404:
                    message = "No active recharge plan found";
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
        updateUI(new ArrayList<>());
    }



    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.swipeRefreshLayout.setRefreshing(false);
    }

    private void showError(String message) {
        Snackbar.make(binding.coordinatorLayout, message, Snackbar.LENGTH_LONG)
                .setAction("Retry", v -> fetchRechargeHistory())
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}