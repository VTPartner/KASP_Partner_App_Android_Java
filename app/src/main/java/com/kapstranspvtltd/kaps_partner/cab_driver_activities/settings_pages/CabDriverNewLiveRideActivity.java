package com.kapstranspvtltd.kaps_partner.cab_driver_activities.settings_pages;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.kapstranspvtltd.kaps_partner.cab_driver_activities.CabDriverHomeActivity;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityCabDriverNewLiveRideBinding;
import com.kapstranspvtltd.kaps_partner.databinding.DialogPaymentDetailsBinding;
import com.kapstranspvtltd.kaps_partner.fcm.AccessToken;
import com.kapstranspvtltd.kaps_partner.goods_driver_activities.HomeActivity;
import com.kapstranspvtltd.kaps_partner.goods_driver_activities.NewLiveRideActivity;
import com.kapstranspvtltd.kaps_partner.goods_driver_activities.helper.UnloadingTimerManager;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.services.CabLocationUpdateService;
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

public class CabDriverNewLiveRideActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "CabDriverNewLiveRideActivity";
    private BroadcastReceiver serviceRestartReceiver;
    private ActivityCabDriverNewLiveRideBinding binding;
    private PreferenceManager preferenceManager;

    private GoogleMap mMap;


    private boolean isFromFCM;

    private int assignedBookingId;

    private String nextStatus = "";
    private String currentStatus = "";
    private boolean isLoading = false;
    private ProgressDialog progressDialog;

    String customerName = "",customerMobileNo="";

    String customerID = "";
    String pickupAddress = "";
    String dropAddress = "";
    String distance = "";
    String totalTime = "";
