package com.kapstranspvtltd.kaps_partner.driver_app_activities.settings_pages;

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
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.adapters.RatingsAdapter;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityDriverAgentRatingsBinding;
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

public class DriverAgentRatingsActivity extends AppCompatActivity {

    private ActivityDriverAgentRatingsBinding binding;
    private boolean isLoading = true;
    private boolean noOrdersFound = true;
    private List<OrderRatingModel> ordersList;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDriverAgentRatingsBinding.inflate(getLayoutInflater());
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

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("driver_id", getDriverId());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                APIClient.baseUrl + "other_driver_all_orders",
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
                        e.printStackTrace();
                        Toast.makeText(this, "Something Went Wrong", Toast.LENGTH_SHORT).show();
                    } finally {
                        isLoading = false;
                        binding.progressBar.setVisibility(View.GONE);
                    }
                },
                error -> {
                    handleError(error);
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
        return preferenceManager.getStringValue("other_driver_id");
    }

    private void handleError(VolleyError error) {
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