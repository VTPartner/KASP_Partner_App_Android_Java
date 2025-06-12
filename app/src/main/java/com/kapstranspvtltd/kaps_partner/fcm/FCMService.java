package com.kapstranspvtltd.kaps_partner.fcm;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.kapstranspvtltd.kaps_partner.GoodsNewBookingFloatingWindowService;
import com.kapstranspvtltd.kaps_partner.SplashScreenActivity;
import com.kapstranspvtltd.kaps_partner.cab_driver_activities.CabDriverHomeActivity;
import com.kapstranspvtltd.kaps_partner.cab_driver_activities.CabNewBookingAcceptService;
import com.kapstranspvtltd.kaps_partner.cab_driver_activities.bookings.CabBookingAcceptActivity;
import com.kapstranspvtltd.kaps_partner.driver_app_activities.DriverAgentHomeActivity;
import com.kapstranspvtltd.kaps_partner.driver_app_activities.DriverAgentNewBookingAcceptService;
import com.kapstranspvtltd.kaps_partner.fcm.popups.GoodsBookingAcceptActivity;
import com.kapstranspvtltd.kaps_partner.goods_driver_activities.HomeActivity;
import com.kapstranspvtltd.kaps_partner.handyman_agent_activities.HandyManAgentHomeActivity;
import com.kapstranspvtltd.kaps_partner.handyman_agent_activities.HandyManNewBookingAcceptService;
import com.kapstranspvtltd.kaps_partner.jcb_crane_agent_activities.JcbCraneDriverNewBookingAcceptService;
import com.kapstranspvtltd.kaps_partner.jcb_crane_agent_activities.JcbCraneHomeActivity;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.services.FloatingWindowService;
import com.kapstranspvtltd.kaps_partner.services.LocationUpdateService;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;
import com.kapstranspvtltd.kaps_partner.R;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;




public class FCMService extends FirebaseMessagingService {
    private static final String TAG = "FCMService";
    private static final String BOOKING_CHANNEL_ID = "booking_notifications";

    private PreferenceManager preferenceManager;
    private NotificationManager notificationManager;
    private MediaPlayer mediaPlayer;

    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        preferenceManager = new PreferenceManager(this);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
        initializeMediaPlayer();
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
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    stopNotificationSound();
                }, 15000);
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

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        updateDriverToken(token);
    }

    private void updateDriverToken(String token) {
        String driverId = preferenceManager.getStringValue("goods_driver_id");
        String cabDriverId = preferenceManager.getStringValue("cab_driver_id");
        String otherDriverId = preferenceManager.getStringValue("other_driver_id");
        String jcbCraneAgentId = preferenceManager.getStringValue("jcb_crane_agent_id");
        String handymanAgentId = preferenceManager.getStringValue("handyman_agent_id");


        String url = "";
        String key = "", value = "";
        String tokenKey; // Key for saving token in preferences

        if (!driverId.isEmpty()) {
            url = APIClient.baseUrl + "update_firebase_goods_driver_token";
            key = "goods_driver_id";
            value = driverId;
            tokenKey = "goods_driver_token";
        } else if (!cabDriverId.isEmpty()) {
            url = APIClient.baseUrl + "update_firebase_cab_driver_token";
            key = "cab_driver_id";
            value = cabDriverId;
            tokenKey = "cab_driver_token";
        } else if (!otherDriverId.isEmpty()) {
            url = APIClient.baseUrl + "update_firebase_other_driver_token";
            key = "other_driver_id";
            value = otherDriverId;
            tokenKey = "other_driver_token";
        } else if (!jcbCraneAgentId.isEmpty()) {
            url = APIClient.baseUrl + "update_firebase_jcb_crane_driver_token";
            key = "jcb_crane_driver_id";
            value = jcbCraneAgentId;
            tokenKey = "jcb_crane_token";
        } else if (!handymanAgentId.isEmpty()) {
            url = APIClient.baseUrl + "update_firebase_handyman_token";
            key = "handyman_id";
            value = handymanAgentId;
            tokenKey = "handyman_token";
        } else {
            tokenKey = "";
        }

        // If no valid driver type found, return
        if (url.isEmpty() || token.isEmpty()) return;



        JSONObject params = new JSONObject();
        try {
            params.put(key, value);
            params.put("authToken", token);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    params,
                    response -> {
                        Log.d(TAG, "Token updated successfully");
                        // Save token based on driver type
                        preferenceManager.saveStringValue(tokenKey, token);
                    },
                    error -> {
                        Log.e(TAG, "Error updating token: " + error.getMessage());
                        // You might want to handle error here
                    }
            );

            VolleySingleton.getInstance(this).addToRequestQueue(request);
        } catch (Exception e) {
            Log.e(TAG, "Error creating token update request: " + e.getMessage());
        }
    }

