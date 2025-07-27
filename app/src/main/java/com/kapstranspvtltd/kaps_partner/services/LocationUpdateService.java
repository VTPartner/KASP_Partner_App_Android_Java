package com.kapstranspvtltd.kaps_partner.services;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.messaging.FirebaseMessaging;
import com.kapstranspvtltd.kaps_partner.SplashScreenActivity;
import com.kapstranspvtltd.kaps_partner.fcm.AccessToken;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;
import com.kapstranspvtltd.kaps_partner.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationUpdateService extends Service {
    public LocationUpdateService() {
    }
    private static final String TAG = "LocationUpdateService";
    private static final String CHANNEL_ID = "location_service_channel";
    private static final int NOTIFICATION_ID = 12345;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private PreferenceManager preferenceManager;
    private VolleySingleton volleySingleton;

    private static Location lastKnownLocation = null;
    private static final Object locationLock = new Object();

    private Handler tokenHandler;
    private Runnable tokenRunnable;
    private static final long TOKEN_REFRESH_INTERVAL = 2 * 60 * 1000L; // 2 minutes

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        preferenceManager = new PreferenceManager(this);
        volleySingleton = VolleySingleton.getInstance(this);
        createNotificationChannel();

        // Starting the periodic token fetch
        startTokenRefreshLoop();
    }

    private void startTokenRefreshLoop() {
        tokenHandler = new Handler(Looper.getMainLooper());
        tokenRunnable = new Runnable() {
            @Override
            public void run() {
                getFCMToken();
                tokenHandler.postDelayed(this, TOKEN_REFRESH_INTERVAL);
            }
        };
        tokenHandler.post(tokenRunnable); // Start the first run
    }


    private void getFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    System.out.println("Driver device token::" + token);
                    preferenceManager.saveStringValue("goods_driver_token", token);
                    updateDriverAuthToken(token);
                });
    }

    private void updateDriverAuthToken(String deviceToken) {
        Log.d("FCMNewTokenFound", "updating goods driver authToken");

        String driverId = preferenceManager.getStringValue("goods_driver_id");
        String cabDriverId = preferenceManager.getStringValue("cab_driver_id");
        if (driverId.isEmpty() || deviceToken == null || deviceToken.isEmpty()) {

            return;
        }


        // Using ExecutorService instead of Coroutines
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                String serverToken = AccessToken.getAccessToken();
                System.out.println("deviceToken::" + deviceToken);
                System.out.println("------------");
                System.out.println("serverToken::" + serverToken);
                String url = APIClient.baseUrl + "update_firebase_goods_driver_token";

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("goods_driver_id", driverId);
                jsonBody.put("authToken", deviceToken);

                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        jsonBody,
                        response -> {
                            String message = response.optString("message");
                            Log.d("Auth", "FCM Token update from LocationUpdateService response: " + message);

                        },
                        error -> {
                            Log.e("Auth", "Error updating token: " + error.getMessage());
                            error.printStackTrace();
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

                // Add retry policy
                request.setRetryPolicy(new DefaultRetryPolicy(
                        30000,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                ));

                // Execute on main thread
                handler.post(() -> {
                    VolleySingleton.getInstance(getApplicationContext())
                            .addToRequestQueue(request);
                });

            } catch (Exception e) {
                Log.e("Auth", "Error in token update process: " + e.getMessage());
                e.printStackTrace();
            }
        });

        // Shutdown executor after use
        executor.shutdown();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification());
        startFloatingWindow();
        requestLocationUpdates();
        return START_STICKY;
    }

    private void startFloatingWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            // Request overlay permission
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            // Start floating window service
            Intent floatingIntent = new Intent(this, FloatingWindowService.class);
            startService(floatingIntent); // Use startService instead of startForegroundService
        }
    }

    public static Location getLastKnownLocation() {
        synchronized (locationLock) {
            return lastKnownLocation;
        }
    }

    private void requestLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)  // 10 seconds
                .setFastestInterval(5000); // 5 seconds

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    // Store location in thread-safe way
                    synchronized (locationLock) {
                        lastKnownLocation = location;
                    }
                    updateLocationToServer(location);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if(fusedLocationClient != null) {
                fusedLocationClient.requestLocationUpdates(locationRequest,
                        locationCallback,
                        Looper.getMainLooper());
            }
        }
    }

    private Location lastSentLocation = null;
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATE = 1.0f; // 3 meters

    private void updateLocationToServer(Location location) {
        if (location == null) return;
// Broadcast location update
        Intent intent = new Intent("driver_location_update");
        intent.putExtra("latitude", location.getLatitude());
        intent.putExtra("longitude", location.getLongitude());
        sendBroadcast(intent);
        // Get driver ID
        String driverId = preferenceManager.getStringValue("goods_driver_id");
        if (driverId.isEmpty()) {
            Log.e(TAG, "Driver ID not found in preferences, can't update location");
            return;
        }

        // Get previously saved location from preferences
        String lastLat = preferenceManager.getStringValue("goods_driver_current_lat");
        String lastLng = preferenceManager.getStringValue("goods_driver_current_lng");

        // Check if this is the first location update since service started
        boolean isFirstUpdate = lastSentLocation == null;

        // If this is the first update or we don't have previous coordinates, always send it
        if (isFirstUpdate || lastLat.isEmpty() || lastLng.isEmpty()) {
            Log.d(TAG, "First location update or no previous location, sending update");
            sendLocationUpdate(location);
            lastSentLocation = location;
            return;
        }

        try {
            // Calculate distance between current and last sent location
            float distance = location.distanceTo(lastSentLocation);

            // Only update if the distance is greater than minimum threshold
            if (distance >= MIN_DISTANCE_CHANGE_FOR_UPDATE) {
                Log.d(TAG, "Distance changed: " + distance + " meters, sending update");
                sendLocationUpdate(location);
                lastSentLocation = location; // Update the last sent location
            } else {
                Log.d(TAG, "Location change too small, skipping update. Distance: " + distance + " meters");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing location update, sending update anyway", e);
            sendLocationUpdate(location);
            lastSentLocation = location;
        }
    }

    private void sendLocationUpdate(Location location) {
        String url = APIClient.baseUrl + "update_goods_drivers_current_location";
        String driverId = preferenceManager.getStringValue("goods_driver_id");

        JSONObject params = new JSONObject();
        try {
            params.put("goods_driver_id", driverId);
            params.put("lat", location.getLatitude());
            params.put("lng", location.getLongitude());

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    params,
                    response -> {
                        Log.d(TAG, "Location updated successfully");
                        // Save the new location after successful update
                        preferenceManager.saveStringValue("goods_driver_current_lat",
                                String.valueOf(location.getLatitude()));
                        preferenceManager.saveStringValue("goods_driver_current_lng",
                                String.valueOf(location.getLongitude()));
                    },
                    error -> Log.e(TAG, "Error updating location: " + error.toString())
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

            volleySingleton.addToRequestQueue(request);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating location update request", e);
        }
    }

//    private void updateLocationToServer(Location location) {
//        if(location == null )return;
//        String url = APIClient.baseUrl + "update_goods_drivers_current_location";
//        String driverId = preferenceManager.getStringValue("goods_driver_id");
//
//        JSONObject params = new JSONObject();
//        try {
//            params.put("goods_driver_id", driverId);
//            params.put("lat", location.getLatitude());
//            params.put("lng", location.getLongitude());
//
//            JsonObjectRequest request = new JsonObjectRequest(
//                    Request.Method.POST,
//                    url,
//                    params,
//                    response -> {
//                        Log.d(TAG, "Location updated successfully");
//                        preferenceManager.saveStringValue("goods_driver_current_lat", location.getLatitude()+"");
//                        preferenceManager.saveStringValue("goods_driver_current_lng", location.getLongitude()+"");
//                    },
//                    error -> Log.e(TAG, "Error updating location: " + error.toString())
//            );
//
//            request.setRetryPolicy(new DefaultRetryPolicy(
//                    30000,
//                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
//                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
//            ));
//
//            volleySingleton.addToRequestQueue(request);
//        } catch (JSONException e) {
//            Log.e(TAG, "Error creating location update request", e);
//        }
//    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, SplashScreenActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("KAPS Partner")
                .setContentText("Updating your location...")
                .setSmallIcon(R.drawable.logo)
                .setContentIntent(pendingIntent)
                .build();
    }

    public static boolean isStoppedManually = false;

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);


        if (!isStoppedManually) {
            Intent restartService = new Intent(getApplicationContext(), LocationUpdateService.class);
            PendingIntent pendingIntent = PendingIntent.getService(
                    getApplicationContext(),
                    1,
                    restartService,
                    PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000, pendingIntent);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (tokenHandler != null && tokenRunnable != null) {
            tokenHandler.removeCallbacks(tokenRunnable);
        }
        // Always remove location updates to avoid duplicates
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        // Restart only if not manually stopped
        if (!isStoppedManually) {
            Log.d(TAG, "Service destroyed unexpectedly, restarting...");

            Intent restartServiceIntent = new Intent(getApplicationContext(), LocationUpdateService.class);
            PendingIntent pendingIntent = PendingIntent.getService(
                    getApplicationContext(),
                    1,
                    restartServiceIntent,
                    PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 1000,
                    pendingIntent
            );
        }

        // Clear the last known location
        synchronized (locationLock) {
            lastKnownLocation = null;
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
