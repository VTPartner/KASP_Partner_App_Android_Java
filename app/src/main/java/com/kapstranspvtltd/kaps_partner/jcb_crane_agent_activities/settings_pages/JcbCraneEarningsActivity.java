package com.kapstranspvtltd.kaps_partner.jcb_crane_agent_activities.settings_pages;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.android.material.snackbar.Snackbar;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.adapters.OrdersAdapter;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityJcbCraneEarningsBinding;
import com.kapstranspvtltd.kaps_partner.models.OrderModel;
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

public class JcbCraneEarningsActivity extends AppCompatActivity {

    private ActivityJcbCraneEarningsBinding binding;
    private List<Double> monthlyEarnings;
    private double monthlyEarningsTotal;
    private List<OrderModel> allOrdersList;
    private boolean isLoading;
    private boolean noOrdersFound;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityJcbCraneEarningsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeVariables();
        setupUI();
        fetchWholeYearsEarnings();
        fetchAllOrders();
    }

    private void initializeVariables() {
        monthlyEarnings = new ArrayList<>();
        allOrdersList = new ArrayList<>();
        isLoading = true;
        noOrdersFound = true;
        preferenceManager = new PreferenceManager(this);
    }

    private void setupUI() {
//        setSupportActionBar(binding.toolbar);
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setTitle("My Earnings");
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        }
//        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void fetchWholeYearsEarnings() {
        showLoading();

        String driverId = preferenceManager.getStringValue("jcb_crane_agent_id");
        String token = preferenceManager.getStringValue("jcb_crane_token");

        JSONObject jsonObject = new JSONObject();
        try {
            String goodsDriverId = getDriverId();
            System.out.println("goodsDriverId::" + goodsDriverId);
            jsonObject.put("driver_id", goodsDriverId);
            jsonObject.put("driver_unique_id", driverId);
            jsonObject.put("auth", token);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                APIClient.baseUrl + "jcb_crane_driver_whole_year_earnings",
                jsonObject,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        monthlyEarningsTotal = calculateMonthlyEarnings(results);
                        monthlyEarnings.clear();

                        for (int i = 0; i < results.length(); i++) {
                            double earning = results.getJSONObject(i).getDouble("total_earnings");
                            monthlyEarnings.add(earning);
                        }

                        updateEarningsUI();
                    } catch (Exception e) {
                        showError("Error parsing earnings data");
                    }
                    hideLoading();
                },
                error -> {
                    handleError(error);
                    hideLoading();
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

    private void fetchAllOrders() {
        String driverId = preferenceManager.getStringValue("jcb_crane_agent_id");
        String token = preferenceManager.getStringValue("jcb_crane_token");

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
                APIClient.baseUrl + "jcb_crane_driver_all_orders",
                jsonObject,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        allOrdersList.clear();

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject orderJson = results.getJSONObject(i);
                            OrderModel order = new OrderModel(
                                    orderJson.getString("customer_name"),
                                    orderJson.getString("booking_date"),
                                    orderJson.getString("total_price")
                            );
                            allOrdersList.add(order);
                        }

                        noOrdersFound = allOrdersList.isEmpty();
                        updateOrdersUI();
                    } catch (Exception e) {
                        showError("Error parsing orders data");
                    }
                    isLoading = false;
                },
                error -> {
                    handleError(error);
                    isLoading = false;
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

    private void updateEarningsUI() {
        BarData barData = generateBarChartData();
        binding.earningsChart.setData(barData);
        binding.earningsChart.invalidate();
        binding.totalEarningsText.setText("₹" + Math.round(monthlyEarningsTotal) + " /-");
    }

    private void updateOrdersUI() {
        if (noOrdersFound) {
            binding.ordersRecyclerView.setVisibility(View.GONE);
            binding.noOrdersText.setVisibility(View.VISIBLE);
        } else {
            binding.ordersRecyclerView.setVisibility(View.VISIBLE);
            OrdersAdapter adapter = new OrdersAdapter(allOrdersList);
            binding.ordersRecyclerView.setAdapter(adapter);
            binding.ordersRecyclerView.setLayoutManager(
                    new LinearLayoutManager(this)
            );
            binding.noOrdersText.setVisibility(View.GONE);
        }
    }

    private BarData generateBarChartData() {
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < monthlyEarnings.size(); i++) {
            entries.add(new BarEntry(i, monthlyEarnings.get(i).floatValue()));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Monthly Earnings");
        dataSet.setColor(ContextCompat.getColor(this, R.color.colorPrimary));
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);

        return new BarData(dataSet);
    }

    private double calculateMonthlyEarnings(JSONArray results) throws JSONException {
        double total = 0;
        for (int i = 0; i < results.length(); i++) {
            total += results.getJSONObject(i).getDouble("total_earnings");
        }
        return total;
    }

    private String getDriverId() {
        return preferenceManager.getStringValue("jcb_crane_agent_id");
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
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }

    private void showLoading() {
        binding.progressBar.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        binding.progressBar.setVisibility(View.GONE);
    }
}