//    String senderName = "";
//    String senderNumber = "";
//    String receiverName = "";
//    String receiverNumber = "";
    Double totalPrice = 0.0;
    String bookingStatus = "";
    String receivedOTP = "";

    String bookingId = "";

    String pickupLat,pickupLng,destinationLat,destinationLng ="0.0";

    int minimumWaitingTime = 0;
    double penaltyCharges = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCabDriverNewLiveRideBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeViews();
        setupClickListeners();
        getCurrentBookingIdDetails();
        startLocationUpdates();

        binding.navigateBtn.setOnClickListener(v -> navigateToDestination());


    }

    private void navigateToDestination() {
        if(pickupLat == null || pickupLng.isEmpty() || destinationLat == null || destinationLng.isEmpty()){
            return;
        }
        LatLng destination;

        // Determine destination based on booking status
        if ("Start Trip".equals(bookingStatus)) {
            destination = new LatLng(
                    Double.parseDouble(destinationLat),
                    Double.parseDouble(destinationLng)
            );
        } else {
            destination = new LatLng(
                    Double.parseDouble(pickupLat),
                    Double.parseDouble(pickupLng)
            );
        }

        // Open Google Maps navigation
        String uri = String.format(
                Locale.US,
                "google.navigation:q=%f,%f",
                destination.latitude,
                destination.longitude
        );

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");

        // Optionally check if Google Maps is installed
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Google Maps app is not installed", Toast.LENGTH_SHORT).show();
            // Optionally open in browser as fallback
            String browserUri = String.format(
                    Locale.US,
                    "https://www.google.com/maps/dir/?api=1&destination=%f,%f",
                    destination.latitude,
                    destination.longitude
            );
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(browserUri)));
        }
    }

    private void initializeViews() {
        preferenceManager = new PreferenceManager(this);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        // Get intent extras
        isFromFCM = getIntent().getBooleanExtra("FromFCM", false);

        // Setup map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Setup toolbar
        if (isFromFCM) {
            binding.imgBack.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        binding.imgBack.setOnClickListener(v -> onBackPressed());
        binding.imgCall.setOnClickListener(v -> handleCallClick());

        // Setup status button clicks based on nextStatus
        setupStatusButtonListeners();
    }

    private void handleCallClick() {
        try {
            String phoneNumber = customerMobileNo;


            if (phoneNumber == null || phoneNumber.isEmpty()) {
                showToast("Phone number not available");
                return;
            }

            // Create the intent to make a call
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber));

            // Check if there's an app that can handle this intent
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
        binding.btnSendTrip.setOnClickListener(v -> showConfirmationDialog("Start Trip"));
        binding.btnSendPaymentDetails.setOnClickListener(v -> showConfirmationDialog("Make Payment"));
        binding.btnEndTrip.setOnClickListener(v -> showPaymentDialog());
    }

    private void showPaymentDialog() {
        String amount = totalPrice.toString();
        // Inflate dialog layout
        DialogPaymentDetailsBinding dialogBinding = DialogPaymentDetailsBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setView(dialogBinding.getRoot())
                .create();

        // Set amount
        dialogBinding.amountValue.setText("₹" + Math.round(Double.parseDouble(amount)));

        // Setup click listeners
        dialogBinding.cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialogBinding.confirmButton.setOnClickListener(v -> {
            dialog.dismiss();
            showLoading("Processing payment...");
            processPayment(amount, getSelectedPaymentType(dialogBinding));
        });

        dialog.show();
    }

    private String getSelectedPaymentType(DialogPaymentDetailsBinding dialogBinding) {
        return dialogBinding.paymentTypeGroup.getCheckedRadioButtonId() == R.id.onlineRadioButton
                ? "Online" : "Cash";
    }

    private void processPayment(String amount, String paymentMethod) {
        // Get required data

        String driverId = preferenceManager.getStringValue("cab_driver_id");
        String token = preferenceManager.getStringValue("cab_driver_token");

        // Get access token asynchronously
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

                // Create request body
                JSONObject params = new JSONObject();
                try {
                    params.put("booking_id", bookingId);
                    params.put("payment_method", paymentMethod);
                    params.put("payment_id", -1);
                    params.put("booking_status", "End Trip");
                    params.put("server_token", accessToken);
                    params.put("driver_id", driverId);
                    params.put("customer_id", customerID);
                    params.put("total_amount", Math.round(Double.parseDouble(amount)));
                    params.put("driver_unique_id", driverId);
                    params.put("auth", token);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Log.d("EndTrip", "Request: " + params.toString());

                // Make API request
                String url = APIClient.baseUrl + "generate_order_id_for_booking_id_cab_driver";

                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        params,
                        response -> {
                            Log.d("EndTrip", "Success Response: " + response);
                            handlePaymentSuccess();
                        },
                        error -> {
                            Log.e("EndTrip", "Error: " + error.getMessage());
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

                request.setRetryPolicy(new DefaultRetryPolicy(
                        30000,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                ));

                VolleySingleton.getInstance(this).addToRequestQueue(request);

            } catch (Exception e) {
                Log.e("EndTrip", "Error in payment processing", e);
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
            preferenceManager.saveStringValue("cab_current_booking_id_assigned", "");

            showToast("Trip Completed Successfully!");

            // Delay before navigation
            new Handler().postDelayed(() -> {
                navigateToHome();
            }, 2000);
        });
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

    private void navigateToHome() {
        Intent intent = new Intent(this, CabDriverHomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void getCurrentBookingIdDetails() {
        showLoading("Fetching booking details...");

        String driverId = preferenceManager.getStringValue("cab_driver_id");
        String token = preferenceManager.getStringValue("cab_driver_token");

        if (driverId == null || driverId.isEmpty()) {

            showError("No Live Ride Found");
            finish();
            return;
        }

        String url = APIClient.baseUrl + "get_cab_driver_current_booking_detail";

        JSONObject params = new JSONObject();
        try {
            params.put("cab_driver_id", driverId);
            params.put("driver_unique_id", driverId);
            params.put("auth", token);
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
                        System.out.println("bookingId::"+bookingId);
                        if(bookingId == -1){
                            preferenceManager.saveBooleanValue("isLiveRide",false);
                            stopLocationUpdates();
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
                            preferenceManager.saveStringValue("cab_current_booking_id_assigned", "");
                            finish();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        stopLocationUpdates();
                        showError("Error processing response");
                        preferenceManager.saveBooleanValue("isLiveRide",false);
                        preferenceManager.saveStringValue("cab_current_booking_id_assigned", "");
                        finish();
                    }
                },
                error -> {
                    stopLocationUpdates();
                    hideLoading();
                    error.printStackTrace();
                    if (error.networkResponse != null && error.networkResponse.statusCode == 404) {
                        showError("No Booking Details Found");
                        preferenceManager.saveBooleanValue("isLiveRide",false);
                        preferenceManager.saveStringValue("cab_current_booking_id_assigned", "");
                    } else {
                        showError("Error fetching booking details");
                        preferenceManager.saveStringValue("cab_current_booking_id_assigned", "");
                    }
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

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void fetchBookingDetails() {
        System.out.println("Fetching ride details...");
        showLoading("Fetching ride details...");
        String driverId = preferenceManager.getStringValue("cab_driver_id");
        String token = preferenceManager.getStringValue("cab_driver_token");

        String url = APIClient.baseUrl + "cab_booking_details_live_track";

        JSONObject params = new JSONObject();
        try {
            params.put("booking_id", assignedBookingId);
            params.put("driver_unique_id", driverId);
            params.put("auth", token);
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

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private UnloadingTimerManager timerManager;

    private void updateRideDetails(JSONObject rideDetails) {
        try {
            bookingId = rideDetails.getString("booking_id");
            customerName = rideDetails.getString("customer_name");
            customerID = rideDetails.getString("customer_id");
            customerMobileNo = rideDetails.getString("customer_mobile_no");
            pickupAddress = rideDetails.getString("pickup_address");
            dropAddress = rideDetails.getString("drop_address");
            distance = rideDetails.getString("distance");
            totalTime = rideDetails.getString("total_time");

//            senderName = rideDetails.getString("sender_name");
//            senderNumber = rideDetails.getString("sender_number");
//            receiverName = rideDetails.getString("receiver_name");
//            receiverNumber = rideDetails.getString("receiver_number");
            totalPrice = rideDetails.getDouble("total_price");
            bookingStatus = rideDetails.getString("booking_status");
            receivedOTP = rideDetails.getString("otp");
            pickupLat = rideDetails.getString("pickup_lat");
            pickupLng = rideDetails.getString("pickup_lng");
            destinationLat = rideDetails.getString("destination_lat");
            destinationLng = rideDetails.getString("destination_lng");
            String bookingTiming = rideDetails.getString("booking_timing");
            String formattedBookingTiming = getFormattedBookingTiming(bookingTiming);

            if (bookingStatus.equalsIgnoreCase("Driver Arrived")) {
                // Initialize timer
                timerManager = new UnloadingTimerManager(this, assignedBookingId,
                        binding.txtUnloadingTime, binding.txtPenaltyInfo,
                        new UnloadingTimerManager.UnloadingTimerListener() {
                            @Override
                            public void onPenaltyUpdated(double totalPenalty, long penaltyMinutes) {
                                // Store penalty for API update
                                penaltyCharges = totalPenalty;
                            }

                            @Override
                            public void onTimerFinished() {
                                showToast("Free unloading time finished!");
                            }
                        });

                binding.timerContainer.setVisibility(View.VISIBLE);
                timerManager.startTimer(minimumWaitingTime, penaltyCharges);
            } else if (bookingStatus.equalsIgnoreCase("Start Trip")) {
                if (timerManager != null) {
                    double finalPenalty = timerManager.getCurrentPenalty();
                    if (finalPenalty > 0) {
                        penaltyCharges = finalPenalty;
                        //TODO: Update penalty amount in backend
//                        updatePenaltyAmount(finalPenalty);
                    }
                    timerManager.stopTimer();
                }
                binding.timerContainer.setVisibility(View.GONE);
            }


            // Update UI
            binding.txtCustomerName.setText(customerName);
            binding.txtPickAddress.setText(pickupAddress);
            binding.txtDropAddress.setText(dropAddress);
            binding.txtDistance.setText(distance + " Km");
            binding.txtTime.setText(totalTime);
            binding.bookingTiming.setText(formattedBookingTiming);
            binding.txtTotalPrice.setText("₹" + Math.round(totalPrice));
            binding.txtBookingId.setText("#CRN" + assignedBookingId);
            binding.toolbarBookingId.setText("#CRN "+ assignedBookingId);
            binding.bookingStatus.setText(bookingStatus);
//            binding.txtSenderNameAndPhoneNumber.setText(senderName+" . "+senderNumber);
//            binding.txtReceiverNameAndPhoneNumber.setText(receiverName+" . "+receiverNumber);

//            String callButtonText = (currentStatus.equals("Driver Accepted") ||
//                    currentStatus.equals("Driver Arrived") ||
//                    currentStatus.equals("Otp Verified"))
//                    ? "Call Sender" : "Call Receiver";
//            binding.imgCall.setContentDescription(callButtonText);

            updateNextStatus(bookingStatus);
            updateStatusButtons();

        } catch (JSONException e) {
            e.printStackTrace();
            showToast("Error updating ride details");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timerManager != null) {
            timerManager.pauseTimer();
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
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
                nextStatus = "Start Trip";
                break;
            case "Start Trip":
                nextStatus = "Send Payment Details";
                break;
            default:
                nextStatus = "End Trip";
                break;
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
            case "Start Trip":
                binding.btnSendTrip.setVisibility(View.VISIBLE);
                break;
            case "Send Payment Details":
                binding.btnSendPaymentDetails.setVisibility(View.VISIBLE);
                break;
            case "End Trip":
                binding.btnEndTrip.setVisibility(View.VISIBLE);
                break;
        }
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

    private void updateRideStatus(String status) {
        String driverId = preferenceManager.getStringValue("cab_driver_id");
        String token = preferenceManager.getStringValue("cab_driver_token");

        showLoading("Updating cab ride status...");
        String url = APIClient.baseUrl + "update_booking_status_cab_driver";
        String accessToken = AccessToken.getAccessToken();
        JSONObject params = new JSONObject();
        try {
            params.put("booking_id", assignedBookingId);
            params.put("booking_status", status);
            params.put("server_token", accessToken);
            params.put("total_payment", totalPrice.toString());
            params.put("customer_id", customerID);
            params.put("driver_unique_id", driverId);
            params.put("auth", token);
            // Add other required parameters
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
                    fetchBookingDetails(); // Refresh the screen
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

    @Override
    public void onBackPressed() {
        if (isFromFCM) {
            // If from FCM, go to home screen instead of going back
            Intent intent = new Intent(this, HomeActivity.class);
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

        // Set map settings
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        showOnMap();
    }

    private void showOnMap() {
        if(pickupLat != null && pickupLat.isEmpty() == false && pickupLng !=null && destinationLat != null && destinationLng !=null){
            // Get pickup and drop coordinates
            LatLng pickupLatLng = new LatLng(Double.parseDouble(pickupLat), Double.parseDouble(pickupLng));
            LatLng dropLatLng = new LatLng(Double.parseDouble(destinationLat), Double.parseDouble(destinationLng));

            // Add markers
            mMap.addMarker(new MarkerOptions()
                    .position(pickupLatLng)
                    .title("Pickup")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_current_long)));

            mMap.addMarker(new MarkerOptions()
                    .position(dropLatLng)
                    .title("Drop")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_destination_long)));

            // Draw route
            drawRoute(pickupLatLng, dropLatLng);

            // Move camera to show both markers
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(pickupLatLng);
            builder.include(dropLatLng);
            LatLngBounds bounds = builder.build();

            int padding = 100;
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            mMap.animateCamera(cu);
        }
    }


    private void drawRoute(LatLng origin, LatLng destination) {
        String url = getDirectionsUrl(origin, destination);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        // Parse route
                        JSONArray routes = response.getJSONArray("routes");
                        JSONObject route = null;
                        try {
                            route = routes.getJSONObject(0);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                        String encodedPath = overviewPolyline.getString("points");

                        List<LatLng> decodedPath = decodePolyline(encodedPath);

                        // Draw polyline
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

    private void startLocationUpdates() {
//        CabLocationUpdateService.isStoppedManually = false; // Reset the flag
//        Intent serviceIntent = new Intent(this, CabLocationUpdateService.class);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForegroundService(serviceIntent);
//        } else {
//            startService(serviceIntent);
//        }
//
//        // Register broadcast receiver for service restart
//        if (serviceRestartReceiver == null) {
//            serviceRestartReceiver = new BroadcastReceiver() {
//                @Override
//                public void onReceive(Context context, Intent intent) {
//                    if (!CabLocationUpdateService.isStoppedManually) {
//                        // Restart service
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                            startForegroundService(new Intent(CabDriverNewLiveRideActivity.this,
//                                    CabLocationUpdateService.class));
//                        } else {
//                            startService(new Intent(CabDriverNewLiveRideActivity.this,
//                                    CabLocationUpdateService.class));
//                        }
//                    }
//                }
//            };
//
//            registerReceiver(serviceRestartReceiver,
//                    new IntentFilter("com.vtpartnertranspvtltd.vt_partner.RESTART_SERVICE"));
//        }
    }

    private void stopLocationUpdates() {
//        CabLocationUpdateService.isStoppedManually = true;
//        Intent serviceIntent = new Intent(this, CabLocationUpdateService.class);
//        stopService(serviceIntent);
//        Intent floatingIntent = new Intent(this, FloatingWindowService.class);
//        stopService(floatingIntent);
    }
}