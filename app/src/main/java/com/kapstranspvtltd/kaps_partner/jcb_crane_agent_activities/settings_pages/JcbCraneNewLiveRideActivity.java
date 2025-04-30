package com.kapstranspvtltd.kaps_partner.jcb_crane_agent_activities.settings_pages;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityJcbCraneNewLiveRideBinding;
import com.kapstranspvtltd.kaps_partner.databinding.DialogPaymentDetailsBinding;
import com.kapstranspvtltd.kaps_partner.fcm.AccessToken;
import com.kapstranspvtltd.kaps_partner.goods_driver_activities.HomeActivity;
import com.kapstranspvtltd.kaps_partner.handyman_agent_activities.settings_pages.HandyManNewLiveRideActivity;
import com.kapstranspvtltd.kaps_partner.jcb_crane_agent_activities.JcbCraneHomeActivity;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.services.FloatingWindowService;
import com.kapstranspvtltd.kaps_partner.services.LocationUpdateService;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class JcbCraneNewLiveRideActivity extends AppCompatActivity implements OnMapReadyCallback {

    private long startServiceTime = 0;
    private long elapsedTime = 0;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private TextView timerTextView;
    private BroadcastReceiver serviceRestartReceiver;
    private ActivityJcbCraneNewLiveRideBinding binding;
    private PreferenceManager preferenceManager;

    private GoogleMap mMap;
    private boolean isFromFCM;
    private int assignedBookingId;
    private String nextStatus = "";
    private String currentStatus = "";
    private boolean isLoading = false;
    private ProgressDialog progressDialog;

    String customerName = "";
    String customerID = "",customerMobileNo="";
    String pickupAddress = "";
    String distance = "";
    String totalTime = "";

    Double penaltyChargesAmount=1.0;

    String senderName = "";
    String senderNumber = "";
    Double totalPrice = 0.0;
    String bookingStatus = "";
    String receivedOTP = "";
    String bookingId = "";
    String pickupLat, pickupLng = "0.0";

    private int penaltyAmount = 0;
    private int allowedMinutes = 0; // totalTime in minutes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityJcbCraneNewLiveRideBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeViews();
        setupClickListeners();
        getCurrentBookingIdDetails();
        startLocationUpdates();
        setupTimer();
        binding.navigateBtn.setOnClickListener(v -> navigateToDestination());
    }

    private void setupTimer() {
        boolean wasServiceRunning = preferenceManager.getBooleanValue(PreferenceManager.Keys.SERVICE_IS_RUNNING, false);
        if (wasServiceRunning) {
            startServiceTime = preferenceManager.getLongValue(PreferenceManager.Keys.SERVICE_START_TIME, 0);
            elapsedTime = preferenceManager.getLongValue(PreferenceManager.Keys.SERVICE_ELAPSED_TIME, 0);
            long lastPauseTime = preferenceManager.getLongValue(PreferenceManager.Keys.SERVICE_LAST_PAUSE_TIME, 0);

            if (lastPauseTime > 0) {
                elapsedTime += (System.currentTimeMillis() - lastPauseTime);
            }
            startServiceTimer();
        }
    }

    private void navigateToDestination() {
        if (pickupLat == null || pickupLat.isEmpty() || pickupLng == null || pickupLng.isEmpty()) {
            showToast("Location coordinates not available");
            return;
        }

        // Always navigate to work location
        String uri = String.format(Locale.US, "google.navigation:q=%s,%s",
                pickupLat, pickupLng);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            showToast("Google Maps app is not installed");
            String browserUri = String.format(Locale.US,
                    "https://www.google.com/maps/dir/?api=1&destination=%s,%s",
                    pickupLat, pickupLng);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(browserUri)));
        }
    }

    private void initializeViews() {
        timerTextView = binding.txtServiceTime;
        preferenceManager = new PreferenceManager(this);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        isFromFCM = getIntent().getBooleanExtra("FromFCM", false);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (isFromFCM) {
            binding.imgBack.setVisibility(View.GONE);
        }
    }

    private void getCurrentBookingIdDetails() {
        showLoading("Fetching booking details...");

        String driverId = preferenceManager.getStringValue("jcb_crane_agent_id");

        if (driverId == null || driverId.isEmpty()) {
            showError("No Live Ride Found");
            finish();
            return;
        }

        String url = APIClient.baseUrl + "get_jcb_crane_driver_current_booking_detail";

        JSONObject params = new JSONObject();
        try {
            params.put("jcb_crane_driver_id", driverId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                params,
                response -> {
                    hideLoading();
                    try {
                        int bookingId = response.optInt("current_booking_id");
                        if(bookingId == -1){
                            preferenceManager.saveBooleanValue("isLiveRide",false);
                            showToast("No Live Orders");
                            finish();
                            return;
                        }
                        if (bookingId > 0) {
                            assignedBookingId = bookingId;
                            preferenceManager.saveBooleanValue("isLiveRide",true);
                            fetchBookingDetails();
                        } else {
                            stopLocationUpdates();
                            showError("No Live Ride Found");
                            preferenceManager.saveBooleanValue("isLiveRide",false);
                            preferenceManager.saveStringValue("current_booking_id_assigned", "");
                            finish();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        stopLocationUpdates();
                        showError("Error processing response");
                        preferenceManager.saveBooleanValue("isLiveRide",false);
                        preferenceManager.saveStringValue("current_booking_id_assigned", "");
                        finish();
                    }
                },
                error -> {
                    stopLocationUpdates();
                    hideLoading();
                    handleVolleyError(error);
                    finish();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void fetchBookingDetails() {
        showLoading("Fetching ride details...");
        String url = APIClient.baseUrl + "jcb_crane_driver_booking_details_live_track";

        JSONObject params = new JSONObject();
        try {
            params.put("booking_id", assignedBookingId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                params,
                response -> {
                    hideLoading();
                    try {
                        JSONArray results = response.getJSONArray("results");
                        if (results.length() > 0) {
                            JSONObject rideDetails = results.getJSONObject(0);
                            updateRideDetails(rideDetails);
                            showOnMap();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        showToast("Error loading ride details");
                        finish();
                    }
                },
                error -> {
                    hideLoading();
                    handleVolleyError(error);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void updateRideDetails(JSONObject rideDetails) {
        try {
            bookingId = rideDetails.getString("booking_id");
            customerName = rideDetails.getString("customer_name");
            customerID = rideDetails.getString("customer_id");
            customerMobileNo = rideDetails.getString("customer_mobile_no");
            pickupAddress = rideDetails.getString("pickup_address");
            distance = rideDetails.getString("distance");
            totalTime = rideDetails.getString("total_time");
            penaltyChargesAmount = rideDetails.getDouble("penalty_charges_amount");

            // Parse totalTime to allowedMinutes (assume format "HH:mm:ss" or "mm:ss")
            allowedMinutes = parseMinutesFromTimeString(totalTime);

            totalPrice = rideDetails.getDouble("total_price");
            bookingStatus = rideDetails.getString("booking_status");
            receivedOTP = rideDetails.getString("otp");
            pickupLat = rideDetails.getString("pickup_lat");
            pickupLng = rideDetails.getString("pickup_lng");
            String bookingTiming = rideDetails.getString("booking_timing");
            String formattedBookingTiming = getFormattedBookingTiming(bookingTiming);

            // Handle timer based on status
            if ("Start Service".equals(bookingStatus)) {
                boolean wasServiceRunning = preferenceManager.getBooleanValue(
                        PreferenceManager.Keys.SERVICE_IS_RUNNING, false);
                if (!wasServiceRunning) {
                    resetTimer();
                    startServiceTimer();
                }
            }

            // Update UI
            binding.txtCustomerName.setText(customerName);
            binding.txtPickAddress.setText(pickupAddress);
            binding.txtDistance.setText(distance + " Km");
            binding.txtTime.setText(totalTime+"Hr");
            binding.bookingTiming.setText(formattedBookingTiming);
            binding.txtTotalPrice.setText("₹" + Math.round(totalPrice));
            binding.txtBookingId.setText("#CRN" + assignedBookingId);
            binding.toolbarBookingId.setText("#CRN "+ assignedBookingId);
            binding.bookingStatus.setText(bookingStatus);

            updateNextStatus(bookingStatus);
            updateStatusButtons();

        } catch (JSONException e) {
            e.printStackTrace();
            showToast("Error updating ride details");
        }
    }

    private int parseMinutesFromTimeString(String timeStr) {
        try {
            // If timeStr is just a number (e.g., "1", "1.5", "2")
            double hours = Double.parseDouble(timeStr);
            return (int) Math.round(hours * 60);
        } catch (Exception e) {
            return 0;
        }
    }

    private void startServiceTimer() {
        if (startServiceTime == 0) {
            startServiceTime = System.currentTimeMillis();
            preferenceManager.saveLongValue(PreferenceManager.Keys.SERVICE_START_TIME, startServiceTime);
            preferenceManager.saveBooleanValue(PreferenceManager.Keys.SERVICE_IS_RUNNING, true);
            preferenceManager.saveLongValue(PreferenceManager.Keys.SERVICE_ELAPSED_TIME, elapsedTime);
        }

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
//                long totalElapsedTime = elapsedTime + (currentTime - startServiceTime);
//
//                int seconds = (int) (totalElapsedTime / 1000);
//                int minutes = seconds / 60;
//                int hours = minutes / 60;
//
//                seconds = seconds % 60;
//                minutes = minutes % 60;
//
//                String timeStr = String.format(Locale.getDefault(),
//                        "%02d:%02d:%02d", hours, minutes, seconds);
//                timerTextView.setText(timeStr);
//
//                preferenceManager.saveLongValue(PreferenceManager.Keys.SERVICE_ELAPSED_TIME, totalElapsedTime);

                /**
                 * Use this for production and comment out below fake timing
                  */
                long totalElapsedTime = elapsedTime + (currentTime - startServiceTime);

                //this is to test a fake timing
//                long fakeOffset = 2 * 60 * 60 * 1000; // 2 hours in milliseconds
//                long totalElapsedTime = elapsedTime + (currentTime - startServiceTime) + fakeOffset;

                int seconds = (int) (totalElapsedTime / 1000);
                int minutes = seconds / 60;
                int hours = minutes / 60;

                seconds = seconds % 60;
                minutes = minutes % 60;

                String timeStr = String.format(Locale.getDefault(),
                        "%02d:%02d:%02d", hours, minutes, seconds);
                timerTextView.setText(timeStr);

                preferenceManager.saveLongValue(PreferenceManager.Keys.SERVICE_ELAPSED_TIME, totalElapsedTime);

// Penalty calculation
//                penaltyChargesAmount //double precision
//                int elapsedMinutes = (int) (totalElapsedTime / 60000);
//                if (allowedMinutes > 0 && elapsedMinutes > allowedMinutes) {
//                    penaltyAmount = elapsedMinutes - allowedMinutes;
//                } else {
//                    penaltyAmount = 0;
//                }
                int elapsedMinutes = (int) (totalElapsedTime / 60000);
                if (allowedMinutes > 0 && elapsedMinutes > allowedMinutes) {
                    int extraMinutes = elapsedMinutes - allowedMinutes;
                    penaltyAmount = (int) Math.round(extraMinutes * penaltyChargesAmount);
                } else {
                    penaltyAmount = 0;
                }
                updatePenaltyUI();
                timerHandler.postDelayed(this, 1000);
            }
        };

        timerHandler.postDelayed(timerRunnable, 0);
    }

    private void updatePenaltyUI() {
        runOnUiThread(() -> {
            if (penaltyAmount > 0) {
                binding.penaltyText.setVisibility(View.VISIBLE);
                binding.penaltyText.setText("Penalty: ₹" + penaltyAmount);
            } else {
                binding.penaltyText.setVisibility(View.GONE);
            }
        });
    }

    private void stopServiceTimer() {
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
            long pauseTime = System.currentTimeMillis();
            preferenceManager.saveLongValue(PreferenceManager.Keys.SERVICE_LAST_PAUSE_TIME, pauseTime);
            preferenceManager.saveLongValue(PreferenceManager.Keys.SERVICE_ELAPSED_TIME,
                    elapsedTime + (pauseTime - startServiceTime));
        }
    }

    private void resetTimer() {
        stopServiceTimer();
        startServiceTime = 0;
        elapsedTime = 0;
        preferenceManager.removeValue(PreferenceManager.Keys.SERVICE_START_TIME);
        preferenceManager.removeValue(PreferenceManager.Keys.SERVICE_IS_RUNNING);
        preferenceManager.removeValue(PreferenceManager.Keys.SERVICE_ELAPSED_TIME);
        preferenceManager.removeValue(PreferenceManager.Keys.SERVICE_LAST_PAUSE_TIME);
    }

    private void updateNextStatus(String bookingStatus) {
        currentStatus = bookingStatus;
        switch (bookingStatus) {
            case "Driver Accepted":
                nextStatus = "Update to Arrived Location";
                break;
            case "Driver Arrived":
                nextStatus = "Verify OTP";
                break;
            case "Otp Verified":
                nextStatus = "Start Service";
                break;
            case "OTP Verified":
                nextStatus = "Start Service";
                break;
            case "Start Service":
                nextStatus = "Send Payment Details";
                break;
            case "Make Payment":
                nextStatus = "End Service";
                break;
            default:
                nextStatus = "";
                break;
        }
    }

    private void updateRideStatus(String status) {
        showLoading("Updating status...");
        String url = APIClient.baseUrl + "update_booking_status_jcb_crane_driver";
        String accessToken = AccessToken.getAccessToken();
        JSONObject params = new JSONObject();
        int baseAmount = (int) Math.round(totalPrice);
        int totalPayable = baseAmount + penaltyAmount;
        try {
            params.put("booking_id", assignedBookingId);
            params.put("booking_status", status);
            params.put("server_token", accessToken);
//            params.put("total_payment", totalPrice.toString());

            params.put("total_payment", totalPayable+"");
            params.put("penalty_amount", penaltyAmount+"");
            params.put("customer_id", customerID);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                params,
                response -> {
                    hideLoading();
                    showToast("Status updated successfully");
                    fetchBookingDetails();
                },
                error -> {
                    hideLoading();
                    handleVolleyError(error);
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

    private void processPayment(String amount, String paymentMethod) {
        String driverId = preferenceManager.getStringValue("jcb_crane_agent_id");

        new Thread(() -> {
            try {
                String accessToken = AccessToken.getAccessToken();
                if (accessToken == null || accessToken.isEmpty()) {
                    runOnUiThread(() -> {
                        hideLoading();
                        showToast("No Token Found!");
                    });
                    return;
                }

                JSONObject params = new JSONObject();
                try {
                    params.put("booking_id", bookingId);
                    params.put("payment_method", paymentMethod);
                    params.put("payment_id", -1);
                    params.put("booking_status", "End Service");
                    params.put("server_token", accessToken);
                    params.put("driver_id", driverId);
                    params.put("customer_id", customerID);
                    params.put("total_amount", Math.round(Double.parseDouble(amount)));
                    params.put("penalty_amount",penaltyAmount);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String url = APIClient.baseUrl + "generate_order_id_for_booking_id_jcb_crane_driver";

                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        params,
                        response -> {
                            handlePaymentSuccess();
                        },
                        error -> {
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

                request.setRetryPolicy(new DefaultRetryPolicy(30000,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

                VolleySingleton.getInstance(this).addToRequestQueue(request);

            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideLoading();
                    showToast("Payment Processing Failed: " + e.getMessage());
                });
            }
        }).start();
    }

    private void handlePaymentSuccess() {
        runOnUiThread(() -> {
            hideLoading();
            preferenceManager.saveStringValue("current_booking_id_assigned", "");
            resetTimer();
            preferenceManager.saveBooleanValue(PreferenceManager.Keys.SERVICE_IS_RUNNING, false);
            showToast("Trip Completed Successfully!");

            new Handler().postDelayed(() -> {
                navigateToHome();
            }, 2000);
        });
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, JcbCraneHomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ... Rest of your existing methods (showOnMap, drawRoute, etc.) remain the same
    // Just update the map logic to only show work location instead of pickup/drop

    @Override
    protected void onPause() {
        super.onPause();
        if (timerRunnable != null) {
            stopServiceTimer();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (preferenceManager.getBooleanValue(PreferenceManager.Keys.SERVICE_IS_RUNNING, false)) {
            long lastPauseTime = preferenceManager.getLongValue(PreferenceManager.Keys.SERVICE_LAST_PAUSE_TIME, 0);
            if (lastPauseTime > 0) {
                elapsedTime = preferenceManager.getLongValue(PreferenceManager.Keys.SERVICE_ELAPSED_TIME, 0);
                elapsedTime += (System.currentTimeMillis() - lastPauseTime);
            }
            startServiceTime = System.currentTimeMillis();
            startServiceTimer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopServiceTimer();
    }

    // Add these methods to your JcbCraneNewLiveRideActivity class

    private void setupClickListeners() {
        binding.imgBack.setOnClickListener(v -> onBackPressed());
        binding.imgCall.setOnClickListener(v -> handleCallClick());
        setupStatusButtonListeners();
    }

    private void handleCallClick() {
        try {
            String phoneNumber = customerMobileNo; // Since JCB/Crane only deals with customer directly

            if (phoneNumber == null || phoneNumber.isEmpty()) {
                showToast("Phone number not available");
                return;
            }

            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber));

            if (callIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(callIntent);
            } else {
                showToast("No app available to make calls");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showToast("Unable to make call");
        }
    }

    private void setupStatusButtonListeners() {
        binding.btnArrived.setOnClickListener(v -> showConfirmationDialog("Driver Arrived"));
        binding.btnVerifyOtp.setOnClickListener(v -> showOtpDialog());
        binding.btnSendTrip.setOnClickListener(v -> showConfirmationDialog("Start Service"));
        binding.btnSendPaymentDetails.setOnClickListener(v -> showConfirmationDialog("Make Payment"));
        binding.btnEndTrip.setOnClickListener(v -> showPaymentDialog());
    }

    private void showPaymentDialog() {
        String amount = totalPrice.toString();
        int baseAmount = (int) Math.round(Double.parseDouble(amount));
        int totalPayable = baseAmount + penaltyAmount;

        DialogPaymentDetailsBinding dialogBinding = DialogPaymentDetailsBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setView(dialogBinding.getRoot())
                .create();

        dialogBinding.amountValue.setText("₹" + baseAmount);
        if (penaltyAmount > 0) {
            dialogBinding.penaltyLabel.setVisibility(View.VISIBLE);
            dialogBinding.penaltyValue.setVisibility(View.VISIBLE);
            dialogBinding.totalPayableLabel.setVisibility(View.VISIBLE);
            dialogBinding.totalPayableValue.setVisibility(View.VISIBLE);
            dialogBinding.penaltyValue.setText("₹" + penaltyAmount);
        } else {
            dialogBinding.penaltyLabel.setVisibility(View.GONE);
            dialogBinding.penaltyValue.setVisibility(View.GONE);
            dialogBinding.totalPayableLabel.setVisibility(View.GONE);
            dialogBinding.totalPayableValue.setVisibility(View.GONE);
        }
        dialogBinding.totalPayableValue.setText("₹" + totalPayable);

        dialogBinding.cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialogBinding.confirmButton.setOnClickListener(v -> {
            dialog.dismiss();
            showLoading("Processing payment...");
            processPayment(String.valueOf(totalPayable), getSelectedPaymentType(dialogBinding));
        });

        dialog.show();
    }

    /*private void showPaymentDialog() {
        String amount = totalPrice.toString();
        DialogPaymentDetailsBinding dialogBinding = DialogPaymentDetailsBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setView(dialogBinding.getRoot())
                .create();

        dialogBinding.amountValue.setText("₹" + Math.round(Double.parseDouble(amount)));
        dialogBinding.cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialogBinding.confirmButton.setOnClickListener(v -> {
            dialog.dismiss();
            showLoading("Processing payment...");
            processPayment(amount, getSelectedPaymentType(dialogBinding));
        });

        dialog.show();
    }*/

    private String getSelectedPaymentType(DialogPaymentDetailsBinding dialogBinding) {
        return dialogBinding.paymentTypeGroup.getCheckedRadioButtonId() == R.id.onlineRadioButton
                ? "Online" : "Cash";
    }

    private void showOtpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_otp_verification, null);
        EditText otpInput = dialogView.findViewById(R.id.otp_input);

        builder.setView(dialogView)
                .setTitle("Verify OTP")
                .setPositiveButton("Verify", null)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String enteredOtp = otpInput.getText().toString().trim();
                if (enteredOtp.isEmpty()) {
                    otpInput.setError("Please enter OTP");
                    return;
                }

                if(!enteredOtp.equals(receivedOTP)){
                    otpInput.setError("Invalid OTP");
                    return;
                }
                dialog.dismiss();
                updateRideStatus("Otp Verified");
            });
        });

        dialog.show();
    }

    private void showConfirmationDialog(String status) {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Action")
                .setMessage("Are you sure you want to " + status + "?")
                .setPositiveButton("Yes", (dialog, which) -> updateRideStatus(status))
                .setNegativeButton("No", null)
                .show();
    }

    private void handlePaymentError(VolleyError error) {
        runOnUiThread(() -> {
            hideLoading();
            if (error.networkResponse != null && error.networkResponse.statusCode == 404) {
                showToast("Already Assigned to Another Driver");
                new Handler().postDelayed(() -> finish(), 2000);
            } else {
                showToast("Payment Processing Failed");
            }
        });
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void handleVolleyError(VolleyError error) {
        String message = "Something went wrong";
        if (error instanceof NoConnectionError) {
            message = "No internet connection";
        } else if (error instanceof TimeoutError) {
            message = "Request timed out";
        }
        showToast(message);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public String getFormattedBookingTiming(String bookingTiming) {
        try {
            double epochTime = Double.parseDouble(bookingTiming);
            long milliseconds = (long) (epochTime * 1000);
            Date date = new Date(milliseconds);
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd, hh:mm a", Locale.getDefault());
            return sdf.format(date);
        } catch (Exception e) {
            return bookingTiming;
        }
    }

    private void updateStatusButtons() {
        // Hide all buttons first
        binding.btnArrived.setVisibility(View.GONE);
        binding.btnVerifyOtp.setVisibility(View.GONE);
        binding.btnSendTrip.setVisibility(View.GONE);
        binding.btnSendPaymentDetails.setVisibility(View.GONE);
        binding.btnEndTrip.setVisibility(View.GONE);

        // Show appropriate button based on nextStatus
        switch (nextStatus) {
            case "Update to Arrived Location":
                binding.btnArrived.setVisibility(View.VISIBLE);
                break;
            case "Verify OTP":
                binding.btnVerifyOtp.setVisibility(View.VISIBLE);
                break;
            case "Start Service":
                binding.btnSendTrip.setText("Start Service");
                binding.btnSendTrip.setVisibility(View.VISIBLE);
                break;
            case "Send Payment Details":
                binding.btnSendPaymentDetails.setVisibility(View.VISIBLE);
                break;
            case "End Service":
                binding.btnEndTrip.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (isFromFCM) {
            Intent intent = new Intent(this, JcbCraneHomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        showOnMap();
    }

    private void showOnMap() {
        if (mMap == null || pickupLat == null || pickupLat.isEmpty() || pickupLng == null || pickupLng.isEmpty()) {
            return;
        }

        mMap.clear();

        // Convert work location coordinates
        LatLng workLocation = new LatLng(Double.parseDouble(pickupLat), Double.parseDouble(pickupLng));

        // Add work location marker
        mMap.addMarker(new MarkerOptions()
                .position(workLocation)
                .title("Work Location")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_current_long)));

        // Move camera to work location
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(workLocation, 15f));

        // Get and show driver's current location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (lastKnownLocation != null) {
                LatLng driverLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());

                // Draw route from driver to work location
                drawRoute(driverLocation, workLocation);

                // Adjust map bounds to show both locations
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(driverLocation);
                builder.include(workLocation);
                LatLngBounds bounds = builder.build();

                int padding = 100;
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            }
        }
    }

    private void drawRoute(LatLng origin, LatLng destination) {
        String url = getDirectionsUrl(origin, destination);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray routes = response.getJSONArray("routes");
                        JSONObject route = routes.getJSONObject(0);
                        JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                        String encodedPath = overviewPolyline.getString("points");

                        List<LatLng> decodedPath = decodePolyline(encodedPath);

                        PolylineOptions polylineOptions = new PolylineOptions()
                                .addAll(decodedPath)
                                .width(8)
                                .color(getResources().getColor(R.color.colorPrimary))
                                .geodesic(true);

                        mMap.addPolyline(polylineOptions);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Log.e("Maps", "Direction API Error: " + error.getMessage())
        );

        VolleySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest);
    }

    private String getDirectionsUrl(LatLng origin, LatLng destination) {
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        String str_dest = "destination=" + destination.latitude + "," + destination.longitude;
        String sensor = "sensor=false";
        String mode = "mode=driving";
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mode + "&key=" + getString(R.string.google_maps_key);
        String output = "json";
        return "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
    }

    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((double) lat / 1E5, (double) lng / 1E5);
            poly.add(p);
        }
        return poly;
    }

    private void showLoading(String message) {
        if (!isFinishing()) {
            progressDialog.setMessage(message);
            progressDialog.show();
        }
    }

    private void hideLoading() {
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void startLocationUpdates() {
        // Implementation for location updates if needed
    }

    private void stopLocationUpdates() {
        // Implementation for stopping location updates if needed
    }


}