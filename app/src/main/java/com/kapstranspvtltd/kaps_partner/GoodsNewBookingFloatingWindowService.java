package com.kapstranspvtltd.kaps_partner;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.kapstranspvtltd.kaps_partner.fcm.AccessToken;
import com.kapstranspvtltd.kaps_partner.fcm.popups.GoodsBookingAcceptActivity;
import com.kapstranspvtltd.kaps_partner.goods_driver_activities.HomeActivity;
import com.kapstranspvtltd.kaps_partner.goods_driver_activities.NewLiveRideActivity;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoodsNewBookingFloatingWindowService extends Service {
    private static final String TAG = "FloatingWindowService";
    private static final int NOTIFICATION_ID = 54321;
    private static final String CHANNEL_ID = "floating_window_channel";

    private MediaPlayer mediaPlayer;
    private WindowManager windowManager;
    private View floatingView;
    private CountDownTimer countDownTimer;
    private PreferenceManager preferenceManager;
    private String bookingId;
    private JSONObject bookingJsonDetails;

    @Override
    public void onCreate() {
        super.onCreate();
        preferenceManager = new PreferenceManager(this);
        createNotificationChannel();
        initializeMediaPlayer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start as foreground service first
        startForeground(NOTIFICATION_ID, buildNotification());

        // Get booking ID from intent
        if (intent != null && intent.getExtras() != null) {
            bookingId = intent.getStringExtra("booking_id");
            if (bookingId != null) {
                Log.d(TAG, "Received booking ID: " + bookingId);
                fetchBookingDetails(bookingId);
                playNotificationSound();
            } else {
                stopSelf();
            }
        } else {
            stopSelf();
        }

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Booking Alert Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );

            serviceChannel.setDescription("Shows notifications for new booking requests");
            serviceChannel.enableLights(true);
            serviceChannel.setLightColor(Color.BLUE);
            serviceChannel.enableVibration(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("New Booking Alert")
                .setContentText("You have a new booking request")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .build();
    }

    private void initializeMediaPlayer() {
        mediaPlayer = MediaPlayer.create(this, R.raw.booking_notification);
        mediaPlayer.setLooping(true);
    }

    private void playNotificationSound() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.start();
                // Stop sound after 15 seconds
                new Handler(Looper.getMainLooper()).postDelayed(this::stopNotificationSound, 15000);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing sound: " + e.getMessage());
        }
    }

    private void stopNotificationSound() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            try {
                mediaPlayer.prepare();
            } catch (Exception e) {
                Log.e(TAG, "Error preparing media player: " + e.getMessage());
            }
        }
    }

    private void fetchBookingDetails(String bookingId) {
        // Get access token asynchronously
        new Thread(() -> {
            try {
                String serverToken = AccessToken.getAccessToken();
                String url = APIClient.baseUrl + "booking_details_for_ride_acceptance";

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("booking_id", bookingId);

                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        jsonBody,
                        response -> {
                            try {
                                JSONArray results = response.optJSONArray("results");
                                if (results != null && results.length() > 0) {
                                    JSONObject bookingDetails = results.getJSONObject(0);
                                    Log.d(TAG, "Booking details: " + bookingDetails.toString());
                                    bookingJsonDetails = bookingDetails;
                                    initializeFloatingWindow();
                                } else {
                                    showToast("No booking details found");
                                    stopSelf();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing booking details: " + e.getMessage());
                                showToast("Error loading booking details");
                                stopSelf();
                            }
                        },
                        error -> {
                            Log.e(TAG, "Error fetching booking details: " + error.getMessage());
                            showToast("Failed to load booking details");
                            stopSelf();
                        }
                ) {
                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Content-Type", "application/json");
                        headers.put("Authorization", "Bearer " + serverToken);
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
                Log.e(TAG, "Error in fetchBookingDetails: " + e.getMessage());
                showToast("Error: " + e.getMessage());
                stopSelf();
            }
        }).start();
    }

    private void showToast(String message) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show());
    }

    private void initializeFloatingWindow() {
        if (bookingJsonDetails == null) {
            stopSelf();
            return;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Inflate the floating view layout
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        floatingView = inflater.inflate(R.layout.activity_goods_booking_accept, null);

        // Set window manager parameters
        WindowManager.LayoutParams params;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params = new WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
            );
        } else {
            params = new WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
            );
        }

        // Set gravity to bottom
        params.gravity = Gravity.BOTTOM;

        // Set the layout height to be around 70% of the screen height
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;
        params.height = (int) (screenHeight * 0.7); // 70% of screen height

        // Add the view to the window
        windowManager.addView(floatingView, params);

        // Setup the UI elements
        setupUI();
    }

    private void setupUI() {
        try {
            // Get views from the layout
            TextView timerText = floatingView.findViewById(R.id.timerText);
            TextView pickupAddressView = floatingView.findViewById(R.id.pickupAddress);
            TextView dropAddressView = floatingView.findViewById(R.id.dropAddress);
            TextView customerNameView = floatingView.findViewById(R.id.customerName);
            TextView rideFareView = floatingView.findViewById(R.id.rideFare);
            TextView distanceView = floatingView.findViewById(R.id.distance);
            TextView pickupDistanceView = floatingView.findViewById(R.id.pickupLocationDistance);
            TextView bookingTypeView = floatingView.findViewById(R.id.bookingTypeText);
            Button acceptButton = floatingView.findViewById(R.id.acceptButton);
            Button rejectButton = floatingView.findViewById(R.id.rejectButton);

            // Start the countdown timer
            startCountdownTimer(timerText);

            // Set button click listeners
            acceptButton.setOnClickListener(v -> handleAcceptBooking());
            rejectButton.setOnClickListener(v -> handleRejectBooking());

            // Set values from booking details
            String pickupAddress = bookingJsonDetails.optString("pickup_address", "N/A");
            String dropAddress = bookingJsonDetails.optString("drop_address", "N/A");
            String customerName = bookingJsonDetails.optString("customer_name", "N/A");
            double totalPrice = bookingJsonDetails.optDouble("total_price", 0);
            double distance = bookingJsonDetails.optDouble("distance", 0);

            pickupAddressView.setText(pickupAddress);
            customerNameView.setText(customerName);
            rideFareView.setText(String.format("₹%.0f", totalPrice));
            distanceView.setText(String.format("%.1f Km", distance));

            // Get multiple drops information
            int multipleDrops = bookingJsonDetails.optInt("multiple_drops", 0);
            System.out.println("multiple_drops::" + multipleDrops);

            // Parse drop_locations string into JSONArray
            JSONArray dropLocations = null;
            try {
                String dropLocationsStr = bookingJsonDetails.optString("drop_locations", "[]");
                System.out.println("drop_locations string::" + dropLocationsStr);

                // Check if it's already a JSONArray object or a string that needs parsing
                if (dropLocationsStr.startsWith("[")) {
                    dropLocations = new JSONArray(dropLocationsStr);
                    System.out.println("Parsed drop_locations::" + dropLocations);
                    System.out.println("drop_locations length::" + dropLocations.length());
                } else {
                    // If it's not a valid JSON string, create an empty array
                    dropLocations = new JSONArray();
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing drop locations: " + e.getMessage());
                dropLocations = new JSONArray();
            }

            // Update booking type text
            if (multipleDrops > 0 && dropLocations != null && dropLocations.length() > 0) {
                bookingTypeView.setText("Multiple Drop Booking (" + dropLocations.length() + " drops)");
                bookingTypeView.setTextColor(getResources().getColor(R.color.colorPrimary));
            } else {
                bookingTypeView.setText("Single Drop Booking");
                bookingTypeView.setTextColor(getResources().getColor(R.color.colorgreen));
            }

            // Calculate pickup distance immediately using device location
            calculatePickupDistance(pickupDistanceView);

            // Setup multiple drop locations if available
            setupDropLocations(dropAddressView, multipleDrops, dropLocations);

        } catch (Exception e) {
            Log.e(TAG, "Error setting up UI: " + e.getMessage(), e);
        }
    }

    private void setupDropLocations(TextView defaultDropAddressView, int multipleDrops, JSONArray dropLocations) {
        try {
            LinearLayout expandedContent = floatingView.findViewById(R.id.expandedContent);
            LinearLayout dropSection = floatingView.findViewById(R.id.dropSection);
            RecyclerView dropRecyclerView = floatingView.findViewById(R.id.dropRecyclerView);

            System.out.println("In setupDropLocations, multipleDrops: " + multipleDrops);
            System.out.println("In setupDropLocations, dropLocations: " + (dropLocations != null ? dropLocations.toString() : "null"));

            // If we have multiple drops and drop locations array is not empty
            if (multipleDrops > 0 && dropLocations != null && dropLocations.length() > 0) {
                // Hide the default drop address section
                dropSection.setVisibility(View.GONE);

                // Show the RecyclerView for drop locations
                dropRecyclerView.setVisibility(View.VISIBLE);

                // Parse drop locations
                List<DropLocation> dropsList = new ArrayList<>();

                for (int i = 0; i < dropLocations.length(); i++) {
                    try {
                        JSONObject drop = dropLocations.getJSONObject(i);
                        DropLocation dropLocation = new DropLocation();
                        dropLocation.address = drop.optString("address", "N/A");
                        dropLocation.position = i + 1;
                        dropsList.add(dropLocation);
                        Log.d(TAG, "Added drop location " + (i+1) + ": " + dropLocation.address);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing drop location at index " + i + ": " + e.getMessage());
                    }
                }

                // Setup the RecyclerView with adapter
                dropRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                DropAdapter adapter = new DropAdapter(dropsList);
                dropRecyclerView.setAdapter(adapter);
                Log.d(TAG, "Drop locations adapter set with " + dropsList.size() + " items");

            } else {
                // Single drop location
                dropSection.setVisibility(View.VISIBLE);
                dropRecyclerView.setVisibility(View.GONE);
                defaultDropAddressView.setText(bookingJsonDetails.optString("drop_address", "N/A"));
                Log.d(TAG, "Single drop location displayed: " + bookingJsonDetails.optString("drop_address", "N/A"));
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting up drop locations: " + e.getMessage(), e);
        }
    }
    private void calculatePickupDistance(TextView distanceView) {
        try {
            // Get pickup coordinates
            double pickupLat = bookingJsonDetails.optDouble("pickup_lat", 0);
            double pickupLng = bookingJsonDetails.optDouble("pickup_lng", 0);

            if (pickupLat == 0 || pickupLng == 0) {
                distanceView.setText("Location unavailable");
                return;
            }

            // Get last known location from shared preferences for immediate display
            float lastLat = preferenceManager.getFloatValue("last_known_lat", 0);
            float lastLng = preferenceManager.getFloatValue("last_known_lng", 0);

            if (lastLat != 0 && lastLng != 0) {
                // Calculate straight-line distance immediately (for responsiveness)
                float[] results = new float[1];
                android.location.Location.distanceBetween(lastLat, lastLng, pickupLat, pickupLng, results);
                float distanceInMeters = results[0];

                if (distanceInMeters < 1000) {
                    distanceView.setText(String.format("%.0f m away", distanceInMeters));
                } else {
                    distanceView.setText(String.format("%.1f km away", distanceInMeters / 1000));
                }
            } else {
                distanceView.setText("Calculating...");
            }

            // Then get actual driving distance from Google Maps API
            if (ActivityCompat.checkSelfPermission(this,
                    ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                FusedLocationProviderClient fusedLocationClient =
                        LocationServices.getFusedLocationProviderClient(this);

                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        // Save current location for future use
                        preferenceManager.saveFloatValue("last_known_lat", (float) location.getLatitude());
                        preferenceManager.saveFloatValue("last_known_lng", (float) location.getLongitude());

                        // Get precise distance with Google Distance Matrix API
                        getDrivingDistance(location.getLatitude(), location.getLongitude(),
                                pickupLat, pickupLng, distanceView);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating distance: " + e.getMessage());
            distanceView.setText("Distance unavailable");
        }
    }

    private void getDrivingDistance(double originLat, double originLng,
                                    double destLat, double destLng, TextView distanceView) {
        String url = String.format(
                "https://maps.googleapis.com/maps/api/distancematrix/json?origins=%f,%f&destinations=%f,%f&mode=driving&key=%s",
                originLat, originLng, destLat, destLng, getString(R.string.google_maps_key)
        );

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray rows = response.getJSONArray("rows");
                        JSONObject elements = rows.getJSONObject(0)
                                .getJSONArray("elements")
                                .getJSONObject(0);

                        if ("OK".equals(elements.getString("status"))) {
                            String distance = elements.getJSONObject("distance").getString("text");
                            String duration = elements.getJSONObject("duration").getString("text");

                            // Update UI with both distance and duration
                            String distanceText = String.format("%s (%s away)", distance, duration);
                            distanceView.setText(distanceText);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing distance matrix response", e);
                    }
                },
                error -> Log.e(TAG, "Error fetching distance", error)
        );

        request.setRetryPolicy(new DefaultRetryPolicy(
                15000, // 15 seconds timeout instead of 30
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        VolleySingleton.getInstance(this).addToRequestQueue(request);
    }
    private void startCountdownTimer(TextView timerText) {
        countDownTimer = new CountDownTimer(15000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerText.setText(String.format("%d sec left", millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                try {
                    stopService(new Intent(GoodsNewBookingFloatingWindowService.this, GoodsNewBookingFloatingWindowService.class));
                } catch (Exception e) {
                    Log.e(TAG, "Error stopping previous service: " + e.getMessage());
                } // Close service when timer expires
            }
        }.start();
    }

    private void handleAcceptBooking() {
        if (bookingId == null) {
            showToast("Invalid booking ID");
            return;
        }

        // Cancel timer and stop sound
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        stopNotificationSound();

        // Use ExecutorService for background operations
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                String driverId = preferenceManager.getStringValue("goods_driver_id");
                String accessToken = AccessToken.getAccessToken();

                if (accessToken == null || accessToken.isEmpty()) {
                    handler.post(() -> {
                        showToast("No Token Found!");
                        stopSelf();
                    });
                    return;
                }

                // Save current booking ID
                preferenceManager.saveStringValue("current_booking_id_assigned", bookingId);

                // Prepare request body
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("booking_id", bookingId);
                jsonBody.put("driver_id", driverId);
                jsonBody.put("server_token", accessToken);
                jsonBody.put("customer_id", bookingJsonDetails.optString("customer_id"));

                // Make API request using Volley
                String url = APIClient.baseUrl + "goods_driver_booking_accepted";

                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        jsonBody,
                        response -> {
                            // Handle successful response
                            Intent intent = new Intent(GoodsNewBookingFloatingWindowService.this,
                                    NewLiveRideActivity.class);
                            intent.putExtra("booking_id", bookingId);
                            intent.putExtra("FromFCM", true);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            stopSelf();
                        },
                        error -> {
                            handleVolleyError(error);
                            stopSelf();
                        }
                ) {
                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Content-Type", "application/json");
                        headers.put("Authorization", "Bearer " + accessToken);
                        return headers;
                    }
                };

                request.setRetryPolicy(new DefaultRetryPolicy(
                        30000,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                ));

                // Add to request queue on main thread
                handler.post(() -> {
                    VolleySingleton.getInstance(GoodsNewBookingFloatingWindowService.this)
                            .addToRequestQueue(request);
                });

            } catch (Exception e) {
                handler.post(() -> {
                    Log.e(TAG, "Error: " + e.getMessage());
                    showToast("Error: " + e.getMessage());
                    stopSelf();
                });
            }
        });

        executor.shutdown();
    }

    private void handleRejectBooking() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        stopNotificationSound();
