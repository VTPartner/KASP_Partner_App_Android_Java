package com.kapstranspvtltd.kaps_partner.jcb_crane_agent_activities.settings_pages;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.snackbar.Snackbar;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.adapters.RidesAdapter;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityJcbCraneAllRidesBinding;
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

public class JcbCraneAllRidesActivity extends AppCompatActivity {

    private ActivityJcbCraneAllRidesBinding binding;
    private boolean isLoading = true;
    private boolean noOrdersFound = true;
    private List<OrderRidesModel> ordersList = new ArrayList<>();
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityJcbCraneAllRidesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(this);
        setupUI();
        setupRecyclerView();
        fetchAllOrders();
    }

    private void setupUI() {
//        Toolbar toolbar = binding.toolbar;
//        toolbar.setTitle("My Jcb/Crane Services");
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

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("driver_id", getDriverId());
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

    private String getDriverId() {
        return preferenceManager.getStringValue("jcb_crane_agent_id");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}