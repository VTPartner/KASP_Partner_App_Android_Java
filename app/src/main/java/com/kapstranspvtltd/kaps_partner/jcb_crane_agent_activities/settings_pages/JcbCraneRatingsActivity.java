package com.kapstranspvtltd.kaps_partner.jcb_crane_agent_activities.settings_pages;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.android.volley.NetworkError;
import com.android.volley.Request;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.snackbar.Snackbar;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.adapters.RatingsAdapter;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityJcbCraneratingsBinding;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityMyRatingsBinding;
import com.kapstranspvtltd.kaps_partner.models.OrderRatingModel;
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

public class JcbCraneRatingsActivity extends AppCompatActivity {

    private ActivityJcbCraneratingsBinding binding;
    private boolean isLoading = true;
    private boolean noOrdersFound = true;
    private List<OrderRatingModel> ordersList;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityJcbCraneratingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeVariables();
        setupUI();
        setupRecyclerView();
        fetchAllOrders();
    }

    private void initializeVariables() {
        ordersList = new ArrayList<>();
        preferenceManager = new PreferenceManager(this);
    }

    private void setupUI() {
//        setSupportActionBar(binding.toolbar);
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setTitle("My Ratings");
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        }
//        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(new RatingsAdapter(ordersList));
    }

    private void fetchAllOrders() {
        isLoading = true;
        ordersList.clear();
        binding.progressBar.setVisibility(View.VISIBLE);
        String token = preferenceManager.getStringValue("jcb_crane_token");
        String driverId = preferenceManager.getStringValue("jcb_crane_agent_id");

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("driver_id", driverId);
            jsonObject.put("driver_unique_id", driverId);
            jsonObject.put("auth", token);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                APIClient.baseUrl + "jcb_crane_driver_all_orders",
                jsonObject,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        ordersList.clear();

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject orderJson = results.getJSONObject(i);
                            ordersList.add(OrderRatingModel.fromJson(orderJson));
                        }

                        noOrdersFound = ordersList.isEmpty();
                        updateUI();
                    } catch (Exception e) {
                        handleError(e);
                    } finally {
                        isLoading = false;
                        binding.progressBar.setVisibility(View.GONE);
                    }
                },
                error -> {
                    handleErrorServer(error);
                    isLoading = false;
                    binding.progressBar.setVisibility(View.GONE);
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

    private void updateUI() {
        if (noOrdersFound) {
            binding.recyclerView.setVisibility(View.GONE);
            binding.noOrdersText.setVisibility(View.VISIBLE);
        } else {
            binding.recyclerView.setVisibility(View.VISIBLE);
            binding.noOrdersText.setVisibility(View.GONE);
            ((RatingsAdapter) binding.recyclerView.getAdapter()).notifyDataSetChanged();
        }
    }

    private String getDriverId() {
        return preferenceManager.getStringValue("jcb_crane_driver_id");
    }

    private void handleError(Exception error) {
        String message;
        if (error.getMessage() != null && error.getMessage().contains("No Data Found")) {
            noOrdersFound = true;
            message = "No Ratings Found";
        } else {
            message = "An error occurred";
        }
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }


    private void handleErrorServer(VolleyError error) {
        String message;
        // Check if there's a network response
        if (error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;

            switch (statusCode) {

                case 404:
                    message = "You have not yet completed any rides";
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
}