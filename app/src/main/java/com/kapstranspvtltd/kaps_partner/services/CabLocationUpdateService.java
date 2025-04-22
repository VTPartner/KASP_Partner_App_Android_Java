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

public class CabLocationUpdateService extends Service {
    public CabLocationUpdateService() {
    }

    private static final String TAG = "LocationUpdateService";
    private static final String CHANNEL_ID = "location_service_channel";
    private static final int NOTIFICATION_ID = 12345;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private PreferenceManager preferenceManager;
    private VolleySingleton volleySingleton;

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

   /* private void updateLocationToServer(Location location) {
        if(location == null )return;
        String url = APIClient.baseUrl + "update_cab_drivers_current_location";
        String driverId = preferenceManager.getStringValue("cab_driver_id");

        JSONObject params = new JSONObject();
        try {
            params.put("cab_driver_id", driverId);
            params.put("lat", location.getLatitude());
            params.put("lng", location.getLongitude());

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    params,
                    response -> {
                        Log.d(TAG, "Location updated successfully");
                        preferenceManager.saveStringValue("cab_driver_current_lat", location.getLatitude()+"");
                        preferenceManager.saveStringValue("cab_driver_current_lng", location.getLongitude()+"");
                    },
                    error -> Log.e(TAG, "Error updating location: " + error.toString())
            );

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
*/

    private void updateLocationToServer(Location location) {
        if (location == null) return;

        // Get all agent IDs from preferences
        String cabDriverId = preferenceManager.getStringValue("cab_driver_id");
        String otherDriverId = preferenceManager.getStringValue("other_driver_id");
        String jcbCraneAgentId = preferenceManager.getStringValue("jcb_crane_agent_id");
        String handymanAgentId = preferenceManager.getStringValue("handyman_agent_id");

        // Update location for each active agent type
        if (!cabDriverId.isEmpty()) {
            updateCabDriverLocation(location, cabDriverId);
        }
        if (!otherDriverId.isEmpty()) {
            updateOtherDriverLocation(location, otherDriverId);
        }
        if (!jcbCraneAgentId.isEmpty()) {
            updateJcbCraneDriverLocation(location, jcbCraneAgentId);
        }
        if (!handymanAgentId.isEmpty()) {
            updateHandymanLocation(location, handymanAgentId);
        }

        // Save current location in preferences for all active agents
        saveCurrentLocation(location);
    }

    private void updateCabDriverLocation(Location location, String driverId) {
        String url = APIClient.baseUrl + "update_cab_drivers_current_location";
        JSONObject params = new JSONObject();
        try {
            params.put("cab_driver_id", driverId);
            params.put("lat", location.getLatitude());
            params.put("lng", location.getLongitude());
            makeLocationUpdateRequest(url, params, "cab driver");
        } catch (JSONException e) {
            Log.e(TAG, "Error creating cab driver location update request", e);
        }
    }

    private void updateOtherDriverLocation(Location location, String driverId) {
        String url = APIClient.baseUrl + "update_other_drivers_current_location";
        JSONObject params = new JSONObject();
        try {
            params.put("other_driver_id", driverId);
            params.put("lat", location.getLatitude());
            params.put("lng", location.getLongitude());
            makeLocationUpdateRequest(url, params, "other driver");
        } catch (JSONException e) {
            Log.e(TAG, "Error creating other driver location update request", e);
        }
    }

    private void updateJcbCraneDriverLocation(Location location, String driverId) {
        String url = APIClient.baseUrl + "update_jcb_crane_drivers_current_location";
        JSONObject params = new JSONObject();
        try {
            params.put("jcb_crane_driver_id", driverId);
            params.put("lat", location.getLatitude());
            params.put("lng", location.getLongitude());
            makeLocationUpdateRequest(url, params, "JCB crane driver");
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JCB crane driver location update request", e);
        }
    }

    private void updateHandymanLocation(Location location, String handymanId) {
        String url = APIClient.baseUrl + "update_handymans_current_location";
        JSONObject params = new JSONObject();
        try {
            params.put("handyman_id", handymanId);
            params.put("lat", location.getLatitude());
            params.put("lng", location.getLongitude());
            makeLocationUpdateRequest(url, params, "handyman");
        } catch (JSONException e) {
            Log.e(TAG, "Error creating handyman location update request", e);
        }
    }

    private void makeLocationUpdateRequest(String url, JSONObject params, String agentType) {
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                params,
                response -> {
                    Log.d(TAG, "Location updated successfully for " + agentType);
                },
                error -> {
                    Log.e(TAG, "Error updating location for " + agentType + ": " + error.toString());
                    // Implement retry mechanism if needed
                    handleLocationUpdateError(error, agentType);
                }
        ) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000, // 30 seconds timeout
                2,     // 2 retry attempts
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        volleySingleton.addToRequestQueue(request);
    }

    private void saveCurrentLocation(Location location) {
        // Save location for all agent types
        preferenceManager.saveStringValue("current_lat", String.valueOf(location.getLatitude()));
        preferenceManager.saveStringValue("current_lng", String.valueOf(location.getLongitude()));

        // Save specific agent locations if they are active
        if (!preferenceManager.getStringValue("cab_driver_id").isEmpty()) {
            preferenceManager.saveStringValue("cab_driver_current_lat", String.valueOf(location.getLatitude()));
            preferenceManager.saveStringValue("cab_driver_current_lng", String.valueOf(location.getLongitude()));
        }
        if (!preferenceManager.getStringValue("other_driver_id").isEmpty()) {
            preferenceManager.saveStringValue("other_driver_current_lat", String.valueOf(location.getLatitude()));
            preferenceManager.saveStringValue("other_driver_current_lng", String.valueOf(location.getLongitude()));
        }
        if (!preferenceManager.getStringValue("jcb_crane_agent_id").isEmpty()) {
            preferenceManager.saveStringValue("jcb_crane_current_lat", String.valueOf(location.getLatitude()));
            preferenceManager.saveStringValue("jcb_crane_current_lng", String.valueOf(location.getLongitude()));
        }
        if (!preferenceManager.getStringValue("handyman_agent_id").isEmpty()) {
            preferenceManager.saveStringValue("handyman_current_lat", String.valueOf(location.getLatitude()));
            preferenceManager.saveStringValue("handyman_current_lng", String.valueOf(location.getLongitude()));
        }
    }

    private void handleLocationUpdateError(Exception error, String agentType) {
        // Log the error
        Log.e(TAG, "Location update failed for " + agentType, error);

        // You could implement specific error handling here
        // For example: retry logic, user notification, or error reporting

        // Save error state if needed
        preferenceManager.saveStringValue("last_location_error_" + agentType,
                error.getMessage() != null ? error.getMessage() : "Unknown error");
        preferenceManager.saveStringValue("last_location_error_time_" + agentType,
                String.valueOf(System.currentTimeMillis()));
    }
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