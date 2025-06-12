package com.kapstranspvtltd.kaps_partner.cab_driver_activities;

import android.Manifest;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.cab_driver_activities.bookings.CabBookingAcceptActivity;

import com.kapstranspvtltd.kaps_partner.cab_driver_activities.settings_pages.CabDriverNewLiveRideActivity;
import com.kapstranspvtltd.kaps_partner.fcm.AccessToken;
import com.kapstranspvtltd.kaps_partner.fcm.FCMService;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CabNewBookingAcceptService extends Service {
    private static final String TAG = "CabFloatingWindow";
    private static final int NOTIFICATION_ID = 54322; // Different from goods service
    private static final String CHANNEL_ID = "cab_floating_window_channel";

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

//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        startForeground(NOTIFICATION_ID, buildNotification());
//
//        if (intent != null && intent.getExtras() != null) {
//            bookingId = intent.getStringExtra("booking_id");
//            if (bookingId != null) {
//                Log.d(TAG, "Received booking ID: " + bookingId);
//                fetchBookingDetails(bookingId);
//                playNotificationSound();
//            } else {
//                stopSelf();
//            }
//        } else {
//            stopSelf();
//        }
//
//        return START_STICKY;
//    }
@Override
public int onStartCommand(Intent intent, int flags, int startId) {
    Log.d(TAG, "Service onStartCommand called");

    // Start as foreground service first
    startForeground(NOTIFICATION_ID, buildNotification());

    // Handle null intent
    if (intent == null || intent.getExtras() == null) {
        Log.e(TAG, "Null intent or extras received");
        stopSelf();
        return START_NOT_STICKY;
    }

    // Get booking ID
    bookingId = intent.getStringExtra("booking_id");
    if (bookingId == null || bookingId.isEmpty()) {
        Log.e(TAG, "Invalid booking ID received");
        stopSelf();
        return START_NOT_STICKY;
    }

    Log.d(TAG, "Received booking ID: " + bookingId);
    fetchBookingDetails(bookingId);
    playNotificationSound();

    return START_NOT_STICKY; // Don't recreate if killed
}

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Cab Booking Alert Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );

            serviceChannel.setDescription("Shows notifications for new cab booking requests");
            serviceChannel.enableLights(true);
            serviceChannel.setLightColor(Color.BLUE);
            serviceChannel.enableVibration(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, CabBookingAcceptActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("New Cab Booking Alert")
                .setContentText("You have a new cab booking request")
                .setSmallIcon(R.drawable.logo)
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
        new Thread(() -> {
            try {
                String serverToken = AccessToken.getAccessToken();

                String driverId = preferenceManager.getStringValue("cab_driver_id");
                String token = preferenceManager.getStringValue("cab_driver_token");

                String url = APIClient.baseUrl + "cab_booking_details_for_ride_acceptance";

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("booking_id", bookingId);
                jsonBody.put("driver_unique_id", driverId);
                jsonBody.put("auth", token);

                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        jsonBody,
                        response -> {
                            try {
                                JSONArray results = response.optJSONArray("results");
                                if (results != null && results.length() > 0) {
                                    JSONObject bookingDetails = results.getJSONObject(0);
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

                request.setRetryPolicy(new DefaultRetryPolicy(30000,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

                VolleySingleton.getInstance(this).addToRequestQueue(request);

            } catch (Exception e) {
                Log.e(TAG, "Error in fetchBookingDetails: " + e.getMessage());
                showToast("Error: " + e.getMessage());
                stopSelf();
            }
        }).start();
    }

    private void initializeFloatingWindow() {
        if (bookingJsonDetails == null) {
            stopSelf();
            return;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        floatingView = inflater.inflate(R.layout.activity_cab_booking_accept, null);

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

        params.gravity = Gravity.BOTTOM;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;
//        params.height = (int) (screenHeight * 0.7);
        params.height = (int) (screenHeight);

        windowManager.addView(floatingView, params);
        setupUI();
    }

    private void setupUI() {
        try {
            TextView timerText = floatingView.findViewById(R.id.timerText);
            TextView pickupAddressView = floatingView.findViewById(R.id.pickupAddress);
            TextView dropAddressView = floatingView.findViewById(R.id.dropAddress);
            TextView customerNameView = floatingView.findViewById(R.id.customerName);
            TextView rideFareView = floatingView.findViewById(R.id.rideFare);

            TextView hikePriceTxt = floatingView.findViewById(R.id.hikePriceTxt);
            TextView tripTimeTxt = floatingView.findViewById(R.id.tripTime);

            TextView distanceView = floatingView.findViewById(R.id.distance);
            TextView pickupDistanceView = floatingView.findViewById(R.id.pickupLocationDistance);
            Button acceptButton = floatingView.findViewById(R.id.acceptButton);
            ImageView rejectButton = floatingView.findViewById(R.id.rejectButton);

            startCountdownTimer(timerText);

            acceptButton.setOnClickListener(v -> handleAcceptBooking());
            rejectButton.setOnClickListener(v -> handleRejectBooking());

            String pickupAddress = bookingJsonDetails.optString("pickup_address", "N/A");
            String dropAddress = bookingJsonDetails.optString("drop_address", "N/A");
            String customerName = bookingJsonDetails.optString("customer_name", "N/A");
            double totalPrice = bookingJsonDetails.optDouble("total_price", 0);
            double distance = bookingJsonDetails.optDouble("distance", 0);
            String totalTime = bookingJsonDetails.optString("total_time", "0 Mins");
            int hikePrice = bookingJsonDetails.optInt("hike_price", 0); //hike price

            if(hikePrice>0){
                hikePriceTxt.setVisibility(View.VISIBLE);
                hikePriceTxt.setText("+ ₹"+hikePrice+"");
                totalPrice-=hikePrice;
            }
            tripTimeTxt.setText(totalTime+" trip");

            pickupAddressView.setText(pickupAddress);
            dropAddressView.setText(dropAddress);
            customerNameView.setText(customerName);
            rideFareView.setText(String.format("₹%.0f", totalPrice));
            distanceView.setText(String.format("%.1f Km", distance));

            calculatePickupDistance(pickupDistanceView);

        } catch (Exception e) {
            Log.e(TAG, "Error setting up UI: " + e.getMessage());
        }
    }

    private void calculatePickupDistance(TextView distanceView) {
        try {
            double pickupLat = bookingJsonDetails.optDouble("pickup_lat", 0);
            double pickupLng = bookingJsonDetails.optDouble("pickup_lng", 0);

            if (pickupLat == 0 || pickupLng == 0) {
                distanceView.setText("Location unavailable");
                return;
            }

            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                FusedLocationProviderClient fusedLocationClient =
                        LocationServices.getFusedLocationProviderClient(this);

                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        String url = String.format(
                                "https://maps.googleapis.com/maps/api/distancematrix/json?origins=%f,%f&destinations=%f,%f&mode=driving&key=%s",
                                location.getLatitude(),
                                location.getLongitude(),
                                pickupLat,
                                pickupLng,
                                getString(R.string.google_maps_key)
                        );

                        JsonObjectRequest request = new JsonObjectRequest(
                                Request.Method.GET,
                                url,
                                null,
                                response -> {
                                    try {
                                        JSONArray rows = response.getJSONArray("rows");
                                        JSONObject elements = rows.getJSONObject(0)
                                                .getJSONArray("elements")
                                                .getJSONObject(0);

                                        if ("OK".equals(elements.getString("status"))) {
                                            String distance = elements.getJSONObject("distance")
                                                    .getString("text");
                                            String duration = elements.getJSONObject("duration")
                                                    .getString("text");
                                            distanceView.setText(String.format("%s (%s away)",
                                                    distance, duration));
                                        }
                                    } catch (JSONException e) {
                                        Log.e(TAG, "Error parsing distance matrix response", e);
                                        distanceView.setText("Distance calculation error");
                                    }
                                },
                                error -> {
                                    Log.e(TAG, "Error fetching distance", error);
                                    distanceView.setText("Distance unavailable");
                                }
                        );

                        request.setRetryPolicy(new DefaultRetryPolicy(15000,
                                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

                        VolleySingleton.getInstance(this).addToRequestQueue(request);
                    } else {
                        distanceView.setText("Location unavailable");
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating distance: " + e.getMessage());
            distanceView.setText("Distance unavailable");
        }
    }

    private void startCountdownTimer(TextView timerText) {
        countDownTimer = new CountDownTimer(15000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerText.setText(String.format("%d sec left", millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                stopSelf();
            }
        }.start();
    }

    private void handleAcceptBooking() {
        if (bookingId == null) {
            showToast("Invalid booking ID");
            return;
        }

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        stopNotificationSound();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                String driverId = preferenceManager.getStringValue("cab_driver_id");
                String accessToken = AccessToken.getAccessToken();

                if (accessToken == null || accessToken.isEmpty()) {
                    handler.post(() -> {
                        showToast("No Token Found!");
                        stopSelf();
                    });
                    return;
                }

                preferenceManager.saveStringValue("current_cab_booking_id_assigned", bookingId);


                String token = preferenceManager.getStringValue("cab_driver_token");

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("booking_id", bookingId);
                jsonBody.put("driver_id", driverId);
                jsonBody.put("server_token", accessToken);
                jsonBody.put("customer_id", bookingJsonDetails.optString("customer_id"));
                jsonBody.put("driver_unique_id", driverId);
                jsonBody.put("auth", token);

                String url = APIClient.baseUrl + "cab_driver_booking_accepted";

                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        jsonBody,
                        response -> {

                            Intent intent = new Intent(this, CabDriverNewLiveRideActivity.class);
                            intent.putExtra("booking_id", bookingId);
                            intent.putExtra("FromFCM", true);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            stopSelf();
                            FCMService.cancelAllNotifications(this);
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

                request.setRetryPolicy(new DefaultRetryPolicy(30000,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

                handler.post(() -> VolleySingleton.getInstance(this).addToRequestQueue(request));

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

    private void removeFloatingWindow() {
        if (windowManager != null && floatingView != null) {
            try {
                windowManager.removeView(floatingView);
                floatingView = null;
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Error removing floating window: " + e.getMessage());
            }
        }
    }

    private void handleRejectBooking() {
        // First, stop all ongoing operations
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        stopNotificationSound();

        // Remove the floating window immediately
        removeFloatingWindow();

        // Stop the service
        stopForeground(true); // Remove notification
        stopSelf(); // Stop the service
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
        preferenceManager.saveStringValue("current_cab_booking_id_assigned", "");
        showToast("Already Assigned to Another Driver.\nPlease be quick at receiving ride requests to earn more.");
        onDestroy();
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

    private void showToast(String message) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service onDestroy called");

        try {
            // Cancel timer first
            if (countDownTimer != null) {
                countDownTimer.cancel();
                countDownTimer = null;
            }

            // Stop and release media player
            if (mediaPlayer != null) {
                try {
                    stopNotificationSound();
                    mediaPlayer.release();
                } catch (Exception e) {
                    Log.e(TAG, "Error releasing MediaPlayer: " + e.getMessage());
                } finally {
                    mediaPlayer = null;
                }
            }

            // Remove floating window with error handling
            if (windowManager != null && floatingView != null && floatingView.isAttachedToWindow()) {
                try {
                    windowManager.removeView(floatingView);
                } catch (Exception e) {
                    Log.e(TAG, "Error removing window: " + e.getMessage());
                } finally {
                    floatingView = null;
                    windowManager = null;
                }
            }

            // Cancel all network requests
            try {
                VolleySingleton.getInstance(this).getRequestQueue().cancelAll(TAG);
            } catch (Exception e) {
                Log.e(TAG, "Error cancelling requests: " + e.getMessage());
            }

            // Stop foreground first
            stopForeground(true);

            // Clear any saved references
            preferenceManager = null;
            bookingJsonDetails = null;
            bookingId = null;

            // Call super last
            super.onDestroy();

            // Force stop the service
            stopSelf();

            // Kill the process if needed (last resort)
            android.os.Process.killProcess(android.os.Process.myPid());

        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage());
            // Force stop even if there's an error
            stopSelf();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}