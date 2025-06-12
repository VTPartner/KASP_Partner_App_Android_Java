package com.kapstranspvtltd.kaps_partner.goods_driver_activities;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

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
import com.kapstranspvtltd.kaps_partner.adapters.CalendarAdapter;
import com.kapstranspvtltd.kaps_partner.adapters.DateChipAdapter;
import com.kapstranspvtltd.kaps_partner.adapters.EarningsSummary;
import com.kapstranspvtltd.kaps_partner.adapters.OrdersAdapter;
import com.kapstranspvtltd.kaps_partner.adapters.TripsAdapter;
import com.kapstranspvtltd.kaps_partner.common_activities.models.CalendarDay;
import com.kapstranspvtltd.kaps_partner.models.OrderModel;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityMyEarningsBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class MyEarningsActivity extends AppCompatActivity {

    private ActivityMyEarningsBinding binding;
    private List<Double> monthlyEarnings;
    private double monthlyEarningsTotal;
    private List<OrderModel> allOrdersList;
    private boolean isLoading;
    private boolean noOrdersFound;
    private PreferenceManager preferenceManager;

    private List<OrderModel> ordersList;
    private SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private EarningsSummary todaySummary;
    private EarningsSummary weeklySummary;

    private CalendarAdapter calendarAdapter;
    private List<CalendarDay> calendarDays = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyEarningsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeVariables();
//        setupUI();
//        fetchWholeYearsEarnings();
//        fetchAllOrders();
//        setupDateSelection();

        setupCalendarView();
        setupBackButton();
        fetchTodayOrders();
        fetchWeeklySummary();

    }

    private void setupBackButton() {
        binding.backButton.setOnClickListener(v -> onBackPressed());
    }

    private void setupCalendarView() {
        // Generate last 7 days
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            calendarDays.add(new CalendarDay(
                    dayFormat.format(calendar.getTime()),
                    calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.getTime()
            ));
            calendar.add(Calendar.DAY_OF_MONTH, -1);
        }

        // Setup RecyclerView
        binding.calendarRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        calendarAdapter = new CalendarAdapter(calendarDays, (day, position) -> {
            // Handle day selection

            SimpleDateFormat currentDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String currentDate = currentDateFormat.format(calendar.getTime());

            String selectedDate = apiDateFormat.format(day.getDate()); // use the same format for both

            if (selectedDate.equals(currentDate)) {
                binding.todaySummaryTxt.setText("Today's Summary");
            } else {
                binding.todaySummaryTxt.setText(selectedDate + " Summary");
            }

            fetchOrders(selectedDate, selectedDate, summary -> {
                todaySummary = summary;
                updateTodaySummary();
            });
        });

        binding.calendarRecyclerView.setAdapter(calendarAdapter);
    }


    private void initializeVariables() {
        ordersList = new ArrayList<>();
        monthlyEarnings = new ArrayList<>();
        allOrdersList = new ArrayList<>();
        isLoading = true;
        noOrdersFound = true;
        preferenceManager = new PreferenceManager(this);
    }





    private void fetchWeeklySummary() {
        Calendar calendar = Calendar.getInstance();
        String endDate = apiDateFormat.format(calendar.getTime());
        calendar.add(Calendar.DAY_OF_WEEK, -7);
        String startDate = apiDateFormat.format(calendar.getTime());

        fetchOrders(startDate, endDate, summary -> {
            weeklySummary = summary;
            updateWeeklySummary();
        });
    }

    private void fetchOrders(String startDate, String endDate, Consumer<EarningsSummary> callback) {

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("driver_id", preferenceManager.getStringValue("goods_driver_id"));
            requestBody.put("driver_unique_id", preferenceManager.getStringValue("goods_driver_id"));
            requestBody.put("auth", preferenceManager.getStringValue("goods_driver_token"));
            requestBody.put("start_date", startDate);
            requestBody.put("end_date", endDate);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                APIClient.baseUrl + "goods_driver_earning_orders",
                requestBody,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        double totalEarnings = response.getDouble("total_earnings");
                        String timeSpent = response.getString("time_spent");
                        String weeklyTimeSpent = response.getString("weekly_time_spent");
                        int totalOrders = response.getInt("total_orders");

                        List<OrderModel> orders = parseOrders(results);
                        EarningsSummary summary = new EarningsSummary(
                                totalEarnings,
                                timeSpent,
                                weeklyTimeSpent,
                                totalOrders,
                                orders
                        );

                        callback.accept(summary);
                    } catch (JSONException e) {
                        showError("Error parsing response");
                    }
