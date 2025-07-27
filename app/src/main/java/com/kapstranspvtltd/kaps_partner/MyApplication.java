package com.kapstranspvtltd.kaps_partner;

import static android.Manifest.permission.READ_PHONE_STATE;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.kapstranspvtltd.kaps_partner.fcm.AccessToken;
import com.kapstranspvtltd.kaps_partner.models.AppContent;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.utils.AppContentManager;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;
import com.kapstranspvtltd.kaps_partner.R;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MyApplication extends Application {
    public static Context mContext;

    private PreferenceManager preferenceManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        createNotificationChannel();
        preferenceManager = new PreferenceManager(this);

        FirebaseApp.initializeApp(this);

        getFCMToken();
        // Fetch app content for all screens
        fetchAppContent();
    }

    private void fetchAppContent() {
        AppContentManager.getInstance(this).fetchAppContent(this, new AppContentManager.AppContentCallback() {
            @Override
            public void onSuccess(Map<String, List<AppContent>> content) {
                Log.d("MyApplication", "App content fetched successfully");
                // Load splash screen content immediately
//                loadSplashScreenContent();
            }

            @Override
            public void onError(String error) {
                Log.e("MyApplication", "Error fetching app content: " + error);
            }
        });
    }


    private void getFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    System.out.println("Driver device token::"+token);

                    updateDriverAuthToken(token);
                });
    }

    private void updateDriverAuthToken(String deviceToken) {
        Log.d("FCMNewTokenFound", "updating driver authToken");

        String driverId = preferenceManager.getStringValue("goods_driver_id");
        String cabDriverId = preferenceManager.getStringValue("cab_driver_id");
        String otherDriverId = preferenceManager.getStringValue("other_driver_id");
        String jcbCraneAgentId = preferenceManager.getStringValue("jcb_crane_agent_id");
        String handymanAgentId = preferenceManager.getStringValue("handyman_agent_id");

        String url = "";
        String key = "", value = "";
        String tokenKey = "";

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
        }

        // If no valid driver type or token, return
        if (url.isEmpty() || deviceToken == null || deviceToken.isEmpty()) {
            return;
        }

        // Using ExecutorService instead of Coroutines
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        final String finalUrl = url;
        final String finalKey = key;
        final String finalValue = value;
        final String finalTokenKey = tokenKey;

        executor.execute(() -> {
            try {
                String serverToken = AccessToken.getAccessToken();
                System.out.println("deviceToken::" + deviceToken);
                System.out.println("------------");
                System.out.println("serverToken::" + serverToken);

                JSONObject jsonBody = new JSONObject();
                jsonBody.put(finalKey, finalValue);
                jsonBody.put("authToken", deviceToken);

                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.POST,
                        finalUrl,
                        jsonBody,
                        response -> {
                            String message = response.optString("message");
                            Log.d("Auth", "Token update response: " + message);
                            // Save token on successful update
                            preferenceManager.saveStringValue(finalTokenKey, deviceToken);
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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Default channel
            NotificationChannel defaultChannel = new NotificationChannel(
                    getString(R.string.default_notification_channel_id),
                    "Default Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            defaultChannel.setDescription("Default notification channel");

            // Booking channel
            NotificationChannel bookingChannel = new NotificationChannel(
                    "booking_channel",
                    "Booking Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            bookingChannel.setDescription("Notifications for new bookings");
            bookingChannel.setSound(null, null);
            bookingChannel.enableVibration(true);
            bookingChannel.setVibrationPattern(new long[]{1000, 1000});
            bookingChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(defaultChannel);
            notificationManager.createNotificationChannel(bookingChannel);
        }
    }

    public static boolean isActivityVisible() {
        return activityVisible;
    }

    public static void activityResumed() {
        activityVisible = true;
    }

    public static void activityPaused() {
        activityVisible = false;
    }

    private static boolean activityVisible;
}