package com.kapstranspvtltd.kaps_partner.goods_driver_activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.kapstranspvtltd.kaps_partner.fcm.AccessToken;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.services.FloatingWindowService;
import com.kapstranspvtltd.kaps_partner.services.LocationUpdateService;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityNewLiveRideBinding;
import com.kapstranspvtltd.kaps_partner.databinding.DialogPaymentDetailsBinding;
import com.kapstranspvtltd.kaps_partner.databinding.ItemDropLocationBinding; // Import generated binding

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NewLiveRideActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "NewLiveRideActivity";

    // --- Booking Status Constants ---
    private static final String STATUS_DRIVER_ACCEPTED = "Driver Accepted";
    private static final String STATUS_DRIVER_ARRIVED = "Driver Arrived";
    private static final String STATUS_OTP_VERIFIED = "Otp Verified";
    private static final String STATUS_START_TRIP = "Start Trip";
    // Specific status for API if backend uses them
    private static final String STATUS_REACHED_DROP_1 = "Reached Drop Location 1";
    private static final String STATUS_REACHED_DROP_2 = "Reached Drop Location 2";
    private static final String STATUS_REACHED_DROP_3 = "Reached Drop Location 3";
    private static final String STATUS_MAKE_PAYMENT = "Make Payment";
    private static final String STATUS_END_TRIP = "End Trip";
    // Internal state marker for logic
    private static final String INTERNAL_STATE_REACHED_LAST_DROP = "INTERNAL_ReachedLastDrop";
    private static final String ACTION_CONFIRM_ARRIVAL = "Confirm Arrival";
    private static final String ACTION_VERIFY_OTP = "Verify OTP";
    private static final String ACTION_START_TRIP = "Start Trip";
    private static final String ACTION_CONFIRM_REACHED_DROP = "Confirm Reached Drop "; // Append number
    private static final String ACTION_SEND_PAYMENT_DETAILS = "Send Payment Details";
    private static final String ACTION_END_TRIP = "End Trip";
    // --- End Constants ---


    private BroadcastReceiver serviceRestartReceiver;
    private ActivityNewLiveRideBinding binding;
    private PreferenceManager preferenceManager;
    private GoogleMap mMap;
    private Marker pickupMarker;
    private List<Marker> dropMarkers = new ArrayList<>();
    private List<Polyline> routePolylines = new ArrayList<>();


    private boolean isFromFCM;
    private int assignedBookingId = -1; // Initialize to invalid ID
    private String nextAction = ""; // Renamed from nextStatus for clarity
    private String currentApiStatus = ""; // Status received from API
    private ProgressDialog progressDialog;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    // Ride Details
    String customerName = "";
    String customerID = "";
    String pickupAddress = "";
    String dropAddress = ""; // For single drop display
    String distance = "";
    String totalTime = "";
    String senderName = "";
    String senderNumber = "";
    String receiverName = ""; // Primary receiver for single drop
    String receiverNumber = ""; // Primary receiver for single drop
    Double totalPrice = 0.0;
    String receivedOTP = "";
    String bookingIdStr = ""; // String version of assignedBookingId
    String pickupLat = "0.0", pickupLng = "0.0", destinationLat = "0.0", destinationLng = "0.0";

    // Multiple Drop Details
    private JSONArray dropLocationsArray = null;
    private JSONArray dropContactsArray = null;
    private int multipleDrops = 0; // 0 for single, > 0 for multiple
    private int currentDropIndex = 0; // 0-based index of the *next* drop target or completed count
    private List<LatLng> allDropLatLngs = new ArrayList<>();
    private List<String> allDropAddresses = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNewLiveRideBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.d(TAG, "onCreate");

        initializeViews();
        setupClickListeners();
        getCurrentBookingIdDetails(); // This will trigger fetchBookingDetails if ID is found
        startLocationUpdates(); // Start location service regardless

        binding.navigateBtn.setOnClickListener(v -> navigateToDestination());
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
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "Map fragment not found!");
            showToast("Error initializing map");
        }

        // Setup toolbar
        if (isFromFCM) {
            binding.imgBack.setVisibility(View.GONE);
        }

        // Initially hide action buttons until details are loaded
        hideAllActionButtons();
    }

    private void hideAllActionButtons() {
        binding.btnArrived.setVisibility(View.GONE);
        binding.btnVerifyOtp.setVisibility(View.GONE);
        binding.btnSendTrip.setVisibility(View.GONE);
        binding.btnSendPaymentDetails.setVisibility(View.GONE);
        binding.btnEndTrip.setVisibility(View.GONE);
    }

    private void loadCurrentDropIndex() {
        if (assignedBookingId > 0) {
            String key = "current_drop_index_" + assignedBookingId;
            currentDropIndex = preferenceManager.getIntValue(key, 0); // Default to 0 if not found
            Log.d(TAG, "Loaded currentDropIndex: " + currentDropIndex + " for booking ID: " + assignedBookingId);
        } else {
            currentDropIndex = 0; // Reset if booking ID is invalid
            Log.w(TAG, "Cannot load drop index, invalid booking ID.");
        }
    }

    private void saveCurrentDropIndex() {
        if (assignedBookingId > 0) {
            String key = "current_drop_index_" + assignedBookingId;
            preferenceManager.saveIntValue(key, currentDropIndex);
            Log.d(TAG, "Saved currentDropIndex: " + currentDropIndex + " for booking ID: " + assignedBookingId);
        } else {
            Log.w(TAG, "Cannot save drop index, invalid booking ID.");
        }
    }

    private void setupClickListeners() {
        binding.imgBack.setOnClickListener(v -> onBackPressed());
        binding.imgCall.setOnClickListener(v -> handleCallClick());
        setupStatusButtonListeners();
    }

    private void setupStatusButtonListeners() {
        // Each button triggers a confirmation dialog which then calls updateRideStatus
        binding.btnArrived.setOnClickListener(v -> showConfirmationDialog(STATUS_DRIVER_ARRIVED, "Confirm you have arrived at the pickup location?"));
        binding.btnVerifyOtp.setOnClickListener(v -> showOtpDialog()); // OTP dialog calls updateRideStatus internally
        binding.btnSendTrip.setOnClickListener(v -> showConfirmationDialog(STATUS_START_TRIP, "Confirm starting the trip?"));

        // This button handles multiple actions based on 'nextAction'
        binding.btnSendPaymentDetails.setOnClickListener(v -> {
            if (nextAction != null && nextAction.startsWith(ACTION_CONFIRM_REACHED_DROP)) {
                handleReachedDropConfirmation(); // Special confirmation for drops
            } else if (ACTION_SEND_PAYMENT_DETAILS.equals(nextAction)) {
                showConfirmationDialog(STATUS_MAKE_PAYMENT, "Confirm sending payment details to customer?");
            } else {
                Log.w(TAG, "SendPaymentDetails button clicked in unexpected state: " + nextAction);
                // Maybe default to Make Payment confirmation as a fallback?
                showConfirmationDialog(STATUS_MAKE_PAYMENT, "Confirm sending payment details?");
            }
        });

        binding.btnEndTrip.setOnClickListener(v -> showPaymentDialog()); // Payment dialog calls processPayment -> end trip API
    }


    // --- Ride State & Details ---

    private void getCurrentBookingIdDetails() {
        showLoading("Checking for live ride...");
        String driverId = preferenceManager.getStringValue("goods_driver_id");

        if (driverId == null || driverId.isEmpty()) {
            hideLoading();
            showError("Driver ID not found. Please login again.");
            preferenceManager.saveBooleanValue("isLiveRide",false);
            preferenceManager.saveStringValue("current_booking_id_assigned", "");
            navigateToHomeDelayed(); // Navigate home after showing message
            return;
        }

        String url = APIClient.baseUrl + "get_goods_driver_current_booking_detail";
        JSONObject params = new JSONObject();
        try {
            params.put("goods_driver_id", driverId);
        } catch (JSONException e) {
            Log.e(TAG, "JSON Exception creating params", e);
            hideLoading();
            showError("Error preparing request.");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> {
                    hideLoading();
                    try {
                        int bookingId = response.optInt("current_booking_id", -1); // Default to -1
                        Log.d(TAG, "getCurrentBookingIdDetails Response - current_booking_id: " + bookingId);

                        if (bookingId > 0) {
                            assignedBookingId = bookingId;
                            bookingIdStr = String.valueOf(assignedBookingId);
                            preferenceManager.saveStringValue("current_booking_id_assigned", bookingIdStr);
                            preferenceManager.saveBooleanValue("isLiveRide", true);
                            loadCurrentDropIndex(); // Load saved index for this booking
                            fetchBookingDetails(assignedBookingId); // Fetch details for this specific ID
                        } else {
                            // No active booking found
                            handleNoLiveRideFound();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing getCurrentBookingIdDetails response", e);
                        handleNoLiveRideFound("Error processing booking data.");
                    }
                },
                error -> {
                    hideLoading();
                    Log.e(TAG, "getCurrentBookingIdDetails VolleyError: " + error.toString());
                    if (error.networkResponse != null && error.networkResponse.statusCode == 404) {
                        handleNoLiveRideFound("No active ride found.");
                    } else {
                        handleNoLiveRideFound("Network error checking for rides.");
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return getApiHeaders();
            }
        };
        configureAndAddRequest(request);
    }

    private void fetchBookingDetails(int bookingIdToFetch) {
        Log.d(TAG, "Fetching booking details for ID: " + bookingIdToFetch);
        showLoading("Fetching ride details...");
        String url = APIClient.baseUrl + "booking_details_live_track";

        JSONObject params = new JSONObject();
        try {
            params.put("booking_id", bookingIdToFetch);
        } catch (JSONException e) {
            Log.e(TAG, "JSON Exception creating params for fetchBookingDetails", e);
            hideLoading();
            showError("Error preparing request.");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> {
                    hideLoading();
                    try {
                        JSONArray results = response.getJSONArray("results");
                        if (results.length() > 0) {
                            JSONObject rideDetails = results.getJSONObject(0);
                            Log.d(TAG, "Received Ride Details: " + rideDetails.toString());
                            updateRideDetails(rideDetails);
                            showOnMap(); // Update map after details are processed
                        } else {
                            Log.w(TAG, "No results found in fetchBookingDetails response for ID: " + bookingIdToFetch);
                            showError("Booking details not found.");
                            handleNoLiveRideFound(); // Treat as no live ride if details missing
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing ride details JSON", e);
                        showError("Error loading ride details.");
                        handleNoLiveRideFound();
                    }
                },
                error -> {
                    hideLoading();
                    Log.e(TAG, "fetchBookingDetails VolleyError: " + error.toString());
                    handleVolleyError(error); // Generic error handling
                    handleNoLiveRideFound("Error fetching details. Ride may be cancelled.");
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                return getApiHeaders();
            }
        };
        configureAndAddRequest(request);
    }

    private void updateRideDetails(JSONObject rideDetails) {
        try {
            // --- Basic Details ---
            bookingIdStr = rideDetails.getString("booking_id"); // Keep string version if needed
            // assignedBookingId should already be set
            customerName = rideDetails.getString("customer_name");
            customerID = rideDetails.getString("customer_id");
            pickupAddress = rideDetails.getString("pickup_address");
            dropAddress = rideDetails.getString("drop_address"); // Used for single drop display
            distance = rideDetails.getString("distance");
            totalTime = rideDetails.getString("total_time");
            senderName = rideDetails.getString("sender_name");
            senderNumber = rideDetails.getString("sender_number");
            receiverName = rideDetails.getString("receiver_name"); // Primary receiver
            receiverNumber = rideDetails.getString("receiver_number"); // Primary receiver
            totalPrice = rideDetails.getDouble("total_price");
            currentApiStatus = rideDetails.getString("booking_status"); // Store the status from API
            receivedOTP = rideDetails.getString("otp");
            pickupLat = rideDetails.getString("pickup_lat");
            pickupLng = rideDetails.getString("pickup_lng");
            destinationLat = rideDetails.getString("destination_lat"); // Single drop lat
            destinationLng = rideDetails.getString("destination_lng"); // Single drop lng
            String bookingTiming = rideDetails.getString("booking_timing");
            String formattedBookingTiming = getFormattedBookingTiming(bookingTiming);

            // --- Multiple Drop Details ---
            multipleDrops = rideDetails.optInt("multiple_drops", 0); // Default to 0 (single)
            Log.d(TAG, "Multiple Drops Count: " + multipleDrops);

            dropLocationsArray = null; // Reset before parsing
            dropContactsArray = null;
            allDropLatLngs.clear();
            allDropAddresses.clear();

            if (multipleDrops > 0) {
                String dropLocationsStr = rideDetails.optString("drop_locations");
                String dropContactsStr = rideDetails.optString("drop_contacts");

                if (dropLocationsStr != null && !dropLocationsStr.isEmpty() && !dropLocationsStr.equals("[]")) {
                    try {
                        dropLocationsArray = new JSONArray(dropLocationsStr);
                        // Parse contacts only if locations were parsed successfully
                        if (dropContactsStr != null && !dropContactsStr.isEmpty() && !dropContactsStr.equals("[]")) {
                            dropContactsArray = new JSONArray(dropContactsStr);
                        } else {
                            Log.w(TAG,"Drop contacts string is missing or empty.");
                            dropContactsArray = new JSONArray(); // Create empty array if missing
                        }


                        for (int i = 0; i < dropLocationsArray.length(); i++) {
                            JSONObject drop = dropLocationsArray.getJSONObject(i);
                            double lat = drop.optDouble("lat", 0.0);
                            double lng = drop.optDouble("lng", 0.0);
                            String address = drop.optString("address", "Address N/A");
                            if (lat != 0.0 && lng != 0.0) { // Basic validation
                                allDropLatLngs.add(new LatLng(lat, lng));
                                allDropAddresses.add(address);
                            } else {
                                Log.w(TAG,"Invalid lat/lng for drop location at index " + i);
                            }
                        }
                        Log.d(TAG, "Parsed " + allDropLatLngs.size() + " valid drop locations.");
                        if (allDropLatLngs.isEmpty()) multipleDrops = 0; // Treat as single if parsing failed

                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing drop locations/contacts JSON: " + e.getMessage());
                        multipleDrops = 0; // Treat as single drop if parsing fails
                    }
                } else {
                    Log.w(TAG,"Multiple drops indicated ("+multipleDrops+"), but drop_locations string is missing or empty.");
                    multipleDrops = 0; // Treat as single drop
                }
            }

            // --- Update UI Elements ---
            binding.txtCustomerName.setText(customerName);
            binding.txtPickAddress.setText(pickupAddress);
            binding.txtSenderNameAndPhoneNumber.setText(senderName + " · " + senderNumber);
            binding.txtDistance.setText(distance + " Km");
            binding.txtTime.setText(totalTime);
            binding.bookingTiming.setText(formattedBookingTiming);
            binding.txtTotalPrice.setText("₹" + Math.round(totalPrice));
            binding.txtBookingId.setText("#CRN" + assignedBookingId);
            binding.toolbarBookingId.setText("#CRN " + assignedBookingId);
            binding.bookingStatus.setText(currentApiStatus); // Show status from API

            // Handle UI visibility for single vs multiple drops
            if (multipleDrops > 0 && !allDropLatLngs.isEmpty()) {
                binding.singleDropLayout.setVisibility(View.GONE);
                binding.multipleDropsIndicator.setVisibility(View.VISIBLE);
                binding.multipleDropsCount.setText(String.format(Locale.getDefault(), "%d Drops", allDropLatLngs.size()));
                updateDropLocationsUI(); // Update the list of drops UI
            } else {
                // Single drop display
                binding.singleDropLayout.setVisibility(View.VISIBLE);
                binding.multipleDropsIndicator.setVisibility(View.GONE);
                binding.txtDropAddress.setText(dropAddress);
                binding.txtReceiverNameAndPhoneNumber.setText(receiverName + " · " + receiverNumber);
                binding.dropLocationsContainer.removeAllViews(); // Clear multi-drop UI
            }

            // --- Determine Next Action & Update Buttons ---
            // **Important:** Load index *before* determining next status
            loadCurrentDropIndex();
            updateNextStatus(currentApiStatus);
            updateStatusButtons();

        } catch (JSONException e) {
            Log.e(TAG, "Fatal error updating ride details from JSON", e);
            showToast("Error displaying ride details.");
            handleNoLiveRideFound(); // Finish if core details fail
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in updateRideDetails", e);
            showToast("An unexpected error occurred.");
            handleNoLiveRideFound();
        }
    }

    private void updateDropLocationsUI() {
        binding.dropLocationsContainer.removeAllViews(); // Clear previous views
        if (dropLocationsArray == null || allDropAddresses.isEmpty()) {
            Log.d(TAG, "No drop locations to display in UI.");
            return;
        }
        Log.d(TAG,"Updating drop locations UI. Current Index: " + currentDropIndex);

        LayoutInflater inflater = LayoutInflater.from(this);
        try {
            for (int i = 0; i < allDropAddresses.size(); i++) {
                // Use ViewBinding for the item layout
                ItemDropLocationBinding itemBinding = ItemDropLocationBinding.inflate(inflater, binding.dropLocationsContainer, false);

                JSONObject dropContact = null;
                if (dropContactsArray != null && i < dropContactsArray.length()) {
                    try {
                        dropContact = dropContactsArray.getJSONObject(i);
                    } catch (JSONException e){
                        Log.w(TAG, "Could not parse drop contact at index " + i);
                    }
                }


                // Set data
                itemBinding.dropNumber.setText(String.format(Locale.getDefault(), "Drop %d", i + 1));
                itemBinding.dropAddress.setText(allDropAddresses.get(i));

                if (dropContact != null) {
                    String name = dropContact.optString("name", "N/A");
                    String mobile = dropContact.optString("mobile", "N/A");
                    if (name.equals("N/A") && mobile.equals("N/A")) {
                        itemBinding.dropContact.setText("No contact information");
                        itemBinding.dropContact.setTextColor(Color.GRAY);
                    } else {
                        itemBinding.dropContact.setText(name + " · " + mobile);
                        
                    }
                } else {
                    itemBinding.dropContact.setText("No contact information");
                    itemBinding.dropContact.setTextColor(Color.GRAY);
                }

                // --- Highlighting Logic ---
                if (i == currentDropIndex && currentApiStatus != null && currentApiStatus.equals(STATUS_START_TRIP)) {
                    // Current drop to visit
                    itemBinding.getRoot().setBackgroundResource(R.drawable.current_drop_background); // Use your drawable
                    itemBinding.statusIndicator.setBackgroundResource(R.drawable.status_current);     // Use your drawable
                } else if (i < currentDropIndex) {
                    // Completed drops
                    itemBinding.getRoot().setBackgroundResource(android.R.color.transparent); // Or a completed style
                    itemBinding.statusIndicator.setBackgroundResource(R.drawable.status_completed);   // Use your drawable
                    itemBinding.getRoot().setAlpha(0.7f); // Fade completed items slightly
                } else {
                    // Upcoming drops
                    itemBinding.getRoot().setBackgroundResource(android.R.color.transparent);
                    itemBinding.statusIndicator.setBackgroundResource(R.drawable.status_pending); // Use your drawable for pending
                    itemBinding.getRoot().setAlpha(1.0f);
                }
                // --- End Highlighting Logic ---

                binding.dropLocationsContainer.addView(itemBinding.getRoot());
            }
        } catch (Exception e) { // Catch broader exceptions during UI inflation/update
            Log.e(TAG, "Error updating drop locations UI: " + e.getMessage(), e);
        }
    }

    // Determines the next *action* the driver should take
    private void updateNextStatus(String currentApiStatus) {
        this.currentApiStatus = currentApiStatus; // Store the latest status from API
        nextAction = ""; // Reset next action
        System.out.println("currentApiStatus::"+currentApiStatus);
        switch (currentApiStatus) {
            case STATUS_DRIVER_ACCEPTED:
                nextAction = ACTION_CONFIRM_ARRIVAL;
                break;
            case STATUS_DRIVER_ARRIVED:
                nextAction = ACTION_VERIFY_OTP;
                break;
            case STATUS_OTP_VERIFIED:
                nextAction = ACTION_START_TRIP;
                break;
            case STATUS_START_TRIP:
                // Determine if heading to a drop or finished all drops
                if (multipleDrops > 0 && !allDropLatLngs.isEmpty()) {
                    if (currentDropIndex < allDropLatLngs.size()) {
                        // Still have drops to visit
                        int dropNumber = currentDropIndex + 1;
                        nextAction = ACTION_CONFIRM_REACHED_DROP + dropNumber;
                    } else {
                        // All multi-drops completed
                        nextAction = ACTION_SEND_PAYMENT_DETAILS; // Ready for payment
                    }
                } else {
                    // Single drop scenario
                    nextAction = ACTION_SEND_PAYMENT_DETAILS; // Ready for payment
                }
                break;
            // Handle cases where the API status reflects a completed drop
            case STATUS_REACHED_DROP_1:
            case STATUS_REACHED_DROP_2:
            case STATUS_REACHED_DROP_3: // Add more if needed
                if (multipleDrops > 0 && !allDropLatLngs.isEmpty()) {
                    // Assume currentDropIndex was updated *before* this status was set
                    if (currentDropIndex < allDropLatLngs.size()) {
                        int dropNumber = currentDropIndex + 1;
                        nextAction = ACTION_CONFIRM_REACHED_DROP + dropNumber;
                    } else {
                        nextAction = ACTION_SEND_PAYMENT_DETAILS; // All done
                    }
                } else {
                    // Should technically not happen for single drop
                    nextAction = ACTION_SEND_PAYMENT_DETAILS;
                }
                break;

            case STATUS_MAKE_PAYMENT: // Status indicating payment details sent
                nextAction = ACTION_END_TRIP; // Final step is ending via payment dialog
                break;
            case STATUS_END_TRIP:
                nextAction = ""; // Ride finished
                // Consider triggering navigation home here if not done elsewhere
                handleNoLiveRideFound();
                break;
            // Add case for "Cancelled" if needed
            default:
                Log.w(TAG, "Unhandled API Status: " + currentApiStatus);
                // If unknown status, maybe default to ending trip or showing no action
                if (totalPrice > 0) { // Basic check if ride seems active
                    nextAction = ACTION_END_TRIP;
                } else {
                    nextAction = "";
                }
                break;
        }
        Log.i(TAG, "updateNextStatus - Current API Status: '" + currentApiStatus + "', Next Driver Action: '" + nextAction + "', Drop Index: " + currentDropIndex);
    }

    // Updates button visibility and text based on 'nextAction'
    private void updateStatusButtons() {
        hideAllActionButtons(); // Start clean

        Log.d(TAG,"Updating status buttons based on nextAction: " + nextAction);

        if (ACTION_CONFIRM_ARRIVAL.equals(nextAction)) {
            binding.btnArrived.setVisibility(View.VISIBLE);
        } else if (ACTION_VERIFY_OTP.equals(nextAction)) {
            binding.btnVerifyOtp.setVisibility(View.VISIBLE);
        } else if (ACTION_START_TRIP.equals(nextAction)) {
            binding.btnSendTrip.setVisibility(View.VISIBLE);
        } else if (nextAction != null && nextAction.startsWith(ACTION_CONFIRM_REACHED_DROP)) {
            binding.btnSendPaymentDetails.setVisibility(View.VISIBLE);
            // Extract number and set text, e.g., "Confirm Reached Drop 1"
            binding.btnSendPaymentDetails.setText(nextAction);
        } else if (ACTION_SEND_PAYMENT_DETAILS.equals(nextAction)) {
            binding.btnSendPaymentDetails.setVisibility(View.VISIBLE);
            binding.btnSendPaymentDetails.setText(ACTION_SEND_PAYMENT_DETAILS); // Explicitly set text
        } else if (ACTION_END_TRIP.equals(nextAction)) {
            binding.btnEndTrip.setVisibility(View.VISIBLE);
        } else {
            Log.d(TAG,"No specific action button to show for nextAction: " + nextAction);
            // No button shown if nextAction is empty or unhandled
        }
    }


    // --- User Actions & Dialogs ---

    private void showOtpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_otp_verification, null);
        EditText otpInput = dialogView.findViewById(R.id.otp_input);

        builder.setView(dialogView)
                .setTitle("Verify OTP")
                .setPositiveButton("Verify", null) // Set null to override later
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                String enteredOtp = otpInput.getText().toString().trim();
                if (enteredOtp.isEmpty()) {
                    otpInput.setError("Please enter OTP");
                    return;
                }
                if (receivedOTP == null || receivedOTP.isEmpty()) {
                    otpInput.setError("Could not verify OTP. Please contact support."); // Should not happen
                    Log.e(TAG, "Cannot verify OTP, receivedOTP is null or empty.");
                    return;
                }

                if (!enteredOtp.equals(receivedOTP)) {
                    otpInput.setError("Invalid OTP");
                    return;
                }
                // OTP is correct
                dialog.dismiss();
                updateRideStatus(STATUS_OTP_VERIFIED); // Update status to OTP Verified
            });
        });

        dialog.show();
    }

    // Generic confirmation for status updates (except OTP and Reached Drop)
    private void showConfirmationDialog(String statusToUpdate, String message) {
        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle("Confirm Action")
                .setMessage(message != null ? message : "Are you sure you want to update status to " + statusToUpdate + "?")
                .setPositiveButton("Yes", (dialog, which) -> updateRideStatus(statusToUpdate))
                .setNegativeButton("No", null)
                .show();
    }

    // Specific confirmation for reaching a drop-off point
    private void handleReachedDropConfirmation() {
        if (multipleDrops <= 0 || allDropAddresses.isEmpty() || currentDropIndex >= allDropAddresses.size()) {
            Log.e(TAG, "Invalid state for Reached Drop confirmation. Index: " + currentDropIndex + ", Total Drops: " + allDropAddresses.size());
            showToast("Internal error confirming drop.");
            return;
        }

        int dropNumberUi = currentDropIndex + 1; // For display (1-based)
        String address = allDropAddresses.get(currentDropIndex);
        String title = "Confirm Drop " + dropNumberUi;
        String message = "Have you reached Drop " + dropNumberUi + " at:\n" + address + "?";

        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Yes, Reached", (dialog, which) -> {
                    // --- Driver Confirmed Reaching Drop ---
                    Log.i(TAG, "Driver confirmed reaching Drop " + dropNumberUi);

                    // Determine the API status to send based on which drop it is
                    String apiStatusUpdate;
                    int nextDropIndex = currentDropIndex + 1; // Index after this one is completed

                    if (nextDropIndex >= allDropLatLngs.size()) {
                        // This was the LAST drop
                        apiStatusUpdate = STATUS_MAKE_PAYMENT; // Or a specific "Reached Last Drop" if API supports it
                        Log.d(TAG, "This was the last drop. Setting status for API: " + apiStatusUpdate);
                    } else {
                        // Intermediate drop completed, determine specific status
                        switch(dropNumberUi) {
                            case 1: apiStatusUpdate = STATUS_REACHED_DROP_1; break;
                            case 2: apiStatusUpdate = STATUS_REACHED_DROP_2; break;
                            case 3: apiStatusUpdate = STATUS_REACHED_DROP_3; break;
                            // Add more cases if needed, or use a generic "Reached Drop"
                            default:
                                Log.w(TAG, "Reached drop " + dropNumberUi + ", using generic status (if API supports).");
                                apiStatusUpdate = "Reached Drop Location"; // Generic fallback - CHECK API
                                break;
                        }
                        Log.d(TAG, "Intermediate drop " + dropNumberUi + " reached. Setting status for API: " + apiStatusUpdate);
                    }

                    // Update the status via API
                    updateRideStatus(apiStatusUpdate);


                })
                .setNegativeButton("No", null)
                .show();
    }


    // --- API Calls & Handling ---

    private Map<String, String> getApiHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        // Add Authorization or other common headers if needed
        // String token = preferenceManager.getToken();
        // if (token != null) headers.put("Authorization", "Bearer " + token);
        return headers;
    }

    private void configureAndAddRequest(JsonObjectRequest request) {
        request.setRetryPolicy(new DefaultRetryPolicy(
                30000, // 30 seconds timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void updateRideStatus(String statusToUpdate) {
        if (assignedBookingId <= 0) {
            showToast("Cannot update status, invalid booking ID.");
            return;
        }

        showLoading("Updating status to " + statusToUpdate + "...");
        String url = APIClient.baseUrl + "update_booking_status_driver";

        // Use ExecutorService for background token retrieval
        executorService.submit(() -> {
            String accessToken = null;
            try {
                accessToken = AccessToken.getAccessToken(); // Assuming this might block
            } catch (Exception e) {
                Log.e(TAG, "Error getting access token", e);
            }

            final String finalAccessToken = accessToken;
            mainThreadHandler.post(() -> { // Switch back to main thread for UI and Volley
                if (finalAccessToken == null || finalAccessToken.isEmpty()) {
                    hideLoading();
                    showToast("Authentication error. Please login again.");
                    // Potentially redirect to login
                    return;
                }

                JSONObject params = new JSONObject();
                try {
                    params.put("booking_id", assignedBookingId);
                    params.put("booking_status", statusToUpdate);
                    params.put("server_token", finalAccessToken);
                    params.put("total_payment", totalPrice != null ? totalPrice.toString() : "0.0"); // Ensure totalPrice isn't null
                    params.put("customer_id", customerID != null ? customerID : ""); // Ensure customerID isn't null


                    // --- Include current_drop_index if relevant ---
                    // Send the index of the drop that was *just completed* when confirming reach.
                    if (statusToUpdate.startsWith("Reached Drop Location") || statusToUpdate.equals(STATUS_MAKE_PAYMENT)) {
                        // If status is "Reached Drop Location X", index X-1 was just completed.
                        // If status is "Make Payment", the last drop (index size-1) was just completed.
                        int completedIndex = -1;
                        if (statusToUpdate.equals(STATUS_MAKE_PAYMENT)) {
                            completedIndex = allDropLatLngs.size() - 1; // Last index
                        } else if (statusToUpdate.startsWith("Reached Drop Location")) {
                            try {
                                // Extract number from status string (e.g., "Reached Drop Location 1" -> 1)
                                String numberStr = statusToUpdate.substring(statusToUpdate.lastIndexOf(" ") + 1);
                                int dropNumber = Integer.parseInt(numberStr);
                                completedIndex = dropNumber - 1; // 0-based index
                            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                                Log.e(TAG,"Could not parse drop number from status: " + statusToUpdate);
                                // Fallback: send the current index? Check API docs.
                                completedIndex = currentDropIndex; // Might be off by one depending on timing
                            }
                        }

                        if (completedIndex >= 0) {
                            params.put("current_drop_index", completedIndex);
                            Log.d(TAG,"Including completed_drop_index: " + completedIndex + " for status update: " + statusToUpdate);
                        } else {
                            Log.w(TAG, "Not including drop index for status: " + statusToUpdate);
                        }
                    }
                    // --- End include current_drop_index ---


                } catch (JSONException e) {
                    Log.e(TAG, "JSONException creating status update params", e);
                    hideLoading();
                    showToast("Error creating status update request.");
                    return;
                }

                Log.d(TAG, "updateRideStatus Request: " + params.toString());

                JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                        response -> {
                            hideLoading();
                            Log.d(TAG, "updateRideStatus Success: " + response.toString());
                            showToast("Status updated to " + statusToUpdate);

                            // --- IMPORTANT: Update local state AFTER API success ---
                            if (statusToUpdate.startsWith("Reached Drop Location") || statusToUpdate.equals(STATUS_MAKE_PAYMENT) ) {
                                // If confirming a drop or starting payment, increment the index locally
                                // (assuming the API call means the *previous* index is now done)
                                if (currentDropIndex < allDropLatLngs.size()){ // Prevent index out of bounds
                                    currentDropIndex++;
                                    saveCurrentDropIndex(); // Persist the new index
                                    Log.i(TAG, "Incremented currentDropIndex to: " + currentDropIndex + " after status update.");
                                }
                            }
                            // --- End local state update ---

                            // Refresh details from server to get the absolute latest state
                            fetchBookingDetails(assignedBookingId);
                        },
                        error -> {
                            hideLoading();
                            Log.e(TAG, "updateRideStatus VolleyError: " + error.toString());
                            handleVolleyError(error); // Show appropriate error message
                            // Consider fetching details even on error to resync state?
                            // fetchBookingDetails(assignedBookingId);
                        }
                ) {
                    @Override
                    public Map<String, String> getHeaders() {
                        return getApiHeaders();
                    }
                };
                configureAndAddRequest(request);
            });
        });
    }


    // --- Payment & End Trip ---

    private void showPaymentDialog() {
        if (totalPrice == null) {
            showToast("Cannot process payment, amount is missing.");
            return;
        }
        String amountStr = String.valueOf(Math.round(totalPrice));

        // Inflate dialog layout using ViewBinding
        DialogPaymentDetailsBinding dialogBinding = DialogPaymentDetailsBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setView(dialogBinding.getRoot())
//                .setTitle("Payment Details")
                .setCancelable(false) // Prevent dismissing easily
                .create();

        dialogBinding.amountValue.setText("₹" + amountStr);

        // Ensure Cash is selected by default or based on preference
        dialogBinding.cashRadioButton.setChecked(true);

        dialogBinding.cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialogBinding.confirmButton.setOnClickListener(v -> {
            String paymentMethod = getSelectedPaymentType(dialogBinding);
            dialog.dismiss();
            processPayment(amountStr, paymentMethod);
        });

        dialog.show();
    }

    private String getSelectedPaymentType(DialogPaymentDetailsBinding dialogBinding) {
        return dialogBinding.paymentTypeGroup.getCheckedRadioButtonId() == R.id.onlineRadioButton
                ? "Online" : "Cash";
    }

    private void processPayment(String amount, String paymentMethod) {
        Log.d(TAG,"Processing payment. Amount: " + amount + ", Method: " + paymentMethod);
        showLoading("Processing payment...");

        String driverId = preferenceManager.getStringValue("goods_driver_id");
        if (driverId == null || driverId.isEmpty() || customerID == null || customerID.isEmpty() || bookingIdStr == null || bookingIdStr.isEmpty()) {
            hideLoading();
            showToast("Cannot process payment. Missing required IDs.");
            Log.e(TAG,"Missing driverId, customerID, or bookingIdStr for payment processing.");
            return;
        }


        // Use background thread for token retrieval
        executorService.submit(() -> {
            String accessToken = null;
            try {
                accessToken = AccessToken.getAccessToken();
            } catch (Exception e) {
                Log.e(TAG, "Error getting access token for payment", e);
            }

            final String finalAccessToken = accessToken;
            mainThreadHandler.post(() -> {
                if (finalAccessToken == null || finalAccessToken.isEmpty()) {
                    hideLoading();
                    showToast("Authentication error. Please login again.");
                    return;
                }

                JSONObject params = new JSONObject();
                try {
                    params.put("booking_id", bookingIdStr); // Use the string booking ID
                    params.put("payment_method", paymentMethod);
                    params.put("payment_id", -1); // Assuming backend handles this or it's not needed
                    params.put("booking_status", STATUS_END_TRIP); // Final status
                    params.put("server_token", finalAccessToken);
                    params.put("driver_id", driverId);
                    params.put("customer_id", customerID);
                    params.put("total_amount", Math.round(Double.parseDouble(amount))); // Send rounded amount
                } catch (JSONException | NumberFormatException e) {
                    Log.e(TAG, "Error creating payment JSON params", e);
                    hideLoading();
                    showToast("Error preparing payment request.");
                    return;
                }

                Log.d(TAG, "EndTrip API Request: " + params.toString());
                String url = APIClient.baseUrl + "generate_order_id_for_booking_id_goods_driver";

                JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                        response -> {
                            Log.d(TAG, "EndTrip API Success: " + response.toString());
                            handlePaymentSuccess(); // Calls navigateToHome after delay
                        },
                        error -> {
                            Log.e(TAG, "EndTrip API Error: " + error.toString());
                            handlePaymentError(error); // Shows relevant error message
                        }
                ) {
                    @Override
                    public Map<String, String> getHeaders() {
                        return getApiHeaders();
                    }
                };
                configureAndAddRequest(request);
            });
        });
    }

    private void handlePaymentSuccess() {
        // Already on main thread from processPayment callback
        hideLoading();
        clearRideState(); // Clear saved state for this ride
        showToast("Trip Completed Successfully!");
        navigateToHomeDelayed();
    }

    private void handlePaymentError(VolleyError error) {
        // Already on main thread
        hideLoading();
        if (error.networkResponse != null) {
            Log.e(TAG, "Payment Error Status Code: " + error.networkResponse.statusCode);
            if (error.networkResponse.statusCode == 404) {
                // Specific handling for 404
                showError("Booking not found or already completed.");
                clearRideState();
                navigateToHomeDelayed();
            } else {
                // Other server errors
                showError("Payment processing failed on server.");
            }
        } else if (error instanceof NoConnectionError) {
            showError("No internet connection. Please check network and try again.");
        } else if (error instanceof TimeoutError) {
            showError("Payment request timed out. Please try again.");
        } else {
            showError("Payment failed. Please try again or contact support.");
        }
    }

    private void clearRideState() {
        Log.d(TAG,"Clearing ride state for booking ID: " + assignedBookingId);
        preferenceManager.saveBooleanValue("isLiveRide", false);
        preferenceManager.saveStringValue("current_booking_id_assigned", "");
        if (assignedBookingId > 0) {
            // Clear the drop index specific to this booking
            String key = "current_drop_index_" + assignedBookingId;
            preferenceManager.removeValue(key);
            Log.d(TAG,"Removed drop index from preferences for key: " + key);
        }
        // Reset local variables
        assignedBookingId = -1;
        currentDropIndex = 0;
        // Consider clearing other ride detail variables if necessary
    }

    private void handleNoLiveRideFound(String... message) {
        Log.d(TAG, "handleNoLiveRideFound called.");
        String msgToShow = (message != null && message.length > 0) ? message[0] : "No active ride found.";
        showToast(msgToShow);
        clearRideState();
        stopLocationUpdates(); // Stop service if no ride
        navigateToHome(); // Navigate immediately
    }

    private void navigateToHome() {
        Log.d(TAG, "Navigating to HomeActivity.");
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Finish this activity
    }

    private void navigateToHomeDelayed() {
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateToHome, 1500); // 1.5 second delay
    }


    // --- Map & Navigation ---

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        Log.d(TAG, "Map is ready.");
        try {
            // Basic map settings
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.getUiSettings().setCompassEnabled(true);
            mMap.getUiSettings().setMapToolbarEnabled(false); // Disable distracting toolbar

            // Set custom info window adapter if needed
            // mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(this));

            // Load initial data if available
            if (assignedBookingId > 0) {
                showOnMap();
            } else {
                Log.d(TAG,"Map ready, but no booking details yet.");
                // Optionally move camera to default location like driver's current location
            }

        } catch (Exception e) {
            Log.e(TAG, "Error configuring map settings", e);
            showToast("Error setting up map.");
        }
    }

    private void showOnMap() {
        if (mMap == null) {
            Log.w(TAG, "showOnMap called but map is not ready.");
            return;
        }
        if (pickupLat == null || pickupLat.equals("0.0") || pickupLng == null || pickupLng.equals("0.0")) {
            Log.w(TAG, "showOnMap called but pickup location is invalid.");
            // Don't clear map if pickup is invalid, might be showing previous state
            return;
        }

        Log.d(TAG,"showOnMap - Updating map for booking: " + assignedBookingId + ", Current Drop Index: " + currentDropIndex + ", Status: " + currentApiStatus);

        // --- Clear Previous Map Elements ---
        mMap.clear(); // Clears all markers, polylines, etc.
        dropMarkers.clear();
        routePolylines.clear();

        // --- Add Pickup Marker ---
        LatLng pickupLatLng = new LatLng(Double.parseDouble(pickupLat), Double.parseDouble(pickupLng));
        pickupMarker = mMap.addMarker(new MarkerOptions()
                .position(pickupLatLng)
                .title("Pickup")
                .snippet(pickupAddress) // Show address in snippet
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_current_long)) // Use specific icon
                .zIndex(0.5f)); // Slightly above default

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boundsBuilder.include(pickupLatLng);

        // --- Add Drop Markers & Calculate Routes ---
        LatLng lastPointForRoute = pickupLatLng; // Start route from pickup

        if (multipleDrops > 0 && !allDropLatLngs.isEmpty()) {
            // Multiple Drops Scenario
            for (int i = 0; i < allDropLatLngs.size(); i++) {
                LatLng dropLatLng = allDropLatLngs.get(i);
                String title = "Drop " + (i + 1);
                int markerIconRes;
                float alpha = 1.0f;
                float zIndex = 0.0f;
                boolean isCurrentTarget = (i == currentDropIndex && currentApiStatus != null && (currentApiStatus.equals(STATUS_START_TRIP) || currentApiStatus.startsWith("Reached Drop")));

                if (i < currentDropIndex) {
                    // Completed Drop
                    markerIconRes = R.drawable.ic_destination_long; // Use completed icon
                    title += " (Completed)";
                    alpha = 0.6f; // Fade completed
                } else if (isCurrentTarget) {
                    // Current Target Drop
                    markerIconRes = R.drawable.ic_current_long; // Use highlighted icon
                    title += " (Next)";
                    zIndex = 1.0f; // Bring to front
                } else {
                    // Upcoming Drop
                    markerIconRes = R.drawable.ic_destination_long; // Standard drop icon
                }

                Marker dropMarker = mMap.addMarker(new MarkerOptions()
                        .position(dropLatLng)
                        .title(title)
                        .snippet(allDropAddresses.size() > i ? allDropAddresses.get(i) : "Drop Address") // Show address
                        .icon(BitmapDescriptorFactory.fromResource(markerIconRes))
                        .alpha(alpha)
                        .zIndex(zIndex));
                dropMarkers.add(dropMarker);
                boundsBuilder.include(dropLatLng);

                // Draw route segment if this drop is relevant
                if (i <= currentDropIndex || i == 0) { // Draw route up to and including current target
                    drawRoute(lastPointForRoute, dropLatLng);
                    lastPointForRoute = dropLatLng; // Next route starts from here
                }
            }
        } else {
            // Single Drop Scenario
            if (destinationLat != null && !destinationLat.equals("0.0") && destinationLng != null && !destinationLng.equals("0.0")) {
                LatLng dropLatLng = new LatLng(Double.parseDouble(destinationLat), Double.parseDouble(destinationLng));
                Marker dropMarker = mMap.addMarker(new MarkerOptions()
                        .position(dropLatLng)
                        .title("Drop")
                        .snippet(dropAddress)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_destination_long))); // Standard drop icon
                dropMarkers.add(dropMarker); // Add to list even if single
                boundsBuilder.include(dropLatLng);
                drawRoute(lastPointForRoute, dropLatLng); // Draw route from pickup to single drop
            } else {
                Log.w(TAG,"Single drop selected, but destination coordinates are invalid.");
            }
        }

        // --- Move Camera ---
        try {
            // Use post to ensure map layout is complete
            final View mapView = getSupportFragmentManager().findFragmentById(R.id.map).getView();
            if (mapView != null && mapView.getViewTreeObserver().isAlive()) {
                mapView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // Remove listener to prevent multiple calls
                        if (mapView.getViewTreeObserver().isAlive()){
                            mapView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }

                        try {
                            LatLngBounds bounds = boundsBuilder.build();
                            int padding = 150; // Pixels padding
                            // Check if width/height are valid before calling newLatLngBounds
                            if (mapView.getWidth() == 0 || mapView.getHeight() == 0) {
                                Log.w(TAG,"Map layout not ready for bounds animation (width/height is 0). Moving camera directly.");
                                // Fallback: Center on pickup or current drop target
                                LatLng centerPoint = determineMapCenter();
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(centerPoint, 14));
                            } else {
                                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                                mMap.animateCamera(cu);
                                Log.d(TAG,"Map camera animated to bounds.");
                            }
                        } catch (IllegalStateException ise) {
                            // Bounds contain no points
                            Log.e(TAG, "Error building bounds for camera animation (likely no points): " + ise.getMessage());
                            LatLng centerPoint = determineMapCenter();
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(centerPoint, 14)); // Fallback zoom
                        } catch (Exception e) {
                            Log.e(TAG,"Error animating camera in OnGlobalLayoutListener: " + e.getMessage());
                        }
                    }
                });
            } else {
                Log.w(TAG,"MapView or ViewTreeObserver is null/dead, cannot animate camera safely.");
                LatLng centerPoint = determineMapCenter();
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(centerPoint, 14)); // Non-animated fallback
            }
        } catch (Exception e) {
            Log.e(TAG, "General error setting up map camera animation: " + e.getMessage());
            LatLng centerPoint = determineMapCenter();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(centerPoint, 14)); // Non-animated fallback
        }
    }

    private LatLng determineMapCenter() {
        LatLng centerPoint = new LatLng(0,0); // Default fallback
        if (currentApiStatus != null && currentApiStatus.equals(STATUS_START_TRIP) && !allDropLatLngs.isEmpty() && currentDropIndex < allDropLatLngs.size()) {
            centerPoint = allDropLatLngs.get(currentDropIndex); // Center on current target drop
        } else if (pickupLat != null && !pickupLat.equals("0.0")) {
            centerPoint = new LatLng(Double.parseDouble(pickupLat), Double.parseDouble(pickupLng)); // Center on pickup
        }
        Log.d(TAG,"Determined map center fallback point: " + centerPoint);
        return centerPoint;
    }


    private void drawRoute(LatLng origin, LatLng destination) {
        Log.d(TAG, "Drawing route from " + origin + " to " + destination);
        String url = getDirectionsUrl(origin, destination);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray routes = response.optJSONArray("routes");
                        if (routes != null && routes.length() > 0) {
                            JSONObject route = routes.getJSONObject(0);
                            JSONObject overviewPolyline = route.optJSONObject("overview_polyline");
                            if (overviewPolyline != null) {
                                String encodedPath = overviewPolyline.getString("points");
                                List<LatLng> decodedPath = decodePolyline(encodedPath);

                                if (!decodedPath.isEmpty()) {
                                    PolylineOptions polylineOptions = new PolylineOptions()
                                            .addAll(decodedPath)
                                            .width(10) // Slightly thicker line
                                            .color(ContextCompat.getColor(this, R.color.colorPrimary)) // Use color resource
                                            .geodesic(true);
                                    Polyline polyline = mMap.addPolyline(polylineOptions);
                                    routePolylines.add(polyline); // Keep track to clear later
                                    Log.d(TAG,"Route polyline added.");
                                } else {
                                    Log.w(TAG,"Decoded path is empty for route segment.");
                                }
                            } else {
                                Log.w(TAG,"No overview_polyline found in route object.");
                            }
                        } else {
                            Log.w(TAG, "No routes found in Directions API response.");
                            // Optionally show a message to the user that route couldn't be drawn
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSONException parsing directions response", e);
                    } catch (Exception e) {
                        Log.e(TAG, "Unexpected error processing directions response", e);
                    }
                },
                error -> Log.e(TAG, "Directions API VolleyError: " + error.getMessage())
        );
        configureAndAddRequest(jsonObjectRequest); // Use common config/add method
    }

    private String getDirectionsUrl(LatLng origin, LatLng destination) {
        String apiKey = getString(R.string.google_maps_key);
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_API_KEY")) {
            Log.e(TAG,"DIRECTIONS API KEY IS MISSING/INVALID in strings.xml");
            showToast("Map Directions API Key Missing");
            return ""; // Return empty URL if key missing
        }
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        String str_dest = "destination=" + destination.latitude + "," + destination.longitude;
        String parameters = str_origin + "&" + str_dest + "&key=" + apiKey;
        return "https://maps.googleapis.com/maps/api/directions/json?" + parameters;
    }

    // Standard polyline decoding function
    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b = 0, shift = 0, result = 0;
            do {
                if (index >= len) break; // Avoid IndexOutOfBoundsException
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            if (index > len && b < 0x20) break; // Check if loop terminated correctly

            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                if (index >= len) break; // Avoid IndexOutOfBoundsException
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            if (index > len && b < 0x20) break; // Check if loop terminated correctly

            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((double) lat / 1E5, (double) lng / 1E5);
            poly.add(p);
        }
        return poly;
    }


    private void navigateToDestination() {
        if (mMap == null) {
            showToast("Map not ready for navigation.");
            return;
        }
        // Re-validate essential data just before navigating
        if (pickupLat == null || pickupLat.equals("0.0")) {
            showToast("Pickup location missing."); return;
        }

        LatLng targetLatLng = null;
        String targetLabel = "Destination";

        if (currentApiStatus != null && currentApiStatus.equals(STATUS_START_TRIP)) {
            // During the trip, navigate to the current drop target
            if (multipleDrops > 0 && !allDropLatLngs.isEmpty()) {
                if (currentDropIndex < allDropLatLngs.size()) {
                    targetLatLng = allDropLatLngs.get(currentDropIndex);
                    targetLabel = "Drop " + (currentDropIndex + 1);
                } else {
                    Log.w(TAG,"Navigation requested in Start Trip state, but drop index is out of bounds.");
                    showToast("Cannot determine next drop destination."); return;
                }
            } else {
                // Single drop
                if (destinationLat != null && !destinationLat.equals("0.0")) {
                    targetLatLng = new LatLng(Double.parseDouble(destinationLat), Double.parseDouble(destinationLng));
                    targetLabel = "Drop Location";
                } else {
                    Log.w(TAG,"Navigation requested for single drop, but destination coords missing.");
                    showToast("Drop location not available."); return;
                }
            }
        } else {
            // Before trip starts, navigate to pickup
            targetLatLng = new LatLng(Double.parseDouble(pickupLat), Double.parseDouble(pickupLng));
            targetLabel = "Pickup Location";
        }

        if (targetLatLng == null) {
            Log.e(TAG,"Target LatLng for navigation is null.");
            showToast("Could not determine navigation target.");
            return;
        }

        Log.d(TAG, "Starting navigation to: " + targetLabel + " at " + targetLatLng);

        // Create Google Maps navigation intent
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + targetLatLng.latitude + "," + targetLatLng.longitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        try {
            startActivity(mapIntent);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "Google Maps app not installed.");
            showToast("Google Maps app not installed. Opening in browser.");
            // Fallback to browser directions
            String browserUri = "https://www.google.com/maps/dir/?api=1&destination=" + targetLatLng.latitude + "," + targetLatLng.longitude;
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(browserUri)));
            } catch (Exception browserException){
                Log.e(TAG, "Could not open maps in browser.", browserException);
                showToast("Could not open maps in browser.");
            }
        }
    }


    // --- Calling Logic ---

    private void handleCallClick() {
        try {
            String phoneNumber = null;
            String contactName = "Contact"; // Default name

            if (currentApiStatus == null || currentApiStatus.isEmpty()) {
                showToast("Ride status unclear, cannot determine who to call.");
                return;
            }

            // Determine who to call based on current status and drop index
            if (currentApiStatus.equals(STATUS_START_TRIP) || currentApiStatus.startsWith("Reached Drop") || currentApiStatus.equals(STATUS_MAKE_PAYMENT)) {
                // During trip or at drops: Call the receiver for the CURRENT target drop
                if (multipleDrops > 0 && dropContactsArray != null && currentDropIndex < dropContactsArray.length()) {
                    try {
                        JSONObject currentContact = dropContactsArray.getJSONObject(currentDropIndex);
                        phoneNumber = currentContact.optString("mobile", null);
                        contactName = currentContact.optString("name", "Drop " + (currentDropIndex + 1) + " Contact");
                        if (phoneNumber != null && (phoneNumber.equalsIgnoreCase("N/A") || phoneNumber.trim().isEmpty())) phoneNumber = null;
                    } catch (JSONException e) {
                        Log.e(TAG, "Error getting drop contact info for call: " + e.getMessage());
                    }
                }

                // Fallback to primary receiver if no specific drop contact or single drop
                if (phoneNumber == null) {
                    phoneNumber = receiverNumber;
                    contactName = (receiverName != null && !receiverName.isEmpty()) ? receiverName : "Receiver";
                }

            } else if (currentApiStatus.equals(STATUS_DRIVER_ACCEPTED) ||
                    currentApiStatus.equals(STATUS_DRIVER_ARRIVED) ||
                    currentApiStatus.equals(STATUS_OTP_VERIFIED)) {
                // Before trip starts: Call Sender
                phoneNumber = senderNumber;
                contactName = (senderName != null && !senderName.isEmpty()) ? senderName : "Sender";
            } else {
                Log.w(TAG, "Call button clicked in unexpected status: " + currentApiStatus);
                showToast("Cannot make call at this stage.");
                return;
            }


            if (phoneNumber == null || phoneNumber.trim().isEmpty() || phoneNumber.equalsIgnoreCase("N/A")) {
                showToast("Phone number not available for " + contactName);
                return;
            }

            Log.d(TAG, "Attempting to dial: " + contactName + " at " + phoneNumber);

            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:" + phoneNumber.trim())); // Trim whitespace

            if (callIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(callIntent);
            } else {
                showToast("No app available to make calls");
            }

        } catch (Exception e) {
            Log.e(TAG,"Error during handleCallClick", e);
            showToast("Unable to initiate call.");
        }
    }


    // --- Utilities & Lifecycle ---

    public String getFormattedBookingTiming(String bookingTiming) {
        if (bookingTiming == null || bookingTiming.isEmpty()) return "N/A";
        try {
            // Assuming bookingTiming is epoch seconds as a string
            double epochSeconds = Double.parseDouble(bookingTiming);
            long milliseconds = (long) (epochSeconds * 1000);
            Date date = new Date(milliseconds);
            // Example format: Tue, Aug 27, 03:45 PM
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd, hh:mm a", Locale.getDefault());
            return sdf.format(date);
        } catch (NumberFormatException e) {
            Log.e(TAG,"Error parsing booking timing (expected epoch seconds): " + bookingTiming, e);
            return "Invalid Time"; // Or return original string if parsing fails
        } catch (Exception e) {
            Log.e(TAG,"Error formatting booking timing: " + bookingTiming, e);
            return "Time Error";
        }
    }

    private void showLoading(String message) {
        if (!isFinishing() && progressDialog != null) {
            progressDialog.setMessage(message);
            if (!progressDialog.isShowing()){
                progressDialog.show();
            }
        }
    }

    private void hideLoading() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void handleVolleyError(VolleyError error) {
        String message = "An unknown network error occurred.";
        if (error instanceof NoConnectionError) {
            message = "No internet connection. Please check your network.";
        } else if (error instanceof TimeoutError) {
            message = "Request timed out. Please try again.";
        } else if (error.networkResponse != null) {
            // Server responded with an error status code
            int statusCode = error.networkResponse.statusCode;
            message = "Server error (" + statusCode + "). Please try again later.";
            if (statusCode == 401 || statusCode == 403) {
                message = "Authentication error. Please login again.";
                // Consider redirecting to login
            } else if (statusCode == 404) {
                message = "Resource not found or booking may be cancelled.";
                // Maybe trigger handleNoLiveRideFound?
            } else if (statusCode >= 500) {
                message = "Server error (" + statusCode + "). Please contact support if it persists.";
            }
            // You could potentially parse the error response body here for more details
            // String responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
            // Log.e(TAG, "VolleyError Body: " + responseBody);
        } else {
            // Other Volley errors (e.g., ParseError, AuthFailureError)
            Log.e(TAG, "Unhandled VolleyError: " + error.toString());
            message = "An unexpected error occurred. Please try again.";
        }
        showToast(message);
    }


    private void showToast(String message) {
        // Ensure toast is shown on the main thread
        mainThreadHandler.post(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show());
    }
    private void showError(String message) {
        showToast(message); // Use the same toast mechanism
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed called. isFromFCM: " + isFromFCM);
        if (isFromFCM) {
            // If opened from FCM notification, always go to Home screen
            navigateToHome();
        } else {
            // Allow normal back press behavior otherwise
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        // Clean up resources
        if (serviceRestartReceiver != null) {
            try {
                unregisterReceiver(serviceRestartReceiver);
                serviceRestartReceiver = null;
                Log.d(TAG,"Unregistered serviceRestartReceiver.");
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Receiver not registered or already unregistered.", e);
            }
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        executorService.shutdown(); // Shutdown background thread executor
        mainThreadHandler.removeCallbacksAndMessages(null); // Clear pending main thread tasks

        // Decide whether to stop location updates
        // If the ride is ongoing based on isLiveRide flag, maybe keep it running?
        // If navigating away definitively, stop it.
        // if (!preferenceManager.getBooleanValue("isLiveRide", false)) {
        //      stopLocationUpdates();
        // }

    }


    // --- Location Service Management ---

    private void startLocationUpdates() {
        Log.d(TAG, "Attempting to start LocationUpdateService.");
        LocationUpdateService.isStoppedManually = false; // Allow service to run
        Intent serviceIntent = new Intent(this, LocationUpdateService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Log.i(TAG, "LocationUpdateService started.");
        } catch (Exception e) {
            Log.e(TAG, "Could not start LocationUpdateService", e);
            showToast("Could not start location updates.");
        }


        // Register broadcast receiver for service restart ONLY if it doesn't exist
        if (serviceRestartReceiver == null) {
            serviceRestartReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, "Received RESTART_SERVICE broadcast. isStoppedManually: " + LocationUpdateService.isStoppedManually);
                    if ("com.vtpartnertranspvtltd.vt_partner.RESTART_SERVICE".equals(intent.getAction()) && !LocationUpdateService.isStoppedManually) {
                        Log.i(TAG, "Restarting LocationUpdateService due to broadcast.");
                        startLocationUpdates(); // Call the start method again
                    }
                }
            };

            // Use ContextCompat for broader compatibility if needed, though registerReceiver is standard
            registerReceiver(serviceRestartReceiver, new IntentFilter("com.vtpartnertranspvtltd.vt_partner.RESTART_SERVICE"));
            Log.d(TAG,"Registered serviceRestartReceiver.");
        }
    }

    private void stopLocationUpdates() {
        Log.d(TAG, "Attempting to stop LocationUpdateService.");
        LocationUpdateService.isStoppedManually = true; // Prevent automatic restarts
        Intent serviceIntent = new Intent(this, LocationUpdateService.class);
        Intent floatingIntent = new Intent(this, FloatingWindowService.class);
        try {
            stopService(serviceIntent);
            stopService(floatingIntent);
            Log.i(TAG, "LocationUpdateService and FloatingWindowService stopped.");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping services", e);
        }
    }
}