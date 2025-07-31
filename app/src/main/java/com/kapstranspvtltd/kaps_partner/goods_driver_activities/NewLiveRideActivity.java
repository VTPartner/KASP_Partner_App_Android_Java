package com.kapstranspvtltd.kaps_partner.goods_driver_activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bumptech.glide.Glide;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.kapstranspvtltd.kaps_partner.common_activities.ProximityNotificationManager;
import com.kapstranspvtltd.kaps_partner.common_activities.adapters.CancelReasonAdapter;
import com.kapstranspvtltd.kaps_partner.common_activities.models.CancelReason;
import com.kapstranspvtltd.kaps_partner.fcm.AccessToken;
import com.kapstranspvtltd.kaps_partner.goods_driver_activities.helper.UnloadingTimerManager;
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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;

public class NewLiveRideActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "NewLiveRideActivity";

    // --- Booking Status Constants ---
    private static final String STATUS_DRIVER_ACCEPTED = "Driver Accepted";
    private static final String STATUS_DRIVER_ARRIVED = "Driver Arrived";
    private static final String STATUS_OTP_VERIFIED = "Otp Verified";
    private static final String STATUS_START_TRIP = "Start Trip";
    // Specific status for API if backend uses them
//    private static final String STATUS_REACHED_DROP_1 = "Reached Drop Location 1";
//    private static final String STATUS_REACHED_DROP_2 = "Reached Drop Location 2";
//    private static final String STATUS_REACHED_DROP_3 = "Reached Drop Location 3";

    private static final String STATUS_REACHED_DROP_PREFIX = "Reached Drop Location ";
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

    private static final float ARRIVAL_THRESHOLD_METERS = 150; // Show arrived button within 100 meters

    private ProximityNotificationManager proximityManager;

    private static final float DROP_THRESHOLD_METERS = 150; // Show payment button within 100 meters

    private boolean isWithinDropThreshold = false;
    private BroadcastReceiver locationUpdateReceiver;

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

    private Double walletAmount = 0.0;
    private ProgressDialog progressDialog;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    // Ride Details
    int minimumWaitingTime = 0;
    double penaltyCharges = 0;
    double penaltyAmountFromApi = 0.0; // Store penalty amount from API
    String vehicleMapImage = "";
    double hikePrice = 0;
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

    // --- Timer Persistence Helpers ---
    private static final String PREF_LOADING_OVERTIME_START = "loading_overtime_start_";
    private static final String PREF_UNLOADING_OVERTIME_START = "unloading_overtime_start_";
    private static final String PREF_LOADING_TIMER_START = "loading_timer_start_";
    private static final String PREF_UNLOADING_TIMER_START = "unloading_timer_start_";

    private void saveTimerState(String key, long value) {
        preferenceManager.saveLongValue(key + assignedBookingId, value);
    }
    private long getTimerState(String key) {
        return preferenceManager.getLongValue(key + assignedBookingId, 0);
    }
    private void clearTimerState(String key) {
        preferenceManager.removeValue(key + assignedBookingId);
    }

    // --- Timer Utility Methods for Waiting Penalty Info ---
    private CountDownTimer loadingCountDownTimer;
    private CountDownTimer unloadingCountDownTimer;

    private void startLoadingCountdown(long millisLeft) {
        if (loadingCountDownTimer != null) loadingCountDownTimer.cancel();
        loadingCountDownTimer = new CountDownTimer(millisLeft, 1000) {
            public void onTick(long millisUntilFinished) {
                binding.loadingCountdown.setText("Loading Timer: " + formatTime(millisUntilFinished));
            }
            public void onFinish() {
                binding.loadingCountdown.setText("Loading Timer: Overage");
            }
        }.start();
    }

    private void startLoadingOverageTimer(long overageMillis) {
        if (loadingCountDownTimer != null) loadingCountDownTimer.cancel();
        loadingCountDownTimer = new CountDownTimer(Long.MAX_VALUE, 1000) {
            long currentOverage = overageMillis;
            public void onTick(long millisUntilFinished) {
                currentOverage += 1000;
                binding.loadingCountdown.setText("Loading Timer: +" + formatTime(currentOverage));
            }
            public void onFinish() {}
        }.start();
    }

    private void stopLoadingTimer() {
        if (loadingCountDownTimer != null) {
            loadingCountDownTimer.cancel();
            loadingCountDownTimer = null;
        }
    }

    private void startUnloadingCountdown(long millisLeft) {
        if (unloadingCountDownTimer != null) unloadingCountDownTimer.cancel();
        unloadingCountDownTimer = new CountDownTimer(millisLeft, 1000) {
            public void onTick(long millisUntilFinished) {
                binding.unloadingCountdown.setText("Unloading Timer: " + formatTime(millisUntilFinished));
            }
            public void onFinish() {
                binding.unloadingCountdown.setText("Unloading Timer: Overage");
            }
        }.start();
    }

    private void startUnloadingOverageTimer(long overageMillis) {
        if (unloadingCountDownTimer != null) unloadingCountDownTimer.cancel();
        unloadingCountDownTimer = new CountDownTimer(Long.MAX_VALUE, 1000) {
            long currentOverage = overageMillis;
            public void onTick(long millisUntilFinished) {
                currentOverage += 1000;
                binding.unloadingCountdown.setText("Unloading Timer: +" + formatTime(currentOverage));
            }
            public void onFinish() {}
        }.start();
    }

    private void stopUnloadingTimer() {
        if (unloadingCountDownTimer != null) {
            unloadingCountDownTimer.cancel();
            unloadingCountDownTimer = null;
        }
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, secs);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNewLiveRideBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Log.d(TAG, "onCreate");

        proximityManager = new ProximityNotificationManager(this);
        initializeViews();

        setupClickListeners();
        getCurrentBookingIdDetails(); // This will trigger fetchBookingDetails if ID is found
        setupLocationUpdateReceiver();
        startLocationUpdates(); // Start location service regardless

        binding.navigateBtn.setOnClickListener(v -> navigateToDestination());
    }

    private void checkProximityNotifications(String statusType) {
        String bookingId = String.valueOf(assignedBookingId);
        if (bookingId.isEmpty()) return;

        String url = APIClient.baseUrl + "check_location_proximity";

        try {
            JSONObject params = new JSONObject();
            params.put("booking_id", bookingId);
            params.put("status_type", statusType);
            params.put("server_token", AccessToken.getAccessToken());

            // Add current drop index for drop notifications
            if (statusType.startsWith("Drop_")) {
                params.put("current_drop_index", currentDropIndex);
            }

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    params,
                    response -> {
                        if (response.optBoolean("success", false)) {
                            // Mark notification as sent
                            if (statusType.equals("Pickup")) {
                                preferenceManager.saveBooleanValue("isPickupNotificationSent", true);
                                proximityManager.markNotificationSent(bookingId, "pickup", 0);
                            } else if (statusType.startsWith("Drop_")) {
                                proximityManager.markNotificationSent(bookingId, "drop", currentDropIndex);
                            }
                            Log.d(TAG, "Proximity notification sent: " + statusType);
                        } else {
                            Log.w(TAG, "Failed to send proximity notification: " +
                                    response.optString("message", "Unknown error"));
                        }
                    },
                    error -> {
                        Log.e(TAG, "Error sending proximity notification: " + error.getMessage());
                        handleVolleyError(error);
                    }
            );

            configureAndAddRequest(request);

        } catch (Exception e) {
            Log.e(TAG, "Error preparing proximity notification", e);
        }
    }


    private void setupLocationUpdateReceiver() {
        locationUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() != null && intent.getAction().equals("driver_location_update")) {
                    double driverLat = intent.getDoubleExtra("latitude", 0.0);
                    double driverLng = intent.getDoubleExtra("longitude", 0.0);
                    checkProximityToPickup(driverLat, driverLng);
                }
            }
        };
        // Create intent filter with export flag
        IntentFilter filter = new IntentFilter("driver_location_update");
        registerReceiver(locationUpdateReceiver, filter, Context.RECEIVER_EXPORTED);