//    private void updateDriverToken(String token) {
//        String driverId = preferenceManager.getStringValue("goods_driver_id");
//        String cabDriverId = preferenceManager.getStringValue("cab_driver_id");
//        String otherDriverId = preferenceManager.getStringValue("other_driver_id");
//        String jcbCraneAgentId = preferenceManager.getStringValue("jcb_crane_agent_id");
//        String handymanAgentId = preferenceManager.getStringValue("handyman_agent_id");
////        if (driverId.isEmpty() || token.isEmpty()) return;
//        String url = "";
//        String key = "",value="";
//        if(driverId.isEmpty() == false) {
//            url = APIClient.baseUrl + "update_firebase_goods_driver_token";
//            key = "goods_driver_id";
//            value = driverId;
//        }
//        if(cabDriverId.isEmpty() == false){
//            url = APIClient.baseUrl + "update_firebase_cab_driver_token";
//            key = "cab_driver_id";
//            value = cabDriverId;
//        }
//        if(otherDriverId.isEmpty() == false){
//            url = APIClient.baseUrl + "update_firebase_other_driver_token";
//            key = "other_driver_id";
//            value = otherDriverId;
//        }
//        if(jcbCraneAgentId.isEmpty() == false){
//            url = APIClient.baseUrl + "update_firebase_jcb_crane_driver_token";
//            key = "jcb_crane_driver_id";
//            value = jcbCraneAgentId;
//        }
//        if(handymanAgentId.isEmpty() == false){
//            url = APIClient.baseUrl + "update_firebase_handyman_token";
//            key = "handyman_id";
//            value = handymanAgentId;
//        }
//
//        JSONObject params = new JSONObject();
//        try {
//            params.put(key, value);
//            params.put("authToken", token);
//
//            JsonObjectRequest request = new JsonObjectRequest(
//                    Request.Method.POST,
//                    url,
//                    params,
//                    response -> Log.d(TAG, "Token updated successfully"),
//                    error -> Log.e(TAG, "Error updating token: " + error.getMessage())
//            );
//
//            VolleySingleton.getInstance(this).addToRequestQueue(request);
//        } catch (Exception e) {
//            Log.e(TAG, "Error creating token update request: " + e.getMessage());
//        }
//    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Map<String, String> data = remoteMessage.getData();
        Log.d(TAG, "Received FCM message: " + data);

        // Acquire wake lock to ensure processing
        acquireWakeLock();

        try {
            if ("driver".equals(data.get("intent"))) {
                String bookingId = data.get("booking_id");
                System.out.println("bookingId::" + bookingId);

                // Launch activity on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    handleDriverBooking(data);
                });
            } else if ("cab_driver".equals(data.get("intent"))) {
                String bookingId = data.get("booking_id");
                System.out.println("cab driver new booking request bookingId::" + bookingId);

                // Launch activity on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    handleCabDriverBooking(data);
                });
            }else if ("driver_agent".equals(data.get("intent"))) {
                String bookingId = data.get("booking_id");
                System.out.println("driver agent new booking request bookingId::" + bookingId);

                // Launch activity on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    handleOtherDriverBooking(data);
                });
            }else if ("jcb_crane_driver".equals(data.get("intent"))) {
                String bookingId = data.get("booking_id");
                System.out.println("jcb crane driver agent new booking request bookingId::" + bookingId);

                // Launch activity on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    handleJcbCraneDriverBooking(data);
                });
            }else if ("handy_man_agent".equals(data.get("intent"))) {
                String bookingId = data.get("booking_id");
                System.out.println("Handyman agent new booking request bookingId::" + bookingId);

                // Launch activity on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    handleHandymanAgentNewBooking(data);
                });
            }

            //Below intents are for cancelled bookings
            else if ("driver_home".equals(data.get("intent"))) {
                String bookingId = data.get("booking_id");
                String title = data.get("title");
                String body = data.get("body");
                System.out.println("driver home bookingId::" + bookingId);
// Create notification channel for Android O and above
                createDriverNotificationChannel();

                // Show notification
                showNotification(title, body);
                // Launch activity on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    Intent intent = new Intent(this, HomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                });
            }
            else if ("cab_driver_home".equals(data.get("intent"))) {
                String bookingId = data.get("booking_id");
                String title = data.get("title");
                String body = data.get("body");
                System.out.println("cab driver home bookingId::" + bookingId);
// Create notification channel for Android O and above
                createDriverNotificationChannel();

                // Show notification
//                showNotification("Cab Ride Canceled - [Booking ID: "+ bookingId.toString()+"]", "The ride request has been canceled by the customer!");
                showNotification(title, body);
                // Launch activity on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    Intent intent = new Intent(this, CabDriverHomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                });
            }
            else if ("other_driver_home".equals(data.get("intent"))) {
                String bookingId = data.get("booking_id");
                String title = data.get("title");
                String body = data.get("body");
                System.out.println("other driver home bookingId::" + bookingId);
// Create notification channel for Android O and above
                createDriverNotificationChannel();

                // Show notification
//                showNotification("Ride Canceled - [Booking ID: "+ bookingId.toString()+"]", "The ride request has been canceled by the customer!");
                showNotification(title, body);
                // Launch activity on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    Intent intent = new Intent(this, DriverAgentHomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                });
            }
            else if ("jcb_crane_driver_home".equals(data.get("intent"))) {
                String bookingId = data.get("booking_id");
                String title = data.get("title");
                String body = data.get("body");
                System.out.println("jcb / crane driver home bookingId::" + bookingId);
// Create notification channel for Android O and above
                createDriverNotificationChannel();

                // Show notification
//                showNotification("Service Canceled - [Booking ID: "+ bookingId.toString()+"]", "The service request has been canceled by the customer!");
                showNotification(title, body);
                // Launch activity on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    Intent intent = new Intent(this, JcbCraneHomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                });
            }
            else if ("handyman_agent_home".equals(data.get("intent"))) {
                String bookingId = data.get("booking_id");
                String title = data.get("title");
                String body = data.get("body");
                System.out.println("Handyman Agent home bookingId::" + bookingId);
// Create notification channel for Android O and above
                createDriverNotificationChannel();

                // Show notification
//                showNotification("Service Canceled - [Booking ID: "+ bookingId.toString()+"]", "The service request has been canceled by the customer!");
                showNotification(title, body);
                // Launch activity on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    Intent intent = new Intent(this, HandyManAgentHomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                });
            }
            else {
                showRegularNotification(data);
            }
        } finally {
            releaseWakeLock();
        }
    }

    private void handleOtherDriverBooking(Map<String, String> data) {
        String bookingId = data.get("booking_id");
        if (bookingId == null || bookingId.isEmpty()) {
            Log.e(TAG, "Invalid booking ID received");
            return;
        }



        // Check if app is in foreground
        boolean isAppForeground = isAppInForeground();
        FloatingWindowService floatingService = FloatingWindowService.getInstance();

        if (isAppForeground || floatingService != null) {
            System.out.println("floatingService is " + (floatingService != null ? "not null" : "null"));
            try {
                Intent serviceIntent = new Intent(this, DriverAgentNewBookingAcceptService.class);
                serviceIntent.putExtra("booking_id", bookingId);
                serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Log.d(TAG, "Starting FloatingWindowService with extras");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
//                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error launching activity directly: " + e.getMessage());
                showBookingNotification(bookingId, data);

            }
        } else {
            showBookingNotification(bookingId, data);

        }
    }

    private void handleJcbCraneDriverBooking(Map<String, String> data) {
        String bookingId = data.get("booking_id");
        if (bookingId == null || bookingId.isEmpty()) {
            Log.e(TAG, "Invalid booking ID received");
            return;
        }



        // Check if app is in foreground
        boolean isAppForeground = isAppInForeground();
        FloatingWindowService floatingService = FloatingWindowService.getInstance();

        if (isAppForeground || floatingService != null) {
            System.out.println("floatingService is " + (floatingService != null ? "not null" : "null"));
            try {
                Intent serviceIntent = new Intent(this, JcbCraneDriverNewBookingAcceptService.class);
                serviceIntent.putExtra("booking_id", bookingId);
                serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Log.d(TAG, "Starting FloatingWindowService with extras");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
//                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error launching activity directly: " + e.getMessage());
                showBookingNotification(bookingId, data);

            }
        } else {
            showBookingNotification(bookingId, data);

        }
    }

    private void handleHandymanAgentNewBooking(Map<String, String> data) {
        String bookingId = data.get("booking_id");
        if (bookingId == null || bookingId.isEmpty()) {
            Log.e(TAG, "Invalid booking ID received");
            return;
        }



        // Check if app is in foreground
        boolean isAppForeground = isAppInForeground();
        FloatingWindowService floatingService = FloatingWindowService.getInstance();

        if (isAppForeground || floatingService != null) {
            System.out.println("floatingService is " + (floatingService != null ? "not null" : "null"));
            try {
                Intent serviceIntent = new Intent(this, HandyManNewBookingAcceptService.class);
                serviceIntent.putExtra("booking_id", bookingId);
                serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Log.d(TAG, "Starting FloatingWindowService with extras");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
//                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error launching activity directly: " + e.getMessage());
                showBookingNotification(bookingId, data);

            }
        } else {
            showBookingNotification(bookingId, data);

        }
    }


    private void createDriverNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "driver_home_channel";
            CharSequence channelName = "Driver Home Notifications";
            String channelDescription = "Notifications for driver home updates";

            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription(channelDescription);
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500});

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification(String title, String message) {
        String channelId = "driver_home_channel";

        // Create an explicit intent for the HomeActivity
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Create pending intent
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.logo) // Make sure to have this icon in your drawable
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        // Add sound
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notificationBuilder.setSound(defaultSoundUri);

        // Show the notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            // Use a unique notification ID
            int notificationId = (int) System.currentTimeMillis();
            notificationManager.notify(notificationId, notificationBuilder.build());
        }
    }
    private void handleCabDriverBooking(Map<String, String> data) {
        String bookingId = data.get("booking_id");
        if (bookingId == null || bookingId.isEmpty()) {
            Log.e(TAG, "Invalid booking ID received");
            return;
        }

        // Create pending intent for notification
        Intent intent = new Intent(this, CabBookingAcceptActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("booking_id", bookingId);

        // Check if app is in foreground
        boolean isAppForeground = isAppInForeground();
        FloatingWindowService floatingService = FloatingWindowService.getInstance();

        if (isAppForeground || floatingService != null) {
            System.out.println("floatingService is " + (floatingService != null ? "not null" : "null"));
            try {
                Intent serviceIntent = new Intent(this, CabNewBookingAcceptService.class);
                serviceIntent.putExtra("booking_id", bookingId);
                serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Log.d(TAG, "Starting FloatingWindowService with extras");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
//                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error launching activity directly: " + e.getMessage());
                showBookingNotification(bookingId, data);

            }
        } else {
            showBookingNotification(bookingId, data);

        }
    }

    private void handleDriverBooking(Map<String, String> data) {
        String bookingId = data.get("booking_id");
        if (bookingId == null || bookingId.isEmpty()) {
            Log.e(TAG, "Invalid booking ID received");
            return;
        }

        // Create pending intent for notification
        Intent intent = new Intent(this, GoodsNewBookingFloatingWindowService.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("booking_id", bookingId);

        // Check if app is in foreground
//        boolean isAppForeground = isAppInForeground();
        boolean isAppForeground = true;
        FloatingWindowService floatingService = FloatingWindowService.getInstance();

        if (isAppForeground || floatingService != null) {
            System.out.println("floatingService is " + (floatingService != null ? "not null" : "null"));
            try {
                Intent serviceIntent = new Intent(this, GoodsNewBookingFloatingWindowService.class);
                serviceIntent.putExtra("booking_id", bookingId);
                serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Log.d(TAG, "Starting FloatingWindowService with extras");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
//                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error launching activity directly: " + e.getMessage());
                showBookingNotification(bookingId, data);

            }
        } else {
            showBookingNotification(bookingId, data);

        }
    }

    private void startLocationUpdates() {
        // Start LocationService
        Intent serviceIntent = new Intent(this, LocationUpdateService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void stopLocationUpdates() {
        LocationUpdateService.isStoppedManually = true;
        Intent serviceIntent = new Intent(this, LocationUpdateService.class);
        stopService(serviceIntent);
        Intent floatingIntent = new Intent(this, FloatingWindowService.class);
        stopService(floatingIntent);
    }
    private boolean isAppInForeground() {
        return true;
//        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
//        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
//        if (appProcesses == null) return false;
//
//        String packageName = getPackageName();
//        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
//            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
//                    && appProcess.processName.equals(packageName)) {
//                return true;
//            }
//        }
//        return false;
    }

    private void showBookingNotification(String bookingId, Map<String, String> data) {
        String channelId = "booking_channel";
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Booking Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setSound(null, null);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{1000, 1000});
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, GoodsBookingAcceptActivity.class);
        intent.putExtra("booking_id", bookingId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                Integer.parseInt(bookingId), // Use booking ID as request code
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("New Booking Request")
                .setContentText("Tap to view booking details")
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(Integer.parseInt(bookingId), builder.build());
    }

    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "FCMService:WakeLock");
        wakeLock.acquire(10*60*1000L /*10 minutes*/);
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

//    @Override
//    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
//        super.onMessageReceived(remoteMessage);
//        Map<String, String> data = remoteMessage.getData();
//        Log.d(TAG, "Received FCM message: " + data);
//
//        if ("driver".equals(data.get("intent"))) {
//            String bookingId = data.get("booking_id");
//            handleDriverBooking(data);
////            showBookingNotification(bookingId, data);
//        } else {
//            showRegularNotification(data);
//        }
//    }
//
//    private void handleDriverBooking(Map<String, String> data) {
//        String bookingId = data.get("booking_id");
//        if (bookingId == null || bookingId.isEmpty()) {
//            Log.e(TAG, "Invalid booking ID received");
//            return;
//        }
//        System.out.println("bookingId::"+bookingId);
//
//        // Check if floating service is running
//        FloatingWindowService floatingService = FloatingWindowService.getInstance();
//
//        if (floatingService != null) {
//            System.out.println("floatingService is not null");
//            // If service is running, use it to show booking activity
//            floatingService.handleBookingNotification(bookingId);
//        } else {
//            // If service is not running, show notification or start activity directly
//            Intent intent = new Intent(this, GoodsBookingAcceptActivity.class);
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            intent.putExtra("booking_id", bookingId);
//            startActivity(intent);
//        }
//
//        // Create the intent for the booking activity
////        Intent intent = new Intent(this, GoodsBookingAcceptActivity.class);
////        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
////        intent.putExtra("booking_id", bookingId);
////        // Start the activity
////        startActivity(intent);
//    }
//
//

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    BOOKING_CHANNEL_ID,
                    "Booking Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription("Notifications for new bookings");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 1000});
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setBypassDnd(true);

            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showRegularNotification(Map<String, String> data) {
        String bookingId = data.get("booking_id");
        Intent intent;
        if (bookingId != null && bookingId.isEmpty() == false) {
            Log.e(TAG, "Invalid booking ID received");
            // Create pending intent for notification
            intent = new Intent(this, GoodsBookingAcceptActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("booking_id", bookingId);
        }else {
            intent = new Intent(this, SplashScreenActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }

        stopLocationUpdates();
        startLocationUpdates();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        // Set custom sound URI
        Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.booking_notification);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, BOOKING_CHANNEL_ID)
                .setContentTitle(data.get("title"))
                .setContentText(data.get("body"))
                .setSmallIcon(R.drawable.logo)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSound(soundUri) // Set custom sound
                .setVibrate(new long[]{0, 500, 200, 500}) // Optional vibration pattern
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS); // Keep default lights

        // For Android 8.0 (API 26) and above, set channel properties
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = notificationManager.getNotificationChannel(BOOKING_CHANNEL_ID);
            if (channel == null) {
                AudioAttributes attributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();

                channel = new NotificationChannel(
                        BOOKING_CHANNEL_ID,
                        "Regular Notifications",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setSound(soundUri, attributes);
                channel.enableVibration(true);
                channel.setVibrationPattern(new long[]{0, 500, 200, 500});
                notificationManager.createNotificationChannel(channel);
            }
        }

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    public static void cancelAllNotifications(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
            Log.d("FCMService", "All notifications cancelled");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopNotificationSound();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
