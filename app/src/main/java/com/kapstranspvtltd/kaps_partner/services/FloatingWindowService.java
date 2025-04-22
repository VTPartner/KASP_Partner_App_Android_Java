package com.kapstranspvtltd.kaps_partner.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessaging;
import com.kapstranspvtltd.kaps_partner.GoodsNewBookingFloatingWindowService;
import com.kapstranspvtltd.kaps_partner.SplashScreenActivity;
import com.kapstranspvtltd.kaps_partner.fcm.popups.GoodsBookingAcceptActivity;
import com.kapstranspvtltd.kaps_partner.R;


public class FloatingWindowService extends Service {
    private WindowManager windowManager;
    private static FloatingWindowService instance;
    private View floatingView;
    private WindowManager.LayoutParams params;


    private static final String TAG = "BackgroundService";
    private static final String CHANNEL_ID = "kaps_background_channel";
    private static final int NOTIFICATION_ID = 1002;
    private static final String PREFS_NAME = "KapsPrefs";
    private static final String TOPIC_ALL = "all";

    private static final String KEY_BACKGROUND_SERVICE = "background_service_enabled";
    private static final String ACTION_DRIVER_MESSAGE = "com.kapstranspvtltd.kaps_partner.DRIVER_MESSAGE";


    // Add static method to get instance
    public static FloatingWindowService getInstance() {
        return instance;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        createFloatingWindow();
        Log.d(TAG, "BackgroundService created");
        createNotificationChannel();
        subscribeToTopic();
    }

    private void subscribeToTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_ALL)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Successfully subscribed to topic: " + TOPIC_ALL);
                    } else {
                        Log.e(TAG, "Failed to subscribe to topic: " + TOPIC_ALL, task.getException());
                    }
                });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "BackgroundService started with flags: " + flags + ", startId: " + startId);

        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification());
        Log.d(TAG, "Started as foreground service");

        // Get FCM token
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        Log.d(TAG, "FCM Token: " + token);
                    } else {
                        Log.e(TAG, "Failed to get FCM token", task.getException());
                    }
                });

        // Handle incoming message
        if (intent != null) {
            String action = intent.getAction();
            Log.d(TAG, "Received intent with action: " + action);

            if (ACTION_DRIVER_MESSAGE.equals(action)) {
                Log.d(TAG, "Processing driver message intent");
                if (intent.getExtras() != null) {
                    Log.d(TAG, "Received message with extras: " + intent.getExtras().toString());
                    handleFCMData(intent.getExtras());
                } else {
                    Log.d(TAG, "No extras in driver message intent");
                }
            } else {
                Log.d(TAG, "Not a driver message intent, ignoring");
            }
        } else {
            Log.d(TAG, "Received null intent");
        }

        return START_STICKY;
    }

    private void handleFCMData(Bundle extras) {
        Log.d(TAG, "Handling FCM data with " + extras.size() + " items");

        // Log all extras for debugging
        for (String key : extras.keySet()) {
            Log.d(TAG, "Extra - Key: " + key + ", Value: " + extras.get(key));
        }

        // Check if this is a driver message
        String intent = extras.getString("intent");
        Log.d(TAG, "Message intent: " + intent);

        if ("driver".equals(intent)) {
            Log.d(TAG, "Processing driver message");

            // Check overlay permission
            if (!Settings.canDrawOverlays(this)) {
                Log.e(TAG, "Overlay permission not granted!");
                showPermissionNotification();
                return;
            }
            Log.d(TAG, "Overlay permission granted");

            try {
                // Start floating window service
                Intent serviceIntent = new Intent(this, GoodsNewBookingFloatingWindowService.class);
                serviceIntent.putExtras(extras);
                serviceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Log.d(TAG, "Starting FloatingWindowService with extras");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                Log.d(TAG, "FloatingWindowService started successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error starting FloatingWindowService: ", e);
            }
        } else {
            Log.d(TAG, "Not a driver message, ignoring");
        }
    }

    private void showPermissionNotification() {
        Log.d(TAG, "Showing permission notification");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Overlay Permission Required")
                .setContentText("Please grant overlay permission to show floating window")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.notify(NOTIFICATION_ID + 1, builder.build());
        Log.d(TAG, "Permission notification shown");
    }

    private Notification createNotification() {
        Log.d(TAG, "Creating foreground notification");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("KAPS Partner")
                .setContentText("Running in background")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = CHANNEL_ID;
            CharSequence name = "Background Service";
            String description = "Channel for background service";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
            channel.setShowBadge(false);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created");
            }
        }
    }
    private void createFloatingWindow() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Inflate the floating window layout
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_window, null);

        // Set window parameters
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        // Initial position
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        // Add touch listener for dragging
        setupTouchListener();

        // Add the view to window
        windowManager.addView(floatingView, params);
    }

    private void setupTouchListener() {
        final GestureDetector gestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        // Handle double tap - maybe open your app
                        Intent intent = new Intent(FloatingWindowService.this, SplashScreenActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        return true;
                    }
                });

        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    return true;
                }

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
        }
        instance = null;
        Log.d(TAG, "BackgroundService destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "BackgroundService task removed");
        // Unsubscribe from all topic
        FirebaseMessaging.getInstance().unsubscribeFromTopic("all")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Unsubscribed from all topic");
                    } else {
                        Log.e(TAG, "Failed to unsubscribe from all topic", task.getException());
                    }
                });
    }
}