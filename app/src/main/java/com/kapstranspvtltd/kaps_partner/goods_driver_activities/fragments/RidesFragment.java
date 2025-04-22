package com.kapstranspvtltd.kaps_partner.goods_driver_activities.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.snackbar.Snackbar;
import com.kapstranspvtltd.kaps_partner.adapters.RidesAdapter;
import com.kapstranspvtltd.kaps_partner.models.OrderRidesModel;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;
import com.kapstranspvtltd.kaps_partner.databinding.FragmentRidesBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RidesFragment extends Fragment {

    private FragmentRidesBinding binding;
    private boolean isLoading = true;
    private boolean noOrdersFound = true;
    private List<OrderRidesModel> ordersList = new ArrayList<>();
    private PreferenceManager preferenceManager;
    private RidesAdapter ridesAdapter;
    public RidesFragment() {
        // Required empty public constructor
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentRidesBinding.inflate(inflater, container, false);

        preferenceManager = new PreferenceManager(getActivity());
        setupUI();
        setupRecyclerView();
        fetchAllOrders();
        return binding.getRoot();
    }

    private void setupUI() {
        Toolbar toolbar = binding.toolbar;
        toolbar.setTitle("My Rides");





    }

    private void setupRecyclerView() {
        binding.ridesRecyclerView.setLayoutManager(
                new LinearLayoutManager(getActivity())
        );

        ridesAdapter = new RidesAdapter(ordersList, order -> {
            // Handle ride item click
            // Intent intent = new Intent(GoodsDriverRidesActivity.this,
            //     GoodsDriverRideDetailsActivity.class);
            // intent.putExtra("order", order);
            // startActivity(intent);
        });

        binding.ridesRecyclerView.setAdapter(ridesAdapter);
    }

    private void fetchAllOrders() {
        isLoading = true;
        binding.progressBar.setVisibility(View.VISIBLE);
        ordersList.clear();

        JSONObject params = new JSONObject();
        try {
            params.put("driver_id", getDriverId());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = APIClient.baseUrl + "goods_driver_all_orders";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                params,
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

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        VolleySingleton.getInstance(getActivity()).addToRequestQueue(request);
    }

    private void updateUI() {
        if (noOrdersFound) {
            binding.ridesRecyclerView.setVisibility(View.GONE);
            //binding.emptyView.setVisibility(View.VISIBLE);
        } else {
            binding.ridesRecyclerView.setVisibility(View.VISIBLE);
            //binding.emptyView.setVisibility(View.GONE);
            ridesAdapter.notifyDataSetChanged();
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
        return preferenceManager.getStringValue("goods_driver_id");
    }


}