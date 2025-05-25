package com.kapstranspvtltd.kaps_partner.jcb_crane_agent_activities.settings_pages;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.Request;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.adapters.RechargePlansAdapter;

import com.kapstranspvtltd.kaps_partner.databinding.ActivityJcbCraneRechargeHomeBinding;
import com.kapstranspvtltd.kaps_partner.goods_driver_activities.HomeActivity;
import com.kapstranspvtltd.kaps_partner.jcb_crane_agent_activities.JcbCraneHomeActivity;
import com.kapstranspvtltd.kaps_partner.models.RechargePlan;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;

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
import java.util.concurrent.TimeUnit;

public class JcbCraneRechargeHomeActivity extends AppCompatActivity implements PaymentResultListener {
    private ActivityJcbCraneRechargeHomeBinding binding;
    private List<RechargePlan> plansList = new ArrayList<>();
    private static final String TAG = "RazorpayPayment";
    private boolean planStatus = true;

    private String remainingTimeText = "";
    private RechargePlan selectedPlan;
    private RechargePlansAdapter adapter;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityJcbCraneRechargeHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(this);
        setupToolbar();
        setupCurrentPlan();
        setupRecyclerView();
        fetchCurrentPlanDetails();
        fetchRechargePlans();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void fetchCurrentPlanDetails() {
        showLoading(true);
        String token = preferenceManager.getStringValue("jcb_crane_token");
        String driverId = preferenceManager.getStringValue("jcb_crane_agent_id");
        try {
            JSONObject params = new JSONObject();
            params.put("driver_id", preferenceManager.getStringValue("jcb_crane_agent_id"));
            params.put("driver_unique_id", driverId);
            params.put("auth", token);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    APIClient.baseUrl + "jcb_crane_current_new_recharge_details",
                    params,
                    response -> {
                        showLoading(false);
                        handleCurrentPlanResponse(response);
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
            showError("Error fetching plan details");
        }
    }

    private void handleError(VolleyError error) {
        String message;
        if (error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;

            switch (statusCode) {

                case 404:
                    showNoPlanUI();
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

    private void handleCurrentPlanResponse(JSONObject response) {
        try {
            JSONArray results = response.optJSONArray("results");
            if (results != null && results.length() > 0) {
                JSONObject planDetails = results.getJSONObject(0);
                updateCurrentPlanUI(planDetails);
            } else {
                showNoPlanUI();
            }
        } catch (JSONException e) {
            Log.e("RechargeHome", "Error parsing response: " + e.getMessage());
            showError("Error loading plan details");
        }
    }

    private void updateCurrentPlanUI(JSONObject planDetails) {
        try {
            // Update plan title
            String planTitle = planDetails.getString("plan_title");
            binding.currentPlanTitle.setText(planTitle);

            // Update plan description if you have a TextView for it
            String planDescription = planDetails.getString("plan_description");
            // binding.currentPlanDescription.setText(planDescription);

            // Update plan price
            double planPrice = planDetails.getDouble("plan_price");
            binding.planPrice.setText(String.format(Locale.US, "₹%.2f", planPrice));

            // Parse and format expiry time
            String expiryTime = planDetails.getString("expiry_time");
            updateExpiryStatus(expiryTime);

        } catch (JSONException e) {
            Log.e("RechargeHome", "Error updating UI: " + e.getMessage());
            showError("Error displaying plan details");
        }
    }

    private void updateExpiryStatus(String expiryTimeStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US);

            Date expiryDate = inputFormat.parse(expiryTimeStr);
            Date currentDate = new Date();

            boolean isExpired = currentDate.after(expiryDate);

            if (isExpired) {
                planStatus = false;
                binding.currentPlanValidity.setText("Plan Expired");
                binding.currentPlanValidity.setTextColor(getResources().getColor(R.color.colorerror));
            } else {
                // Calculate remaining time
                long diffInMillis = expiryDate.getTime() - currentDate.getTime();
                String remainingTime = formatRemainingTime(diffInMillis);
                remainingTimeText = remainingTime;
                binding.currentPlanValidity.setText(String.format("Valid till: %s\n%s",
                        outputFormat.format(expiryDate),
                        remainingTime));
                binding.currentPlanValidity.setTextColor(getResources().getColor(R.color.green));
            }
        } catch (ParseException e) {
            Log.e("RechargeHome", "Error parsing date: " + e.getMessage());
            binding.currentPlanValidity.setText("Expiry: " + expiryTimeStr);
        }
    }

    private String formatRemainingTime(long milliseconds) {
        long days = TimeUnit.MILLISECONDS.toDays(milliseconds);
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds) % 24;

        if (days > 0) {
            return String.format(Locale.US, "%d days %d hours remaining", days, hours);
        } else if (hours > 0) {
            return String.format(Locale.US, "%d hours remaining", hours);
        } else {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60;
            return String.format(Locale.US, "%d minutes remaining", minutes);
        }
    }

    private void showNoPlanUI() {
        planStatus = false;
        // Update UI to show no active plan
        binding.currentPlanTitle.setText("No Active Plan");
        binding.currentPlanValidity.setText("Purchase a plan to continue");
        binding.planPrice.setText("₹0.00");

        // Optionally show a button to purchase plan
        // showPurchasePlanButton();
    }

    private void showPurchasePlanButton() {
        // If you have a purchase button in your layout
        if (binding.btnPurchasePlan != null) {
            binding.btnPurchasePlan.setVisibility(View.VISIBLE);
            binding.btnPurchasePlan.setOnClickListener(v -> {
                // Navigate to purchase plan screen
                // startActivity(new Intent(this, PurchasePlanActivity.class));
            });
        }
    }

    private void setupCurrentPlan() {
        // Set dummy values for now
        binding.currentPlanTitle.setText("Premium Plan");
        binding.currentPlanValidity.setText("Valid till: 20 Apr 2024");
        binding.planPrice.setText("Balance: ₹500");
    }

    private void setupRecyclerView() {
        adapter = new RechargePlansAdapter(plansList, plan -> {
            selectedPlan = plan;
            if (planStatus == false)
                startRazorpayPayment(plan);
            else
                showError("Please recharge after " + remainingTimeText);
        });
        binding.plansRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.plansRecyclerView.setAdapter(adapter);
    }

    private void fetchRechargePlans() {
        binding.progressBar.setVisibility(View.VISIBLE);

        JSONObject params = new JSONObject();
        try {
            params.put("category_id", 3); // Set your category ID
            params.put("vehicle_id", 1); // Set your category ID
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                APIClient.baseUrl + "get_goods_driver_new_recharge_plans_list",
                params,
                response -> {
                    binding.progressBar.setVisibility(View.GONE);
                    try {
                        JSONArray results = response.getJSONArray("results");

                        plansList.clear();
                        for (int i = 0; i < results.length(); i++) {
                            plansList.add(RechargePlan.fromJson(results.getJSONObject(i)));
                        }
                        adapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        showError("Error loading plans");
                    }
                },
                error -> {
                    binding.progressBar.setVisibility(View.GONE);
                    showError("Error loading plans");
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void startRazorpayPayment(RechargePlan plan) {
        Checkout checkout = new Checkout();
        checkout.setKeyID(APIClient.RAZORPAY_ID);


        try {
            JSONObject options = new JSONObject();
            options.put("name", "KAPS TRANS PRIVATE LIMITED");
            options.put("description", plan.getTitle());
            options.put("currency", "INR");
            options.put("amount", (int) (plan.getPrice() * 100)); // Convert to paise
            options.put("prefill.email", preferenceManager.getStringValue("email"));
            options.put("prefill.contact", preferenceManager.getStringValue("mobile"));

            checkout.open(this, options);
        } catch (Exception e) {
            showError("Error starting payment");
        }
    }

    private void showError(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }


    @Override
    public void onPaymentSuccess(String razorpayPaymentID) {
        try {
            showLoading(true);

            // Calculate expiry time with proper format
            String expiryTime = calculateExpiryDate(Integer.parseInt(selectedPlan.getExpiryDays()));

            String token = preferenceManager.getStringValue("jcb_crane_token");
            String driverId = preferenceManager.getStringValue("jcb_crane_agent_id");

            JSONObject params = new JSONObject();
            try {
                params.put("razorpay_payment_id", razorpayPaymentID);
                params.put("plan_id", selectedPlan.getPlanId());
                params.put("driver_id", preferenceManager.getStringValue("jcb_crane_agent_id"));
                params.put("amount", String.format(Locale.US, "%.2f", selectedPlan.getPrice()));
                params.put("expiry_time", expiryTime);
                params.put("driver_unique_id", driverId);
                params.put("auth", token);
            } catch (JSONException e) {
                Log.e("Payment", "Error creating params: " + e.getMessage());
                showError("Error processing payment parameters");
                return;
            }

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    APIClient.baseUrl + "jcb_crane_driver_new_recharge_plan",
                    params,
                    response -> {
                        showLoading(false);
                        handlePaymentResponse(response, razorpayPaymentID);
                    },
                    error -> {
                        showLoading(false);
                        handlePaymentError(error);
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            // Set retry policy
            request.setRetryPolicy(new DefaultRetryPolicy(
                    30000, // 30 seconds timeout
                    0,     // no retries
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            VolleySingleton.getInstance(this).addToRequestQueue(request);

        } catch (Exception e) {
            showLoading(false);
            Log.e("Payment", "General error: " + e.getMessage());
            showError("Error processing payment. Please try again.");
        }
    }

    private void handlePaymentResponse(JSONObject response, String razorpayPaymentID) {
        try {
            if (response.getBoolean("success")) {
                // Get recharge history ID from response
                long rechargeHistoryId = response.optLong("recharge_history_id", 0);

                // Save recharge details to preferences
//                saveRechargeDetails(rechargeHistoryId, razorpayPaymentID);

                // Show success dialog
                showSuccessDialog(razorpayPaymentID);

                // Update UI with new plan details
                updatePlanUI();

                // Log success
                logPaymentSuccess(razorpayPaymentID, selectedPlan);


            } else {
                String message = response.optString("message", "Payment verification failed");
                showError(message);
                logPaymentError("Verification Failed", message);
            }
        } catch (JSONException e) {
            Log.e("Payment", "Response parsing error: " + e.getMessage());
            showError("Error processing payment response");
        }
    }

    private void updatePlanUI() {
        // Update current plan details in UI
        binding.currentPlanTitle.setText(selectedPlan.getTitle());
//        binding.currentPlanPrice.setText(String.format(Locale.US, "₹%.2f", selectedPlan.getPrice()));
        binding.currentPlanValidity.setText("Valid for " + selectedPlan.getValidityDays() + " days");

    }

    private void handlePostPaymentNavigation() {
        Intent intent = new Intent(this, JcbCraneHomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

    }

    // Helper method to handle payment errors
    private void handlePaymentError(VolleyError error) {
        String errorMessage;
        if (error instanceof NetworkError) {
            errorMessage = "Network error occurred. Please check your connection.";
        } else if (error instanceof TimeoutError) {
            errorMessage = "Request timed out. Please try again.";
        } else if (error instanceof ServerError) {
            errorMessage = "Server error occurred. Please try again later.";
        } else {
            errorMessage = "Error verifying payment. Please contact support.";
        }
        showError(errorMessage);
        logPaymentError("API Error", error.getMessage());
    }

    @Override
    public void onPaymentError(int code, String description) {
        showLoading(false);

        // Log the error details
        Log.e(TAG, "Payment failed: code: " + code + " description: " + description);

        // Show appropriate error message based on error code
        String errorMessage;
        switch (code) {
            case Checkout.NETWORK_ERROR:
                errorMessage = "Network error occurred. Please check your internet connection.";
                break;
            case Checkout.INVALID_OPTIONS:
                errorMessage = "Invalid payment options provided.";
                break;
            case Checkout.PAYMENT_CANCELED:
                errorMessage = "Payment was cancelled.";
                break;
            case Checkout.TLS_ERROR:
                errorMessage = "Security error occurred. Please try again.";
                break;
            default:
                errorMessage = "Payment failed: " + description;
                break;
        }

        // Show error dialog
        showErrorDialog(errorMessage);

        // Track analytics
        logPaymentError("Razorpay Error", "Code: " + code + " Description: " + description);
    }

    // Helper methods
    private void showLoading(boolean show) {
        if (show) {
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    private void showSuccessDialog(String razorpayPaymentID) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Payment Successful")
                .setMessage("Your plan has been successfully activated!\nYour payment ID is " + razorpayPaymentID)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    // Finish activity or navigate as needed
                    handlePostPaymentNavigation();
                })
                .setCancelable(false)
                .show();
    }

    private void showErrorDialog(String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Payment Failed")
                .setMessage(message)
                .setPositiveButton("Try Again", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void updateCurrentPlanDetails(RechargePlan plan) {
        // Update UI with new plan details
        binding.currentPlanTitle.setText(plan.getTitle());
        binding.currentPlanValidity.setText(String.format("Valid for %d days", plan.getValidityDays()));
//        binding.currentPlanExpiry.setText(String.format("Expires in %d days", plan.getExpiryDays()));

        // Save plan details to preferences if needed
//        preferenceManager.putString("current_plan_id", plan.getPlanId());
//        preferenceManager.putString("current_plan_expiry", calculateExpiryDate(plan.getExpiryDays()));
    }

    //    private String calculateExpiryDate(String expiryDays) {
//        int expiryDaysRet = Integer.parseInt(expiryDays);
//        Calendar calendar = Calendar.getInstance();
//        calendar.add(Calendar.DAY_OF_YEAR, expiryDaysRet);
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//        return sdf.format(calendar.getTime());
//    }
    private String calculateExpiryDate(int expiryDays) {
        // Get current date and time
        Calendar calendar = Calendar.getInstance();

        // Calculate total hours (expiryDays * 24 hours per day)
        int expiryHours = expiryDays * 24;

        // Add the calculated hours to current time
        calendar.add(Calendar.HOUR_OF_DAY, expiryHours);

        // Format the date with time including AM/PM
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault());

        try {
            // Get formatted date-time string
            String expiryDateTime = sdf.format(calendar.getTime());

            // Log for debugging
            Log.d("ExpiryCalculation", String.format(
                    "Current Time: %s, Expiry Days: %d, Expiry Hours: %d, Expiry Time: %s",
                    sdf.format(Calendar.getInstance().getTime()),
                    expiryDays,
                    expiryHours,
                    expiryDateTime
            ));

            return expiryDateTime;
        } catch (Exception e) {
            Log.e("ExpiryCalculation", "Error calculating expiry date: " + e.getMessage());
            return sdf.format(Calendar.getInstance().getTime()); // Return current time in case of error
        }
    }

    // Helper method to get remaining time until expiry
    public String getRemainingTime(String expiryDateTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault());
            Date expiryDate = sdf.parse(expiryDateTime);
            Date currentDate = new Date();

            // Calculate difference in milliseconds
            long diffInMillis = expiryDate.getTime() - currentDate.getTime();

            if (diffInMillis <= 0) {
                return "Expired";
            }

            // Convert to hours and minutes
            long hours = diffInMillis / (60 * 60 * 1000);
            long minutes = (diffInMillis % (60 * 60 * 1000)) / (60 * 1000);

            if (hours >= 24) {
                long days = hours / 24;
                hours = hours % 24;
                return String.format(Locale.getDefault(),
                        "%d days %d hours %d minutes remaining",
                        days, hours, minutes);
            } else {
                return String.format(Locale.getDefault(),
                        "%d hours %d minutes remaining",
                        hours, minutes);
            }

        } catch (Exception e) {
            Log.e("ExpiryCalculation", "Error calculating remaining time: " + e.getMessage());
            return "Unable to calculate remaining time";
        }
    }

    // Usage example in your activity:
//    private void updateCurrentPlanDetails(RechargePlan plan) {
//        String expiryDateTime = calculateExpiryDate(plan.getExpiryDays());
//
//        // Update UI
//        binding.tvCurrentPlanTitle.setText(plan.getTitle());
//        binding.tvCurrentPlanValidity.setText(String.format("Valid for %d days", plan.getValidityDays()));
//        binding.tvCurrentPlanExpiry.setText(expiryDateTime);
//
//        // Add a countdown timer to update remaining time
//        startExpiryCountdown(expiryDateTime);
//
//        // Save to preferences
//        preferenceManager.putString("current_plan_id", plan.getPlanId());
//        preferenceManager.putString("current_plan_expiry", expiryDateTime);
//    }

    // Countdown timer to update remaining time
    private CountDownTimer expiryTimer;

//    private void startExpiryCountdown(String expiryDateTime) {
//        if (expiryTimer != null) {
//            expiryTimer.cancel();
//        }
//
//        try {
//            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault());
//            Date expiryDate = sdf.parse(expiryDateTime);
//            long millisUntilExpiry = expiryDate.getTime() - System.currentTimeMillis();
//
//            if (millisUntilExpiry > 0) {
//                expiryTimer = new CountDownTimer(millisUntilExpiry, 60000) { // Update every minute
//                    @Override
//                    public void onTick(long millisUntilFinished) {
//                        String remainingTime = getRemainingTime(expiryDateTime);
//                        binding.tvRemainingTime.setText(remainingTime);
//                    }
//
//                    @Override
//                    public void onFinish() {
//                        binding.tvRemainingTime.setText("Plan Expired");
//                        // Handle plan expiry (e.g., show dialog, disable features)
//                        handlePlanExpiry();
//                    }
//                }.start();
//            } else {
//                binding.tvRemainingTime.setText("Plan Expired");
//                handlePlanExpiry();
//            }
//        } catch (Exception e) {
//            Log.e("ExpiryTimer", "Error starting countdown: " + e.getMessage());
//        }
//    }

    private void handlePlanExpiry() {
        // Show expiry dialog
        new MaterialAlertDialogBuilder(this)
                .setTitle("Plan Expired")
                .setMessage("Your recharge plan has expired. Please recharge to continue using our services.")
                .setPositiveButton("Recharge Now", (dialog, which) -> {
                    // Navigate to recharge plans
                    // You can add your navigation logic here
                })
                .setNegativeButton("Later", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (expiryTimer != null) {
            expiryTimer.cancel();
        }
    }

    private void logPaymentSuccess(String paymentId, RechargePlan plan) {
        Log.d(TAG, String.format("Payment Success - ID: %s, Plan: %s, Amount: %.2f",
                paymentId, plan.getTitle(), plan.getPrice()));
        // Add your analytics tracking here
    }

    private void logPaymentError(String type, String message) {
        Log.e(TAG, String.format("Payment Error - Type: %s, Message: %s", type, message));
        // Add your analytics tracking here
    }
}