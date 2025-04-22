package com.kapstranspvtltd.kaps_partner.goods_driver_activities.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

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
import com.kapstranspvtltd.kaps_partner.adapters.OrdersAdapter;
import com.kapstranspvtltd.kaps_partner.models.OrderModel;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.databinding.FragmentEarningsBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EarningsFragment extends Fragment {
    private static final String TAG = "EarningsFragment";

    private FragmentEarningsBinding binding;
    private List<Double> monthlyEarnings;
    private double monthlyEarningsTotal;
    private List<OrderModel> allOrdersList;
    private boolean isLoading;
    private boolean noOrdersFound;
    private PreferenceManager preferenceManager;

    public EarningsFragment() {
        // Required empty public constructor
        monthlyEarnings = new ArrayList<>();
        allOrdersList = new ArrayList<>();
        isLoading = true;
        noOrdersFound = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentEarningsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        preferenceManager = new PreferenceManager(requireContext());
        setupUI();
        fetchWholeYearsEarnings();
        fetchAllOrders();
    }

    private void setupUI() {
        binding.toolbar.setTitle("My Earnings");
        binding.toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
    }

    private void fetchWholeYearsEarnings() {
        showLoading();

        JSONObject jsonObject = new JSONObject();
        try {
            String goodsDriverId = getDriverId();
            System.out.println("goodsDriverId::" + goodsDriverId);
            jsonObject.put("driver_id", goodsDriverId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                APIClient.baseUrl + "goods_driver_whole_year_earnings",
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

        if (getContext() != null) {
            VolleySingleton.getInstance(getContext()).addToRequestQueue(request);
        }
    }

    private void fetchAllOrders() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("driver_id", getDriverId());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                APIClient.baseUrl + "goods_driver_all_orders",
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

        if (getContext() != null) {
            VolleySingleton.getInstance(getContext()).addToRequestQueue(request);
        }
    }

    private void updateEarningsUI() {
        if (getActivity() == null) return;

        binding.earningsChart.setData(generateBarChartData());
        binding.earningsChart.invalidate();
        binding.totalEarningsText.setText("₹" + Math.round(monthlyEarningsTotal) + " /-");
    }

    private void updateOrdersUI() {
        if (getActivity() == null) return;

        if (noOrdersFound) {
            binding.ordersRecyclerView.setVisibility(View.GONE);
            binding.noOrdersText.setVisibility(View.VISIBLE);
        } else {
            binding.ordersRecyclerView.setVisibility(View.VISIBLE);
            OrdersAdapter adapter = new OrdersAdapter(allOrdersList);
            binding.ordersRecyclerView.setAdapter(adapter);
            binding.ordersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            binding.noOrdersText.setVisibility(View.GONE);
        }
    }

    private BarData generateBarChartData() {
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < monthlyEarnings.size(); i++) {
            entries.add(new BarEntry(i, monthlyEarnings.get(i).floatValue()));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Monthly Earnings");
        if (getContext() != null) {
            dataSet.setColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        }
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);

        return new BarData(dataSet);
    }

    private double calculateMonthlyEarnings(JSONArray results) throws Exception {
        double total = 0.0;
        for (int i = 0; i < results.length(); i++) {
            total += results.getJSONObject(i).getDouble("total_earnings");
        }
        return total;
    }

    private String getDriverId() {
        return preferenceManager.getStringValue("goods_driver_id");
    }

    private void handleError(VolleyError error) {
        String message;
        if (error instanceof NoConnectionError) {
            message = "No internet connection";
        } else if (error instanceof TimeoutError) {
            message = "Request timed out";
        } else if (error instanceof ServerError) {
            message = "Server error";
        } else {
            message = "An error occurred";
        }
        showError(message);
    }

    private void showError(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
        }
    }

    private void showLoading() {
        binding.progressBar.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        binding.progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}