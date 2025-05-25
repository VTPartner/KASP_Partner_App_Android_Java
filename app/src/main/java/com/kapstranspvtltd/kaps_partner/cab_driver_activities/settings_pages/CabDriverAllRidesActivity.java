package com.kapstranspvtltd.kaps_partner.cab_driver_activities.settings_pages;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import com.kapstranspvtltd.kaps_partner.adapters.RidesAdapter;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityCabDriverAllRidesBinding;
import com.kapstranspvtltd.kaps_partner.models.OrderRidesModel;
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

public class CabDriverAllRidesActivity extends AppCompatActivity {

    private ActivityCabDriverAllRidesBinding binding;
    private boolean isLoading = true;
    private boolean noOrdersFound = true;
    private List<OrderRidesModel> ordersList = new ArrayList<>();
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCabDriverAllRidesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(this);
        setupUI();
        setupRecyclerView();
        fetchAllOrders();
    }

    private void setupUI() {
//        Toolbar toolbar = binding.toolbar;
//        toolbar.setTitle("My Cab Rides");
//        setSupportActionBar(toolbar);
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        }
//        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        binding.ridesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        RidesAdapter adapter = new RidesAdapter(ordersList, order -> {
            // Handle ride item click
            // Intent intent = new Intent(this, GoodsDriverRideDetailsActivity.class);
            // intent.putExtra("order", order);
            // startActivity(intent);
        });
        binding.ridesRecyclerView.setAdapter(adapter);
    }

    private void fetchAllOrders() {
        isLoading = true;
        binding.progressBar.setVisibility(View.VISIBLE);
        ordersList.clear();

        String driverId = preferenceManager.getStringValue("cab_driver_id");
        String token = preferenceManager.getStringValue("cab_driver_token");

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("driver_id", getDriverId());
            jsonObject.put("driver_unique_id", driverId);
            jsonObject.put("auth", token);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                APIClient.baseUrl + "cab_driver_all_orders",
                jsonObject,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        ordersList.clear();

                        for (int i = 0; i < results.length(); i++) {
                            ordersList.add(OrderRidesModel.fromJson(results.getJSONObject(i)));
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
                    handleServerError(error);
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
            binding.ridesRecyclerView.setVisibility(View.GONE);
            // binding.emptyView.setVisibility(View.VISIBLE);
        } else {
            binding.ridesRecyclerView.setVisibility(View.VISIBLE);
            // binding.emptyView.setVisibility(View.GONE);
            RidesAdapter adapter = (RidesAdapter) binding.ridesRecyclerView.getAdapter();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void handleError(Exception error) {
        String message;
        if (error.getMessage() != null && error.getMessage().contains("No Data Found")) {
            noOrdersFound = true;
            message = "No Rides Found";
        } else {
            message = "An error occurred";
        }
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }

    private void handleServerError(VolleyError error) {
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

    private String getDriverId() {
        return preferenceManager.getStringValue("cab_driver_id");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}