//        registerReceiver(locationUpdateReceiver, new IntentFilter("driver_location_update"));
    }

  /*  private void checkProximityToPickup(double driverLat, double driverLng) {
        Location driverLocation = new Location("driver");
        driverLocation.setLatitude(driverLat);
        driverLocation.setLongitude(driverLng);

        // Check pickup proximity
        if (STATUS_DRIVER_ACCEPTED.equals(currentApiStatus) &&
                pickupLat != null && !pickupLat.equals("0.0")) {

            Location pickupLocation = new Location("pickup");
            pickupLocation.setLatitude(Double.parseDouble(pickupLat));
            pickupLocation.setLongitude(Double.parseDouble(pickupLng));

            float distanceToPickup = driverLocation.distanceTo(pickupLocation);

            mainThreadHandler.post(() -> {
                if (distanceToPickup <= ARRIVAL_THRESHOLD_METERS) {
                    binding.btnArrived.setVisibility(View.VISIBLE);
                } else {
                    binding.btnArrived.setVisibility(View.GONE);
                }
            });
        }

        // Check drop location proximity
        // Inside checkProximityToPickup method, in the drop location proximity check section
        if (STATUS_START_TRIP.equals(currentApiStatus)) {
            LatLng targetDrop = null;

            if (multipleDrops > 0 && !allDropLatLngs.isEmpty() && currentDropIndex < allDropLatLngs.size()) {
                targetDrop = allDropLatLngs.get(currentDropIndex);
            } else if (destinationLat != null && !destinationLat.equals("0.0")) {
                targetDrop = new LatLng(
                        Double.parseDouble(destinationLat),
                        Double.parseDouble(destinationLng)
                );
            }

            if (targetDrop != null) {
                Location dropLocation = new Location("drop");
                dropLocation.setLatitude(targetDrop.latitude);
                dropLocation.setLongitude(targetDrop.longitude);

                float distanceToDrop = driverLocation.distanceTo(dropLocation);

                mainThreadHandler.post(() -> {
                    isWithinDropThreshold = distanceToDrop <= DROP_THRESHOLD_METERS;
                    if (isWithinDropThreshold) {
                        if (nextAction != null &&
                                (nextAction.startsWith(ACTION_CONFIRM_REACHED_DROP) ||
                                        (nextAction.equals(ACTION_SEND_PAYMENT_DETAILS) && !STATUS_MAKE_PAYMENT.equals(currentApiStatus)))) {
                            binding.btnSendPaymentDetails.setVisibility(View.VISIBLE);
                            binding.btnSendPaymentDetails.setText(nextAction);
                        }
                    } else {
                        if (nextAction != null &&
                                (nextAction.startsWith(ACTION_CONFIRM_REACHED_DROP) ||
                                        (nextAction.equals(ACTION_SEND_PAYMENT_DETAILS) && !STATUS_MAKE_PAYMENT.equals(currentApiStatus)))) {
//                            binding.btnSendPaymentDetails.setVisibility(View.VISIBLE);
//                            binding.btnSendPaymentDetails.setText(nextAction);
                            binding.btnSendPaymentDetails.setVisibility(View.GONE);
//                            showToast("Get closer to drop location to mark delivery");
                        }
                    }
                });
            }
        }
    }

  */

    private void checkProximityToPickup(double driverLat, double driverLng) {
        Location driverLocation = new Location("driver");
        driverLocation.setLatitude(driverLat);
        driverLocation.setLongitude(driverLng);

        String localCurrentStatus = currentApiStatus;

        // Handle pickup proximity
        if (STATUS_DRIVER_ACCEPTED.equals(localCurrentStatus)) {
            handlePickupProximity(driverLocation);
        }

        // Handle drop location proximity for both START_TRIP and REACHED_DROP states
        if (STATUS_START_TRIP.equals(localCurrentStatus) ||
                localCurrentStatus.startsWith(STATUS_REACHED_DROP_PREFIX)) {
            handleDropProximity(driverLocation);
        }
    }

    private void handlePickupProximity(Location driverLocation) {
        if (pickupLat == null || pickupLat.equals("0.0")) return;

        float distanceToPickup = calculateDistance(
                driverLocation.getLatitude(), driverLocation.getLongitude(),
                Double.parseDouble(pickupLat), Double.parseDouble(pickupLng)
        );

        boolean shouldShowArrived = distanceToPickup <= ARRIVAL_THRESHOLD_METERS;
        boolean isPickupNotificationSent = preferenceManager.getBooleanValue("isPickupNotificationSent", false);

        mainThreadHandler.post(() -> {
            binding.btnArrived.setVisibility(shouldShowArrived ? View.VISIBLE : View.GONE);
            if (shouldShowArrived && !isPickupNotificationSent) {
                checkProximityNotifications("Pickup");
            }
        });
    }

    private void handleDropProximity(Location driverLocation) {
        LatLng targetDrop = getTargetDropLocation();
        if (targetDrop == null) return;

        float distanceToDrop = calculateDistance(
                driverLocation.getLatitude(), driverLocation.getLongitude(),
                targetDrop.latitude, targetDrop.longitude
        );

        boolean newThresholdState = distanceToDrop <= DROP_THRESHOLD_METERS;
        if (newThresholdState != isWithinDropThreshold) {
            isWithinDropThreshold = newThresholdState;
            updateDropButtonVisibility();

            // Send notification if within threshold and not sent yet
            if (isWithinDropThreshold) {
                String bookingId = String.valueOf(assignedBookingId);
                boolean hasNotificationBeenSent;

                if (multipleDrops > 0) {
                    hasNotificationBeenSent = proximityManager.hasNotificationBeenSent(
                            bookingId, "drop", currentDropIndex);
                } else {
                    hasNotificationBeenSent = proximityManager.hasNotificationBeenSent(
                            bookingId, "drop", 0);
                }

                if (!hasNotificationBeenSent) {
                    String dropNotificationType;
                    if (multipleDrops > 0) {
                        dropNotificationType = "Drop_" + (currentDropIndex + 1);
                    } else {
                        dropNotificationType = "Drop_1";
                    }
                    checkProximityNotifications(dropNotificationType);
                }
            }
        }
    }



    private LatLng getTargetDropLocation() {
        if (multipleDrops > 0 && !allDropLatLngs.isEmpty() &&
                currentDropIndex < allDropLatLngs.size()) {
            return allDropLatLngs.get(currentDropIndex);
        } else if (destinationLat != null && !destinationLat.equals("0.0")) {
            return new LatLng(
                    Double.parseDouble(destinationLat),
                    Double.parseDouble(destinationLng)
            );
        }
        return null;
    }

    // Helper method to calculate distance faster
    private float calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        // Using the Haversine formula
        double earthRadius = 6371000; // meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return (float) (earthRadius * c);
    }

    // Helper method to get target drop location


    // Helper method to update drop button visibility
    private void updateDropButtonVisibility() {
        mainThreadHandler.post(() -> {
            if (nextAction == null) return;

            boolean shouldShowButton = false;
            String buttonText = "";

            // Determine if button should be shown and what text to display
            if (nextAction.startsWith(ACTION_CONFIRM_REACHED_DROP)) {
                // Show reached drop button only when within proximity threshold
                if (isWithinDropThreshold) {
                    shouldShowButton = true;
                    // For single drop, show "Reached Drop Location 1"
                    if (multipleDrops == 0 || allDropLatLngs.size() <= 1) {
                        buttonText = "Reached Drop Location 1";
                    } else {
                        // For multiple drops, show "Reached Drop X"
                        buttonText = nextAction;
                    }
                }
            } else if (ACTION_SEND_PAYMENT_DETAILS.equals(nextAction)) {
                // Show "Send Payment Details" button when within proximity threshold
                // This is for the last drop after unloading timer starts
                if (isWithinDropThreshold) {
                    shouldShowButton = true;
                    buttonText = "Send Payment Details";
                }
            }

            // Update button visibility and text
            binding.btnSendPaymentDetails.setVisibility(shouldShowButton ? View.VISIBLE : View.GONE);
            if (shouldShowButton) {
                binding.btnSendPaymentDetails.setText(buttonText);
            }

            // Show toast if moving out of threshold (only for reached drop actions, not payment)
            if (!shouldShowButton && nextAction.startsWith(ACTION_CONFIRM_REACHED_DROP)) {
                showToast("Get closer to drop location to mark delivery");
            }
        });
    }




    /*private void updateDropButtonVisibility() {
        boolean isDropNotificationSent = preferenceManager.getBooleanValue("isDropNotificationSent", false);
        mainThreadHandler.post(() -> {
            if (nextAction != null &&
                    (nextAction.startsWith(ACTION_CONFIRM_REACHED_DROP) ||
                            (nextAction.equals(ACTION_SEND_PAYMENT_DETAILS) &&
                                    !STATUS_MAKE_PAYMENT.equals(currentApiStatus)))) {

                binding.btnSendPaymentDetails.setVisibility(
                        isWithinDropThreshold ? View.VISIBLE : View.GONE
                );
                if(isWithinDropThreshold && isDropNotificationSent){
                    if(multipleDrops>0) {
                        checkProximityNotifications("Drop_" + currentDropIndex);
                    } else{
                        checkProximityNotifications("Drop_1");
                    }
                }
                if (isWithinDropThreshold) {
                    binding.btnSendPaymentDetails.setText(nextAction);
                }
            }
        });
    }*/
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
        binding.txtTripDetails.setOnClickListener(v -> toggleTripDetails());
        binding.cancelTripBtn.setOnClickListener(v -> showCancelBookingBottomSheet());

        setupStatusButtonListeners();
    }

    private boolean isTripDetailsExpanded = false;
    private void toggleTripDetails() {
        isTripDetailsExpanded = !isTripDetailsExpanded;

        // Show/hide cancel button without rotation animation
        if (isTripDetailsExpanded) {
            binding.cancelTripBtn.setVisibility(View.VISIBLE);
            binding.cancelTripBtn.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(200)
                    .start();
        } else {
            binding.cancelTripBtn.animate()
                    .alpha(0f)
                    .translationY(-binding.cancelTripBtn.getHeight())
                    .setDuration(200)
                    .withEndAction(() -> binding.cancelTripBtn.setVisibility(View.GONE))
                    .start();
        }
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
                            preferenceManager.saveBooleanValue("isOnLiveRide",true);
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

    private UnloadingTimerManager timerManager;



    private void updateRideDetails(JSONObject rideDetails) {
        try {
            // --- Basic Details ---
            walletAmount = rideDetails.getDouble("wallet_amount_used");
            currentApiStatus = rideDetails.getString("booking_status"); // Store the status from API
            minimumWaitingTime = rideDetails.getInt("minimum_waiting_time");
            penaltyCharges = rideDetails.getDouble("penalty_charge");
            penaltyAmountFromApi = rideDetails.optDouble("penalty_amount", 0.0); // Get penalty amount from API
            vehicleMapImage = rideDetails.getString("vehicle_map_image");
            hikePrice = rideDetails.getDouble("hike_price");
            System.out.println("minimumWaitingTime::"+minimumWaitingTime);
            System.out.println("penaltyCharges::"+penaltyCharges);
            System.out.println("penaltyAmountFromApi::"+penaltyAmountFromApi);
            System.out.println("currentApiStatus::"+currentApiStatus);


            bookingIdStr = rideDetails.getString("booking_id");
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
            Log.d(TAG, "Drop Locations String: " + rideDetails.optString("drop_locations", "null"));
            Log.d(TAG, "Drop Contacts String: " + rideDetails.optString("drop_contacts", "null"));

            dropLocationsArray = null; // Reset before parsing
            dropContactsArray = null;
            allDropLatLngs.clear();
            allDropAddresses.clear();

            // Check if we have drop locations (even for single drops)
            String dropLocationsStr = rideDetails.optString("drop_locations");
            String dropContactsStr = rideDetails.optString("drop_contacts");
            
            // If we have drop locations but multiple_drops is 0, treat as single drop
            if (multipleDrops == 0 && dropLocationsStr != null && !dropLocationsStr.isEmpty() && !dropLocationsStr.equals("[]")) {
                Log.d(TAG, "Single drop with drop_locations data found");
                // Keep multipleDrops as 0, but we'll use the drop_locations data for consistency
            }

            if (multipleDrops > 0 || (dropLocationsStr != null && !dropLocationsStr.isEmpty() && !dropLocationsStr.equals("[]"))) {

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

            if (STATUS_DRIVER_ARRIVED.equals(currentApiStatus)) {
                binding.txtTripDetails.setVisibility(View.VISIBLE);
            } else {
                binding.txtTripDetails.setVisibility(View.GONE);
                binding.cancelTripBtn.setVisibility(View.GONE); // Hide cancel button when not in ARRIVED state
            }

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
        
        // Update drop button visibility when status changes to ensure proper button display
        if (STATUS_MAKE_PAYMENT.equals(currentApiStatus) || 
            (nextAction != null && (nextAction.startsWith(ACTION_CONFIRM_REACHED_DROP) || 
                ACTION_SEND_PAYMENT_DETAILS.equals(nextAction)))) {
            updateDropButtonVisibility();
        }

            // --- Waiting Penalty Info (Timer & Penalty UI) ---
            showWaitingPenaltyInfo(rideDetails.optJSONObject("waiting_time_info"), rideDetails);

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

    private void showWaitingPenaltyInfo(JSONObject waitingInfo, JSONObject rideDetails) {
        System.out.println("waitingInfo::"+waitingInfo);
        
        // Debug: Print rideDetails to see what's available
        if (rideDetails != null) {
            System.out.println("rideDetails unloading start times: " + rideDetails.optString("unloading_wait_start_times", "null"));
            System.out.println("rideDetails unloading end times: " + rideDetails.optString("unloading_wait_end_times", "null"));
            System.out.println("rideDetails status: " + currentApiStatus);
            System.out.println("minimumWaitingTime: " + minimumWaitingTime);
        }
        
        // Debug: Print waitingInfo keys to see what's available
        if (waitingInfo != null) {
            System.out.println("waitingInfo keys:");
            Iterator<String> keys = waitingInfo.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (key.contains("unloading") || key.contains("wait")) {
                    System.out.println("  Key: " + key + " = " + waitingInfo.opt(key));
                }
            }
        }
        
        if (waitingInfo == null) {
            binding.waitingPenaltyContainer.setVisibility(View.GONE);
            return;
        }
        binding.waitingPenaltyContainer.setVisibility(View.VISIBLE);

        double loadingWait = waitingInfo.optDouble("loading_wait", 0);
        double allowedLoading = waitingInfo.optDouble("allowed_loading_wait", 0);
        double loadingPenalty = waitingInfo.optDouble("loading_penalty", 0);
        double totalPenalty = waitingInfo.optDouble("total_penalty", 0);
        JSONArray unloadingWaits = waitingInfo.optJSONArray("unloading_waits");
        System.out.println("unloadingWaits::"+unloadingWaits);
        // --- Loading Timer ---
        long loadingStart = rideDetails.optLong("loading_wait_start_time", 0);
        long loadingEnd = rideDetails.optLong("loading_wait_end_time", 0);
        if (loadingStart > 0 && loadingEnd == 0) {
            long now = System.currentTimeMillis();
            long elapsed = (now - loadingStart * 1000) / 1000; // seconds
            long allowedSeconds = (long) (allowedLoading * 60);
            if (elapsed < allowedSeconds) {
                long left = allowedSeconds - elapsed;
                startLoadingCountdown(left * 1000);
            } else {
                long overage = elapsed - allowedSeconds;
                startLoadingOverageTimer(overage * 1000);
            }
        } else {
            stopLoadingTimer();
            if (loadingWait >= allowedLoading) {
                binding.loadingCountdown.setText("Loading Timer: Overage");
            } else {
                binding.loadingCountdown.setText("Loading Timer: Not running");
            }
        }

        // --- Unloading Timer (for current drop) ---
        // Get unloading data from rideDetails (which comes from the main API response)
        JSONArray unloadingStarts = null;
        JSONArray unloadingEnds = null;
        
        try {
            // Try to get from rideDetails first - parse the string values
            String unloadingStartsStr = rideDetails.optString("unloading_wait_start_times", "");
            String unloadingEndsStr = rideDetails.optString("unloading_wait_end_times", "");
            
            if (unloadingStartsStr != null && !unloadingStartsStr.isEmpty()) {
                unloadingStarts = new JSONArray(unloadingStartsStr);
            }
            if (unloadingEndsStr != null && !unloadingEndsStr.isEmpty()) {
                unloadingEnds = new JSONArray(unloadingEndsStr);
            }
            
            // Fallback: If no unloading data from rideDetails, try to get from waitingInfo
            if (unloadingStarts == null && waitingInfo != null) {
                unloadingStarts = waitingInfo.optJSONArray("unloading_wait_start_times");
                unloadingEnds = waitingInfo.optJSONArray("unloading_wait_end_times");
                System.out.println("Fallback unloadingStarts from waitingInfo: " + unloadingStarts);
                System.out.println("Fallback unloadingEnds from waitingInfo: " + unloadingEnds);
            }
            
            System.out.println("Final unloadingStarts: " + unloadingStarts);
            System.out.println("Final unloadingEnds: " + unloadingEnds);
        } catch (Exception e) {
            System.out.println("Error parsing unloading arrays: " + e.getMessage());
        }
        
        // Find the current drop's unloading timer
        int currentDropUnloadingIndex = -1;
        if (unloadingStarts != null && unloadingStarts.length() > 0) {
            int startsCount = unloadingStarts.length();
            int endsCount = unloadingEnds != null ? unloadingEnds.length() : 0;

            // If we have more starts than ends, the last start is currently running
            if (startsCount > endsCount) {
                currentDropUnloadingIndex = endsCount; // This is the current drop being unloaded
            }
        }

        if (currentDropUnloadingIndex >= 0) {
            double allowedUnloading = 0;
            
            // Try to get allowed unloading time from unloading_waits array
            if (unloadingWaits != null && currentDropUnloadingIndex < unloadingWaits.length()) {
                JSONObject currentDropInfo = unloadingWaits.optJSONObject(currentDropUnloadingIndex);
                if (currentDropInfo != null) {
                    allowedUnloading = currentDropInfo.optDouble("allowed_wait", 0);
                }
            }
            
            // Fallback to general allowed unloading time
            if (allowedUnloading == 0) {
                allowedUnloading = waitingInfo.optDouble("allowed_unloading", 0);
            }
            
            // If still no allowed time, use minimum waiting time as fallback
            if (allowedUnloading == 0) {
                allowedUnloading = minimumWaitingTime;
            }
            
            double unloadingStartSec = unloadingStarts.optDouble(currentDropUnloadingIndex, 0);
            long nowSec = System.currentTimeMillis() / 1000;
            long elapsed = nowSec - (long)unloadingStartSec;
            
            if (elapsed < 0) elapsed = 0;
            
            if (allowedUnloading > 0) {
                long allowedSeconds = (long) (allowedUnloading * 60);
                if (elapsed < allowedSeconds) {
                    long left = allowedSeconds - elapsed;
                    startUnloadingCountdown(left * 1000);
                } else {
                    long overage = elapsed - allowedSeconds;
                    startUnloadingOverageTimer(overage * 1000);
                }
            } else {
                // If no allowed time specified, just show elapsed time
                startUnloadingCountdown(elapsed * 1000);
            }
        } else {
            // Fallback: If no unloading timer data but status indicates we should be unloading
            if (currentApiStatus.startsWith(STATUS_REACHED_DROP_PREFIX) && !STATUS_MAKE_PAYMENT.equals(currentApiStatus)) {
                // Start a basic unloading timer with minimum waiting time
                System.out.println("Starting fallback unloading timer with minimum waiting time: " + minimumWaitingTime);
                long allowedSeconds = (long) (minimumWaitingTime * 60);
                startUnloadingCountdown(allowedSeconds * 1000);
                
                // Also show a message to indicate this is a fallback timer
                binding.unloadingCountdown.setText("Unloading Timer: " + formatTime(allowedSeconds * 1000) + " (Fallback)");
            } else {
                stopUnloadingTimer();
                binding.unloadingCountdown.setText("Unloading Timer: Not running");
            }
        }

        // --- Info Texts ---
        String loadingText = String.format(
                Locale.getDefault(),
                "Loading Wait: %.1f / %.1f min (Penalty: ₹%.0f)",
                loadingWait, allowedLoading, loadingPenalty
        );
        binding.loadingWaitInfo.setText(loadingText);

        StringBuilder unloadingText = new StringBuilder();
        if (unloadingWaits != null && unloadingWaits.length() > 0) {
            for (int i = 0; i < unloadingWaits.length(); i++) {
                JSONObject dropInfo = unloadingWaits.optJSONObject(i);
                if (dropInfo != null) {
                    double wait = dropInfo.optDouble("wait_time", 0);
                    double allowed = dropInfo.optDouble("allowed_wait", 0);
                    double penalty = dropInfo.optDouble("penalty", 0);
                    unloadingText.append(String.format(
                            Locale.getDefault(),
                            "Drop %d: %.1f / %.1f min (Penalty: ₹%.0f)\n",
                            dropInfo.optInt("drop_index", i) + 1, wait, allowed, penalty
                    ));
                }
            }
        } else {
            unloadingText.append("No unloading wait recorded.");
        }
        binding.unloadingWaitInfo.setText(unloadingText.toString().trim());
        binding.penaltyInfo.setText(String.format(
                Locale.getDefault(),
                "Total Penalty: ₹%.0f", totalPenalty
        ));
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

    private String getReachedDropStatus(int dropNumber) {
        return STATUS_REACHED_DROP_PREFIX + dropNumber;
    }

    private int getDropNumberFromStatus(String status) {
        if (status != null && status.startsWith(STATUS_REACHED_DROP_PREFIX)) {
            try {
                return Integer.parseInt(status.substring(STATUS_REACHED_DROP_PREFIX.length()));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing drop number from status: " + status);
            }
        }
        return -1;
    }

    // Determines the next *action* the driver should take
    private void updateNextStatus(String currentApiStatus) {
        this.currentApiStatus = currentApiStatus;
        nextAction = "";

        if (currentApiStatus.startsWith(STATUS_REACHED_DROP_PREFIX)) {
            // Extract drop number from status
            int completedDropNumber = getDropNumberFromStatus(currentApiStatus);
            if (completedDropNumber > 0) {
                currentDropIndex = completedDropNumber; // Update current index based on completed drop
            }
        }

        switch (currentApiStatus) {
            case STATUS_DRIVER_ACCEPTED:
                binding.txtTripDetails.setVisibility(View.VISIBLE);
                nextAction = ACTION_CONFIRM_ARRIVAL;
                break;

            case STATUS_DRIVER_ARRIVED:
                nextAction = ACTION_VERIFY_OTP;
                break;

            case STATUS_OTP_VERIFIED:
                nextAction = ACTION_START_TRIP;
                break;

            case STATUS_START_TRIP:
                if (multipleDrops > 0 && !allDropLatLngs.isEmpty()) {
                    nextAction = ACTION_CONFIRM_REACHED_DROP + "1";
                } else {
                    nextAction = ACTION_CONFIRM_REACHED_DROP + "1";
                }
                break;

            case STATUS_MAKE_PAYMENT:
                nextAction = ACTION_END_TRIP;
                break;

            case STATUS_END_TRIP:
                nextAction = "";
                handleNoLiveRideFound();
                break;

            default:
                if (currentApiStatus.startsWith(STATUS_REACHED_DROP_PREFIX)) {
                    // Check if this is the last drop
                    int dropNumber = getDropNumberFromStatus(currentApiStatus);
                    if (dropNumber > 0) {
                        if ((multipleDrops > 0 && dropNumber >= allDropLatLngs.size()) || 
                            (multipleDrops <= 0 && dropNumber >= 1)) {
                            // This is the last drop - show "Send Payment Details" button
                            handleLastDropReached();
                        } else {
                            // More drops remaining
                            handleReachedDropStatus();
                        }
                    } else {
                        handleReachedDropStatus();
                    }
                } else {
                    Log.w(TAG, "Unhandled API Status: " + currentApiStatus);
                    if (totalPrice > 0) {
                        nextAction = ACTION_END_TRIP;
                    }
                }
                break;
        }

        Log.i(TAG, String.format("Status Update - API Status: '%s', Next Action: '%s', Drop Index: %d",
                currentApiStatus, nextAction, currentDropIndex));
    }

    private void handleReachedDropStatus() {
        if (multipleDrops > 0 && !allDropLatLngs.isEmpty()) {
            if (currentDropIndex < allDropLatLngs.size()) {
                // More drops remaining
                nextAction = ACTION_CONFIRM_REACHED_DROP + (currentDropIndex + 1);
            } else {
                // All drops completed - show the last drop button first
                nextAction = ACTION_CONFIRM_REACHED_DROP + allDropLatLngs.size();
            }
        } else {
            // Single drop scenario - show the drop button first
            nextAction = ACTION_CONFIRM_REACHED_DROP + "1";
        }
    }

    private void handleLastDropReached() {
        // When the last drop is reached, show "Send Payment Details" button
        // This will be called after unloading timer starts
        nextAction = ACTION_SEND_PAYMENT_DETAILS;
        updateStatusButtons();
        updateDropButtonVisibility();
    }

    // Updates button visibility and text based on 'nextAction'
    private void updateStatusButtons() {
        hideAllActionButtons(); // Start clean

        Log.d(TAG, "Updating status buttons based on nextAction: " + nextAction);

        if (ACTION_CONFIRM_ARRIVAL.equals(nextAction)) {
            // Not showing arrived button here - handled by proximity check
        } else if (ACTION_VERIFY_OTP.equals(nextAction)) {
            binding.btnVerifyOtp.setVisibility(View.VISIBLE);
        } else if (ACTION_START_TRIP.equals(nextAction)) {
            binding.btnSendTrip.setVisibility(View.VISIBLE);
        } else if (nextAction != null && nextAction.startsWith(ACTION_CONFIRM_REACHED_DROP)) {
            // Show reached drop button only when within proximity threshold
            if (isWithinDropThreshold) {
                binding.btnSendPaymentDetails.setVisibility(View.VISIBLE);
                binding.btnSendPaymentDetails.setText(nextAction);
            }
        } else if (ACTION_SEND_PAYMENT_DETAILS.equals(nextAction)) {
            // Show "Send Payment Details" button when within proximity threshold
            if (isWithinDropThreshold) {
                binding.btnSendPaymentDetails.setVisibility(View.VISIBLE);
                binding.btnSendPaymentDetails.setText("Send Payment Details");
            }
        } else if (ACTION_END_TRIP.equals(nextAction)) {
            binding.btnEndTrip.setVisibility(View.VISIBLE);
        } else {
            Log.d(TAG, "No specific action button to show for nextAction: " + nextAction);
        }
    }

   /* private void updateStatusButtons() {
        hideAllActionButtons(); // Start clean

        Log.d(TAG,"Updating status buttons based on nextAction: " + nextAction);

        if (ACTION_CONFIRM_ARRIVAL.equals(nextAction)) {
            // not showing arrived button here - it will be shown by proximity check
//            binding.btnArrived.setVisibility(View.VISIBLE);
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
    }*/


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
        Log.d(TAG, "handleReachedDropConfirmation - multipleDrops: " + multipleDrops + ", allDropLatLngs.size: " + allDropLatLngs.size() + ", currentDropIndex: " + currentDropIndex);
        
        // Handle single drop case
        if (multipleDrops <= 0) {
            // For single drop, use the main destination address
            new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                    .setTitle("Confirm Drop 1")
                    .setMessage("Have you reached Drop 1 at:\n" + dropAddress + "?")
                    .setPositiveButton("Yes, Reached", (dialog, which) -> {
                        // Always set status to "Reached Drop Location 1" first
                        // This allows unloading timer to start and then shows "Send Payment Details" button
                        String apiStatusUpdate = getReachedDropStatus(1);
                        updateRideStatus(apiStatusUpdate);
                    })
                    .setNegativeButton("No", null)
                    .show();
            return;
        }

        // Handle multiple drops case
        if (allDropLatLngs.isEmpty() || currentDropIndex >= allDropLatLngs.size()) {
            showToast("Invalid drop state");
            return;
        }

        int dropNumber = currentDropIndex + 1;
        String address = allDropAddresses.get(currentDropIndex);

        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle("Confirm Drop " + dropNumber)
                .setMessage("Have you reached Drop " + dropNumber + " at:\n" + address + "?")
                .setPositiveButton("Yes, Reached", (dialog, which) -> {
                    // Always set status to "Reached Drop Location X" first
                    // This allows unloading timer to start and then shows "Send Payment Details" button
                    String apiStatusUpdate = getReachedDropStatus(dropNumber);
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
//                    params.put("total_payment", totalPrice != null ? totalPrice.toString() : "0.0"); // Ensure totalPrice isn't null
                    params.put("customer_id", customerID != null ? customerID : ""); // Ensure customerID isn't null

                    // Calculate final amount including penalty if status is Make Payment
                    if (STATUS_MAKE_PAYMENT.equals(statusToUpdate)) {
                        // Use penalty amount from API response
                        double penaltyAmount = penaltyAmountFromApi;
                        double finalAmount = totalPrice + penaltyAmount;

                        params.put("total_payment", Math.round(finalAmount));
                        params.put("penalty_amount", Math.round(penaltyAmount));

                        Log.d(TAG, String.format("Sending payment details - Base: ₹%.2f, Penalty: ₹%.2f, Total: ₹%.2f",
                                totalPrice, penaltyAmount, finalAmount));
                    } else {
                        params.put("total_payment", totalPrice != null ? totalPrice.toString() : "0.0");
                    }

                    // --- Include current_drop_index if relevant ---
                    // Send the index of the drop that was *just completed* when confirming reach.
                    // Handle dynamic drop locations
                    if (statusToUpdate.startsWith(STATUS_REACHED_DROP_PREFIX) ||
                            statusToUpdate.equals(STATUS_MAKE_PAYMENT)) {

                        int completedDropIndex = -1;
                        if (statusToUpdate.equals(STATUS_MAKE_PAYMENT)) {
                            completedDropIndex = allDropLatLngs.size() - 1;
                        } else {
                            int dropNumber = getDropNumberFromStatus(statusToUpdate);
                            if (dropNumber > 0) {
                                completedDropIndex = dropNumber - 1;
                            }
                        }

                        if (completedDropIndex >= 0) {
                            params.put("current_drop_index", completedDropIndex);
                            Log.d(TAG, "Including completed_drop_index: " + completedDropIndex);
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
                            hideAllActionButtons();
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

        // Use penalty amount from API response
        double penaltyAmount = penaltyAmountFromApi;
        double totalAmount = totalPrice;
        double finalAmount = Math.round(totalPrice + penaltyAmount);

        DialogPaymentDetailsBinding dialogBinding = DialogPaymentDetailsBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setView(dialogBinding.getRoot())
                .setCancelable(false)
                .create();

        // Show wallet amount if used
        if(walletAmount > 0){
            dialogBinding.walletLyt.setVisibility(View.VISIBLE);
            dialogBinding.txtWalletAmtUsed.setText("₹" + Math.round(walletAmount));
        } else {
            dialogBinding.walletLyt.setVisibility(View.GONE);
        }

        // Base fare is the original amount before penalty
        double baseFare = totalAmount;

        // Add penalty to get final total
        if (penaltyAmount > 0) {
            totalAmount += penaltyAmount;
            dialogBinding.penaltyContainer.setVisibility(View.VISIBLE);
            dialogBinding.baseFareValue.setVisibility(View.VISIBLE);
            dialogBinding.penaltyValue.setText("₹" + Math.round(penaltyAmount));
            dialogBinding.baseFareValue.setText("Estimation Fare ₹" + Math.round(baseFare));
        } else {
            dialogBinding.penaltyContainer.setVisibility(View.GONE);
            dialogBinding.baseFareValue.setVisibility(View.GONE);
        }

        // Show final total amount
        dialogBinding.amountValue.setText("₹" + Math.round(totalAmount));

        dialogBinding.cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialogBinding.confirmButton.setOnClickListener(v -> {
            dialog.dismiss();
            String paymentMethod = getSelectedPaymentType(dialogBinding);
            processPayment(String.valueOf(finalAmount), paymentMethod);
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

                    // Add penalty amount from API if exists
                    if (penaltyAmountFromApi > 0) {
                        params.put("penalty_amount", Math.round(penaltyAmountFromApi));
                    }
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
        preferenceManager.saveBooleanValue("isOnLiveRide",false);
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
        // Consider clearing other ride detail variables if necessary




        preferenceManager.saveBooleanValue("isPickupNotificationSent",false);
        String bookingId = assignedBookingId+"";
        if (bookingId != null && !bookingId.isEmpty()) {
            proximityManager.clearNotifications(bookingId);
        }

        preferenceManager.saveBooleanValue("isLiveRide", false);
        preferenceManager.saveStringValue("current_booking_id_assigned", "");
        preferenceManager.removeValue("penalty_amount_" + assignedBookingId);
        if (assignedBookingId > 0) {
            // Clear the drop index specific to this booking
            String key = "current_drop_index_" + assignedBookingId;
            preferenceManager.removeValue(key);
            Log.d(TAG,"Removed drop index from preferences for key: " + key);
        }
        // Reset local variables
        assignedBookingId = -1;
        currentDropIndex = 0;

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
        boolean isOnLiveRide = preferenceManager.getBooleanValue("isOnLiveRide");
        Log.d(TAG, "Navigating to HomeActivity.");
        if (isOnLiveRide) {
            // Minimize the app (send to background)
            moveTaskToBack(true);
        } else {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish(); // Finish this activity
        }
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

    /*private void showOnMap() {
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
        Bitmap pickupBitmap = drawTextToBitmap(this, R.drawable.ic_current_long, "P");
        pickupMarker = mMap.addMarker(new MarkerOptions()
                .position(pickupLatLng)
                .title("Pickup")
                .snippet(pickupAddress)
                .icon(BitmapDescriptorFactory.fromBitmap(pickupBitmap))
                .zIndex(0.5f));

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boundsBuilder.include(pickupLatLng);

        // --- Add Drop Markers & Calculate Routes ---
        LatLng lastPointForRoute = pickupLatLng; // Start route from pickup

        if (multipleDrops > 0 && !allDropLatLngs.isEmpty()) {
            // Multiple Drops Scenario
            for (int i = 0; i < allDropLatLngs.size(); i++) {
                LatLng dropLatLng = allDropLatLngs.get(i);
                String title = "Drop " + (i + 1);
                float alpha = 1.0f;
                float zIndex = 0.0f;
                boolean isCurrentTarget = (i == currentDropIndex && currentApiStatus != null &&
                        (currentApiStatus.equals(STATUS_START_TRIP) ||
                                currentApiStatus.startsWith("Reached Drop")));

                // Create marker with index number
                Bitmap markerBitmap;
                if (i < currentDropIndex) {
                    // Completed Drop - faded with number
                    markerBitmap = drawTextToBitmap(this, R.drawable.ic_destination_long,
                            String.valueOf(i + 1));
                    title += " (Completed)";
                    alpha = 0.6f;
                } else if (isCurrentTarget) {
                    // Current Target Drop - highlighted with number
                    markerBitmap = drawTextToBitmap(this, R.drawable.ic_destination_long,
                            String.valueOf(i + 1));
                    title += " (Next)";
                    zIndex = 1.0f;
                } else {
                    // Upcoming Drop - normal with number
                    markerBitmap = drawTextToBitmap(this, R.drawable.ic_destination_long,
                            String.valueOf(i + 1));
                }

                Marker dropMarker = mMap.addMarker(new MarkerOptions()
                        .position(dropLatLng)
                        .title(title)
                        .snippet(allDropAddresses.size() > i ? allDropAddresses.get(i) : "Drop Address")
                        .icon(BitmapDescriptorFactory.fromBitmap(markerBitmap))
                        .alpha(alpha)
                        .zIndex(zIndex));
                dropMarkers.add(dropMarker);
                boundsBuilder.include(dropLatLng);

                // Draw route segment
                if (i <= currentDropIndex || i == 0) {
                    drawRoute(lastPointForRoute, dropLatLng);
                    lastPointForRoute = dropLatLng;
                }
            }
        } else {
            // Single Drop Scenario
            if (destinationLat != null && !destinationLat.equals("0.0") &&
                    destinationLng != null && !destinationLng.equals("0.0")) {

                LatLng dropLatLng = new LatLng(
                        Double.parseDouble(destinationLat),
                        Double.parseDouble(destinationLng));

                // Create single drop marker with "1"
                Bitmap dropBitmap = drawTextToBitmap(this, R.drawable.ic_destination_long, "1");
                Marker dropMarker = mMap.addMarker(new MarkerOptions()
                        .position(dropLatLng)
                        .title("Drop")
                        .snippet(dropAddress)
                        .icon(BitmapDescriptorFactory.fromBitmap(dropBitmap)));

                dropMarkers.add(dropMarker);
                boundsBuilder.include(dropLatLng);
                drawRoute(lastPointForRoute, dropLatLng);
            } else {
                Log.w(TAG, "Single drop selected, but destination coordinates are invalid.");
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
    }*/

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
        Bitmap pickupBitmap = drawTextToBitmap(this, R.drawable.ic_current_long, "P");
        pickupMarker = mMap.addMarker(new MarkerOptions()
                .position(pickupLatLng)
                .title("Pickup")
                .snippet(pickupAddress)
                .icon(BitmapDescriptorFactory.fromBitmap(pickupBitmap))
                .zIndex(0.5f));

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        boundsBuilder.include(pickupLatLng);

        // --- Add Driver Location Marker ---
        Location driverLocation = LocationUpdateService.getLastKnownLocation();
        LatLng driverLatLng = null;


        if (driverLocation != null) {
            driverLatLng = new LatLng(driverLocation.getLatitude(), driverLocation.getLongitude());
            // Use a distinct icon for driver
            Bitmap driverBitmap = drawTextToBitmap(this, R.drawable.ic_logistic, "D");
            mMap.addMarker(new MarkerOptions()
                    .position(driverLatLng)
                    .title("Your Location")
                    .icon(BitmapDescriptorFactory.fromBitmap(driverBitmap))
                    .zIndex(2.0f)); // Keep driver marker on top

            boundsBuilder.include(driverLatLng);
        }






        // Determine navigation target based on current status
        LatLng targetForDriverRoute = null;
        if (STATUS_DRIVER_ACCEPTED.equals(currentApiStatus) ||
                STATUS_DRIVER_ARRIVED.equals(currentApiStatus)) {
            // Route to pickup
            targetForDriverRoute = pickupLatLng;
        } else if (STATUS_START_TRIP.equals(currentApiStatus) ||
                currentApiStatus.startsWith(STATUS_REACHED_DROP_PREFIX)) {
            // Route to current drop
            if (multipleDrops > 0 && !allDropLatLngs.isEmpty() &&
                    currentDropIndex < allDropLatLngs.size()) {
                targetForDriverRoute = allDropLatLngs.get(currentDropIndex);
            } else if (multipleDrops < 0 && destinationLat != null &&
                    !destinationLat.equals("0.0")) {
                targetForDriverRoute = new LatLng(
                        Double.parseDouble(destinationLat),
                        Double.parseDouble(destinationLng)
                );
            }
        }

        // Draw route from driver to target if both points exist
        if (driverLatLng != null && targetForDriverRoute != null) {
            drawDriverRoute(driverLatLng, targetForDriverRoute);
            boundsBuilder.include(targetForDriverRoute);
        }


        // --- Add Drop Markers & Calculate Routes ---
        LatLng lastPointForRoute = pickupLatLng; // Start route from pickup

        if (multipleDrops > 0 && !allDropLatLngs.isEmpty()) {
            // Multiple Drops Scenario
            for (int i = 0; i < allDropLatLngs.size(); i++) {
                LatLng dropLatLng = allDropLatLngs.get(i);
                String title = "Drop " + (i + 1);
                float alpha = 1.0f;
                float zIndex = 0.0f;
                boolean isCurrentTarget = (i == currentDropIndex && currentApiStatus != null &&
                        (currentApiStatus.equals(STATUS_START_TRIP) ||
                                currentApiStatus.startsWith("Reached Drop")));

                // Create marker with index number
                Bitmap markerBitmap;
                if (i < currentDropIndex) {
                    // Completed Drop - faded with number
                    markerBitmap = drawTextToBitmap(this, R.drawable.ic_destination_long,
                            String.valueOf(i + 1));
                    title += " (Completed)";
                    alpha = 0.6f;
                } else if (isCurrentTarget) {
                    // Current Target Drop - highlighted with number
                    markerBitmap = drawTextToBitmap(this, R.drawable.ic_destination_long,
                            String.valueOf(i + 1));
                    title += " (Next)";
                    zIndex = 1.0f;
                } else {
                    // Upcoming Drop - normal with number
                    markerBitmap = drawTextToBitmap(this, R.drawable.ic_destination_long,
                            String.valueOf(i + 1));
                }

                Marker dropMarker = mMap.addMarker(new MarkerOptions()
                        .position(dropLatLng)
                        .title(title)
                        .snippet(allDropAddresses.size() > i ? allDropAddresses.get(i) : "Drop Address")
                        .icon(BitmapDescriptorFactory.fromBitmap(markerBitmap))
                        .alpha(alpha)
                        .zIndex(zIndex));
                dropMarkers.add(dropMarker);
                boundsBuilder.include(dropLatLng);

                // Draw route segment
                if (i <= currentDropIndex || i == 0) {
                    drawRoute(lastPointForRoute, dropLatLng);
                    lastPointForRoute = dropLatLng;
                }
            }
        } else {
            // Single Drop Scenario
            if (destinationLat != null && !destinationLat.equals("0.0") &&
                    destinationLng != null && !destinationLng.equals("0.0")) {

                LatLng dropLatLng = new LatLng(
                        Double.parseDouble(destinationLat),
                        Double.parseDouble(destinationLng));

                // Create single drop marker with "1"
                Bitmap dropBitmap = drawTextToBitmap(this, R.drawable.ic_destination_long, "1");
                Marker dropMarker = mMap.addMarker(new MarkerOptions()
                        .position(dropLatLng)
                        .title("Drop")
                        .snippet(dropAddress)
                        .icon(BitmapDescriptorFactory.fromBitmap(dropBitmap)));

                dropMarkers.add(dropMarker);
                boundsBuilder.include(dropLatLng);
                drawRoute(lastPointForRoute, dropLatLng);
            } else {
                Log.w(TAG, "Single drop selected, but destination coordinates are invalid.");
            }
        }


        // Update camera bounds to include all points
        if (targetForDriverRoute != null) {
            boundsBuilder.include(targetForDriverRoute);
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

    private void drawDriverRoute(LatLng origin, LatLng destination) {
        String url = getDirectionsUrl(origin, destination);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray routes = response.optJSONArray("routes");
                        if (routes != null && routes.length() > 0) {
                            JSONObject route = routes.getJSONObject(0);
                            JSONObject polyline = route.getJSONObject("overview_polyline");
                            String encodedPath = polyline.getString("points");
                            List<LatLng> decodedPath = decodePolyline(encodedPath);

                            PolylineOptions polylineOptions = new PolylineOptions()
                                    .addAll(decodedPath)
                                    .width(12) // Slightly thicker
                                    .color(ContextCompat.getColor(this, R.color.driver_route_color))
                                    .geodesic(true)
                                    .zIndex(2.0f); // Keep on top

                            mainThreadHandler.post(() -> {
                                Polyline driverPolyline = mMap.addPolyline(polylineOptions);
                                routePolylines.add(driverPolyline);
                            });
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing driver route", e);
                    }
                },
                error -> Log.e(TAG, "Error fetching driver route: " + error.getMessage())
        );
        configureAndAddRequest(jsonObjectRequest);
    }

    private Bitmap drawTextToBitmap(Context context, int resourceId, String text) {
        Resources resources = context.getResources();
        float scale = resources.getDisplayMetrics().density;
        Bitmap bitmap = BitmapFactory.decodeResource(resources, resourceId);

        Bitmap.Config bitmapConfig = bitmap.getConfig();
        if (bitmapConfig == null) {
            bitmapConfig = Bitmap.Config.ARGB_8888;
        }
        bitmap = bitmap.copy(bitmapConfig, true);

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Customize text appearance
        paint.setColor(Color.WHITE);  // White text
        paint.setTextSize((int) (14 * scale));  // Adjusted text size
        paint.setFakeBoldText(true);  // Make text bold
        paint.setShadowLayer(2f, 0f, 0f, Color.BLACK);  // Black shadow for better visibility

        // Center text
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        int x = (bitmap.getWidth() - bounds.width()) / 2;
        int y = (bitmap.getHeight() + bounds.height()) / 2;  // Centered vertically

        canvas.drawText(text, x, y, paint);

        return bitmap;
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

        // During the trip, navigate to the current drop target
        if (currentApiStatus != null && (currentApiStatus.equals(STATUS_START_TRIP) || currentApiStatus.equals(STATUS_REACHED_DROP_PREFIX) || currentApiStatus.equals(STATUS_MAKE_PAYMENT)) ) {
//        if (currentApiStatus != null ) {
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
        boolean isOnLiveRide = preferenceManager.getBooleanValue("isOnLiveRide");
        if (isFromFCM) {

            // If opened from FCM notification, always go to Home screen
            navigateToHome();

        } else {
            // Allow normal back press behavior otherwise
            if (isOnLiveRide) {
                // Minimize the app (send to background)
                moveTaskToBack(true);
            }else {
                super.onBackPressed();
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (locationUpdateReceiver != null) {
            try {
                unregisterReceiver(locationUpdateReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering location receiver", e);
            }
        }
        // Clean up resources
        if (timerManager != null) {
            timerManager.pauseTimer();
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

    private void updatePenaltyAmount(double penaltyAmount) {
        if (assignedBookingId <= 0 || !STATUS_START_TRIP.equals(currentApiStatus)) {
            Log.d(TAG, "Cannot update penalty - Invalid state or status");
            return;
        }

        showLoading("Updating penalty amount...");

        executorService.submit(() -> {
            String accessToken = null;
            try {
                accessToken = AccessToken.getAccessToken();
            } catch (Exception e) {
                Log.e(TAG, "Error getting access token for penalty update", e);
            }

            final String finalAccessToken = accessToken;
            mainThreadHandler.post(() -> {
                if (finalAccessToken == null || finalAccessToken.isEmpty()) {
                    hideLoading();
                    showToast("Authentication error");
                    return;
                }

                JSONObject params = new JSONObject();
                try {
                    params.put("booking_id", assignedBookingId);
                    params.put("server_token", finalAccessToken);
                    params.put("penalty_amount", penaltyAmount);
                } catch (JSONException e) {
                    hideLoading();
                    Log.e(TAG, "Error creating penalty update params", e);
                    return;
                }

                JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
                        APIClient.baseUrl+"update_goods_booking_penalty_amount", params,
                        response -> {
                            hideLoading();
                            try {
                                boolean success = response.optBoolean("success", false);
                                if (success) {
                                    // Save penalty amount in preferences
                                    preferenceManager.saveFloatValue("penalty_amount_" + assignedBookingId,
                                            (float) penaltyAmount);
                                    Log.d(TAG, "Penalty amount updated: " + penaltyAmount);
                                } else {
                                    Log.w(TAG, "Penalty update failed: " + response.toString());
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing penalty update response", e);
                            }
                        },
                        error -> {
                            hideLoading();
                            handleVolleyError(error);
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

    private void showCancelBookingBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_cancel_booking, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        // Initialize views

        TextView cancelText = bottomSheetView.findViewById(R.id.cancelText);
        RecyclerView reasonsRecyclerView = bottomSheetView.findViewById(R.id.reasonsRecyclerView);
        TextInputLayout otherReasonLayout = bottomSheetView.findViewById(R.id.otherReasonLayout);
        TextInputEditText otherReasonInput = bottomSheetView.findViewById(R.id.otherReasonInput);
        Button submitButton = bottomSheetView.findViewById(R.id.submitButton);




        cancelText.setText("You are about to cancel the booking which was assigned to " + customerName);

        // Fetch cancel reasons from API
        fetchCancelReasons(reasonsRecyclerView, otherReasonLayout, otherReasonInput, submitButton, bottomSheetDialog);

        bottomSheetDialog.show();
    }

    private void fetchCancelReasons(RecyclerView recyclerView, TextInputLayout otherReasonLayout,
                                    TextInputEditText otherReasonInput, Button submitButton,
                                    BottomSheetDialog dialog) {



        JSONObject params = new JSONObject();
        try {
            params.put("category_id",  1); // Fixed category_id as 1

        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }



        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                APIClient.baseUrl + "get_driver_category_cancel_reasons",
                params,
                response -> {
                    try {
                        JSONArray reasonsArray = response.getJSONArray("reasons");
                        List<CancelReason> cancelReasons = new ArrayList<>();

                        for (int i = 0; i < reasonsArray.length(); i++) {
                            JSONObject reasonObj = reasonsArray.getJSONObject(i);
                            cancelReasons.add(new CancelReason(
                                    reasonObj.getInt("reason_id"),
                                    reasonObj.getString("reason")
                            ));
                        }

                        // Add "Other reasons" option
                        cancelReasons.add(new CancelReason(-1, "Other reasons"));

                        setupCancelReasonAdapter(cancelReasons, recyclerView, otherReasonLayout,
                                otherReasonInput, submitButton, dialog);

                    } catch (JSONException e) {
                        e.printStackTrace();
                        showError("Error loading cancel reasons");
                    }
                },
                error -> {
                    error.printStackTrace();
                    showError("Failed to load cancel reasons");
                }
        );

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }

    private void setupCancelReasonAdapter(List<CancelReason> reasons, RecyclerView recyclerView,
                                          TextInputLayout otherReasonLayout, TextInputEditText otherReasonInput,
                                          Button submitButton, BottomSheetDialog dialog) {

        final CancelReason[] selectedReason = {null};

        CancelReasonAdapter adapter = new CancelReasonAdapter(reasons, reason -> {
            selectedReason[0] = reason;
            submitButton.setEnabled(true);

            // Show/hide other reason input
            if (reason.getReason().equals("Other reasons")) {
                otherReasonLayout.setVisibility(View.VISIBLE);
            } else {
                otherReasonLayout.setVisibility(View.GONE);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Handle submit button
        submitButton.setOnClickListener(v -> {
            if (selectedReason[0] == null) {
                showError("Please select a reason");
                return;
            }

            String finalReason;
            if (selectedReason[0].getReason().equals("Other reasons")) {
                String otherReason = otherReasonInput.getText().toString();
                if (otherReason.isEmpty()) {
                    otherReasonInput.setError("Please enter a reason");
                    return;
                }
                finalReason = otherReason;
            } else {
                finalReason = selectedReason[0].getReason();
            }

            // Show loading
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Cancelling booking...");
            progressDialog.show();

            cancelBooking(finalReason, new CancelBookingCallback() {
                @Override
                public void onSuccess() {
                    progressDialog.dismiss();
                    dialog.dismiss();
                }

                @Override
                public void onError(String error) {
                    progressDialog.dismiss();
                    showError(error);
                }
            });
        });
    }

    private void cancelBooking(String reason, CancelBookingCallback callback) {
        String url =  APIClient.baseUrl +"cancel_booking";
        String customerAccessToken = AccessToken.getAccessToken();
        String agentAccessToken = AccessToken.getAgentAccessToken();


        String fcmToken = preferenceManager.getStringValue("fcm_token");
        String driverId = preferenceManager.getStringValue("goods_driver_id");


        JSONObject params = new JSONObject();
        try {
            params.put("booking_id", bookingIdStr);
            params.put("customer_id", customerID);
            params.put("driver_id", driverId);
            params.put("agent_server_token", agentAccessToken);
            params.put("customer_server_token", customerAccessToken);
            params.put("pickup_address", pickupAddress);
            params.put("cancel_reason", reason);
            params.put("auth", fcmToken);
            params.put("cancelled_by","Agent"  );

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    params,
                    response -> {
                        showError("Booking Cancelled Successfully");
                        clearRideState();
                        onBackPressed();
                    },
                    error -> callback.onError(error.getMessage())
            );

            request.setRetryPolicy(new DefaultRetryPolicy(
                    30000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            VolleySingleton.getInstance(this).addToRequestQueue(request);
        } catch (JSONException e) {
            callback.onError(e.getMessage());
        }
    }

    interface CancelBookingCallback {
        void onSuccess();
        void onError(String error);
    }
}