//        stopSelf();
        try {
            stopService(new Intent(GoodsNewBookingFloatingWindowService.this, GoodsNewBookingFloatingWindowService.class));
        } catch (Exception e) {
            Log.e(TAG, "Error stopping previous service: " + e.getMessage());
        } // Close service when timer expires
    }

    private void handleVolleyError(VolleyError error) {
        if (error.networkResponse != null && error.networkResponse.data != null) {
            try {
                String errorJson = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                JSONObject errorObj = new JSONObject(errorJson);

                if (errorObj.optString("message").contains("No Data Found")) {
                    handleNoDataFound();
                } else {
                    String message = errorObj.optString("message", "Something went wrong");
                    showToast(message);
                }
            } catch (JSONException e) {
                handleDefaultError(error);
            }
        } else {
            handleDefaultError(error);
        }
    }

    private void handleNoDataFound() {
        // Clear current booking ID
        preferenceManager.saveStringValue("current_booking_id_assigned", "");
        showToast("Already Assigned to Another Driver.\nPlease be quick at receiving ride requests to earn more.");
    }

    private void handleDefaultError(VolleyError error) {
        String message = "Something went wrong";
        if (error.toString().contains("NoConnectionError")) {
            message = "No internet connection";
        } else if (error.toString().contains("TimeoutError")) {
            message = "Request timed out";
        }
        showToast(message);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopNotificationSound();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        if (windowManager != null && floatingView != null) {
            windowManager.removeView(floatingView);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Model for drop location
    private static class DropLocation {
        String address;
        int position;
    }

    // Adapter for drop locations
    private class DropAdapter extends RecyclerView.Adapter<DropAdapter.DropViewHolder> {
        private final List<DropLocation> dropList;

        public DropAdapter(List<DropLocation> dropList) {
            this.dropList = dropList;
        }

        @Override
        public DropViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_drop, parent, false);
            return new DropViewHolder(view);
        }

        @Override
        public void onBindViewHolder(DropViewHolder holder, int position) {
            DropLocation drop = dropList.get(position);
            holder.dropAddressText.setText(drop.address);

            // Set the marker with number
            holder.dropMarker.setImageResource(R.drawable.ic_current_long);
            holder.dropMarker.setColorFilter(getResources().getColor(R.color.colorerror));

            // Display position number
            holder.dropPositionText.setText(String.valueOf(drop.position));
        }

        @Override
        public int getItemCount() {
            return dropList.size();
        }

        class DropViewHolder extends RecyclerView.ViewHolder {
            TextView dropAddressText;
            ImageView dropMarker;
            TextView dropPositionText;

            DropViewHolder(View itemView) {
                super(itemView);
                dropAddressText = itemView.findViewById(R.id.dropAddressText);
                dropMarker = itemView.findViewById(R.id.dropMarker);
                dropPositionText = itemView.findViewById(R.id.dropPositionText);
            }
        }
    }
}