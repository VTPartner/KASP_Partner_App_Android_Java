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
import com.kapstranspvtltd.kaps_partner.SplashScreenActivity;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;
import com.kapstranspvtltd.kaps_partner.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

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

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        preferenceManager = new PreferenceManager(this);
        volleySingleton = VolleySingleton.getInstance(this);
        createNotificationChannel();
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
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
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
        if (isStoppedManually == false) {
            // Restart service if it wasn't manually stopped
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
        if(isStoppedManually && locationCallback !=null){
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
//
        if (locationCallback != null) {
            if (isStoppedManually == false)  {
                Intent serviceIntent = new Intent(this, LocationUpdateService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

                // Clear last known location when service is manually stopped
                synchronized (locationLock) {
                    lastKnownLocation = null;
                }
//                // Restart service if it wasn't manually stopped
//                Intent broadcastIntent = new Intent("com.vtpartnertranspvtltd.vt_partner.RESTART_SERVICE");
//                sendBroadcast(broadcastIntent);
//                fusedLocationClient.removeLocationUpdates(locationCallback);
            }

        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