//                    hideLoading();
                },
                error -> {
                    handleError(error);
//                    hideLoading();
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void updateWeeklySummary() {
        if (weeklySummary == null) return;

        binding.weeklyEarnings.setText(String.format("₹%.0f", weeklySummary.getEarnings()));
        binding.weeklyTime.setText(weeklySummary.getWeeklyTimeSpent());  // Use weekly time spent
        binding.weeklyTrips.setText(String.valueOf(weeklySummary.getTripsCount()));
    }

    private void updateTodaySummary() {
        if (todaySummary == null) return;

        binding.todayEarnings.setText(String.format("₹%.0f", todaySummary.getEarnings()));
        binding.todayTime.setText(todaySummary.getTimeSpent());
        binding.todayTrips.setText(String.valueOf(todaySummary.getTripsCount()));

        // Setup trips RecyclerView
        TripsAdapter tripsAdapter = new TripsAdapter(todaySummary.getOrders());
        binding.tripsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.tripsRecyclerView.setAdapter(tripsAdapter);
    }

    private List<OrderModel> parseOrders(JSONArray results) throws JSONException {
        List<OrderModel> orders = new ArrayList<>();
        for (int i = 0; i < results.length(); i++) {
            JSONObject order = results.getJSONObject(i);
            orders.add(new OrderModel(
                    order.getString("customer_name"),
                    order.getString("booking_date"),
                    order.getString("total_price"),
                    order.getDouble("booking_timing")
            ));
        }
        return orders;
    }
    private void fetchTodayOrders() {
        // Get current date in yyyy-MM-dd format
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDate = apiDateFormat.format(calendar.getTime());

        System.out.println("Current Date: " + currentDate); // Debug log

        fetchOrders(currentDate, currentDate, summary -> {
            todaySummary = summary;
            updateTodaySummary();
        });

        // Update calendar view to select current date
        if (calendarAdapter != null) {
            for (int i = 0; i < calendarDays.size(); i++) {
                if (apiDateFormat.format(calendarDays.get(i).getDate()).equals(currentDate)) {
                    calendarDays.get(i).setSelected(true);
                    calendarAdapter.notifyItemChanged(i);
                    break;
                }
            }
        }
    }




//    private void updateEarningsUI() {
//        BarData barData = generateBarChartData();
//        binding.earningsChart.setData(barData);
//        binding.earningsChart.invalidate();
//        binding.totalEarningsText.setText("₹" + Math.round(monthlyEarningsTotal) + " /-");
//    }



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
        return preferenceManager.getStringValue("goods_driver_id");
    }

//    private void handleError(VolleyError error) {
//        String message;
//        if (error instanceof NoConnectionError) {
//            message = "No internet connection";
//        } else if (error instanceof TimeoutError) {
//            message = "Request timed out";
//        } else if (error instanceof ServerError) {
//            message = "Server error";
//        } else {
//            message = "An error occurred";
//        }
//        showError(message);
//    }

    private void handleError(VolleyError error) {
        String message;
        // Check if there's a network response
        if (error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;

            switch (statusCode) {

                case 404:
                    message = "No Rides done on this day";
//                    updateOrdersUI();
                    break;
                case 400:
                    message = "Bad request";
                    break;
                case 500:
                    message = "Server error";
                    break;
                default:
                    message = "Error ";
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


}