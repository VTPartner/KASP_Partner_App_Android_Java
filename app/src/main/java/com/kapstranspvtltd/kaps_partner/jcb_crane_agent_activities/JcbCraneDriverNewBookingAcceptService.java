package com.kapstranspvtltd.kaps_partner.jcb_crane_agent_activities;

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
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.jcb_crane_agent_activities.settings_pages.JcbCraneNewLiveRideActivity;
import com.kapstranspvtltd.kaps_partner.fcm.AccessToken;
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

public class JcbCraneDriverNewBookingAcceptService extends Service {
    private static final String TAG = "JcbCraneFloatingWindow";
    private static final int NOTIFICATION_ID = 54324;
    private static final String CHANNEL_ID = "jcb_crane_floating_window_channel";

    private MediaPlayer mediaPlayer;
    private WindowManager windowManager;
    private View floatingView;
    private CountDownTimer countDownTimer;
    private PreferenceManager preferenceManager;
    private String bookingId;
    private JSONObject bookingJsonDetails;

    private static View currentFloatingView = null;
    private static WindowManager currentWindowManager = null;

    @Override
    public void onCreate() {
        super.onCreate();
        preferenceManager = new PreferenceManager(this);
        createNotificationChannel();
        initializeMediaPlayer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification());

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
                    "JCB/Crane Booking Alert Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );

            serviceChannel.setDescription("Shows notifications for new JCB/Crane booking requests");
            serviceChannel.enableLights(true);
            serviceChannel.setLightColor(Color.BLUE);
            serviceChannel.enableVibration(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, JcbCraneNewLiveRideActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("New JCB/Crane Booking Alert")
                .setContentText("You have a new booking request")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .build();
    }

    private void fetchBookingDetails(String bookingId) {
        new Thread(() -> {
            try {
                String serverToken = AccessToken.getAccessToken();
                String url = APIClient.baseUrl + "jcb_crane_booking_details_for_ride_acceptance";

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

        // Remove any existing floating window before adding a new one
        if (currentWindowManager != null && currentFloatingView != null) {
            try {
                currentWindowManager.removeView(currentFloatingView);
            } catch (Exception e) {
                // Ignore if already removed
            }
            currentFloatingView = null;
            currentWindowManager = null;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        floatingView = inflater.inflate(R.layout.jcb_crane_new_booking_accept_layout, null);

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
        params.height = (int) (screenHeight * 0.7);

        windowManager.addView(floatingView, params);

        //To avoid multiple windows shown at a time
        currentFloatingView = floatingView;
        currentWindowManager = windowManager;
        setupUI();
    }

    private void setupUI() {
        try {
            TextView timerText = floatingView.findViewById(R.id.timerText);
            TextView workLocationView = floatingView.findViewById(R.id.workLocation); // Changed from pickupAddress
            TextView customerNameView = floatingView.findViewById(R.id.customerName);
            TextView serviceFareView = floatingView.findViewById(R.id.serviceFare); // Changed from rideFare
            TextView distanceView = floatingView.findViewById(R.id.distance);
            TextView workLocationDistanceView = floatingView.findViewById(R.id.workLocationDistance); // Changed from pickupLocationDistance
            TextView serviceTypeView = floatingView.findViewById(R.id.serviceType); // New view for service type
            Button acceptButton = floatingView.findViewById(R.id.acceptButton);
            Button rejectButton = floatingView.findViewById(R.id.rejectButton);

            startCountdownTimer(timerText);

            acceptButton.setOnClickListener(v -> handleAcceptBooking());
            rejectButton.setOnClickListener(v -> handleRejectBooking());

            String workLocation = bookingJsonDetails.optString("pickup_address", "N/A"); // Work location is pickup_address
            String customerName = bookingJsonDetails.optString("customer_name", "N/A");
            double totalPrice = bookingJsonDetails.optDouble("total_price", 0);
            String subCatName = bookingJsonDetails.optString("sub_cat_name", "N/A");
            String serviceName = bookingJsonDetails.optString("service_name", "N/A");

            workLocationView.setText(workLocation);
            customerNameView.setText(customerName);
            serviceFareView.setText(String.format("₹%.0f", totalPrice));
            serviceTypeView.setText(String.format("%s - %s", subCatName, serviceName));

            calculateWorkLocationDistance(workLocationDistanceView);

        } catch (Exception e) {
            Log.e(TAG, "Error setting up UI: " + e.getMessage());
        }
    }

    private void calculateWorkLocationDistance(TextView distanceView) {
        try {
            double workLat = bookingJsonDetails.optDouble("pickup_lat", 0);
            double workLng = bookingJsonDetails.optDouble("pickup_lng", 0);

            if (workLat == 0 || workLng == 0) {
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
                                workLat,
                                workLng,
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
                String driverId = preferenceManager.getStringValue("jcb_crane_agent_id");
                String accessToken = AccessToken.getAccessToken();

                if (accessToken == null || accessToken.isEmpty()) {
                    handler.post(() -> {
                        showToast("No Token Found!");
                        stopSelf();
                    });
                    return;
                }

                preferenceManager.saveStringValue("current_jcb_crane_booking_id_assigned", bookingId);

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("booking_id", bookingId);
                jsonBody.put("driver_id", driverId);
                jsonBody.put("server_token", accessToken);
                jsonBody.put("customer_id", bookingJsonDetails.optString("customer_id"));

                String url = APIClient.baseUrl + "jcb_crane_driver_booking_accepted";

                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        jsonBody,
                        response -> {
                            Intent intent = new Intent(this, JcbCraneNewLiveRideActivity.class);
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


    private void handleRejectBooking() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        stopNotificationSound();
        stopSelf();
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
        preferenceManager.saveStringValue("current_other_booking_id_assigned", "");
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
        super.onDestroy();
        stopNotificationSound();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

//        if (windowManager != null && floatingView != null) {
//            windowManager.removeView(floatingView);
//        }

        // Remove floating view if it is the current one
        if (windowManager != null && floatingView != null) {
            try {
                windowManager.removeView(floatingView);
            } catch (Exception e) {
                // Ignore if already removed
            }
            if (currentFloatingView == floatingView) {
                currentFloatingView = null;
                currentWindowManager = null;
            }
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}