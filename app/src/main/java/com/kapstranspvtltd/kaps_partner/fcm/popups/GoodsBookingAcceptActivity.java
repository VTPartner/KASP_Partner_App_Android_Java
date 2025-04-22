package com.kapstranspvtltd.kaps_partner.fcm.popups;

import android.Manifest;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.kapstranspvtltd.kaps_partner.fcm.AccessToken;
import com.kapstranspvtltd.kaps_partner.goods_driver_activities.NewLiveRideActivity;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;
import com.kapstranspvtltd.kaps_partner.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GoodsBookingAcceptActivity extends AppCompatActivity {

    private static final String TAG = "GoodsBookingAccept";
    private PreferenceManager preferenceManager;
    private VolleySingleton volleySingleton;

    private MediaPlayer mediaPlayer;

    private CountDownTimer countDownTimer;

    private String bookingId;
    private JSONObject bookingJsonDetails;

    private void turnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true);
            setShowWhenLocked(true);
            KeyguardManager keyguardManager =
                    (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            keyguardManager.requestDismissKeyguard(this, null);
        } else {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goods_booking_accept);
        turnScreenOn();

        preferenceManager = new PreferenceManager(this);
        volleySingleton = VolleySingleton.getInstance(this);

        // Initialize timer
        TextView timerText = findViewById(R.id.timerText);


        // Initialize buttons
        Button acceptButton = findViewById(R.id.acceptButton);
        Button rejectButton = findViewById(R.id.rejectButton);

        acceptButton.setOnClickListener(v -> handleAcceptBooking());
        rejectButton.setOnClickListener(v -> handleRejectBooking());

        bookingId = getIntent().getStringExtra("booking_id");
        if (bookingId != null) {
            startCountdownTimer(timerText);
            initializeMediaPlayer();
            fetchBookingDetails(bookingId);
        } else {
            Toast.makeText(this, "Invalid booking ID", Toast.LENGTH_SHORT).show();
            finish();
        }
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

    private void fetchBookingDetails(String bookingId) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading booking details...");
        progressDialog.show();
        playNotificationSound();
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
                            progressDialog.dismiss();
                            try {
                                JSONArray results = response.optJSONArray("results");
                                if (results != null && results.length() > 0) {
                                    JSONObject bookingDetails = results.getJSONObject(0);
                                    runOnUiThread(() -> setupBookingDetails(bookingDetails, bookingId));
                                } else {
                                    Toast.makeText(this, "No booking details found", Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing booking details: " + e.getMessage());
                                Toast.makeText(this, "Error loading booking details", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        },
                        error -> {
                            progressDialog.dismiss();
                            Log.e(TAG, "Error fetching booking details: " + error.getMessage());
                            Toast.makeText(this, "Failed to load booking details", Toast.LENGTH_SHORT).show();
                            finish();
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

                volleySingleton.addToRequestQueue(request);

            } catch (Exception e) {
                progressDialog.dismiss();
                Log.e(TAG, "Error in fetchBookingDetails: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }

    private void setupBookingDetails(JSONObject bookingDetails, String bookingId) {

        try {
            Log.d(TAG, "Booking details: " + bookingDetails.toString());
            bookingJsonDetails = bookingDetails;
            // Get views
            TextView pickupAddressView = findViewById(R.id.pickupAddress);
            TextView dropAddressView = findViewById(R.id.dropAddress);
            TextView customerNameView = findViewById(R.id.customerName);
            TextView rideFareView = findViewById(R.id.rideFare);
            TextView distanceView = findViewById(R.id.distance);
            TextView pickupDistanceView = findViewById(R.id.pickupLocationDistance);
            ImageView profileImageView = findViewById(R.id.profileImage);

            // Set values with null checks
            String pickupAddress = bookingDetails.optString("pickup_address", "N/A");
            String dropAddress = bookingDetails.optString("drop_address", "N/A");
            String customerName = bookingDetails.optString("customer_name", "N/A");
            double totalPrice = bookingDetails.optDouble("total_price", 0);
            double distance = bookingDetails.optDouble("distance", 0);

            pickupAddressView.setText(String.format("%s", pickupAddress));
            dropAddressView.setText(String.format("%s", dropAddress));
            customerNameView.setText(customerName);
            rideFareView.setText(String.format("₹%.0f", totalPrice));
            distanceView.setText(String.format("%.1f Km", distance));

            // Calculate pickup distance if coordinates are available
            double pickupLat = bookingDetails.optDouble("pickup_lat", 0);
            double pickupLng = bookingDetails.optDouble("pickup_lng", 0);

            if (pickupLat != 0 && pickupLng != 0) {
                calculatePickupDistance(pickupLat, pickupLng, pickupDistanceView);
            } else {
                pickupDistanceView.setText("Distance unavailable");
            }



        } catch (Exception e) {
            Log.e(TAG, "Error setting up booking details: " + e.getMessage());
            Toast.makeText(this, "Error displaying booking details", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCountdownTimer(TextView timerText) {
      countDownTimer =   new CountDownTimer(15000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerText.setText(String.format("%d sec left", millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                finish(); // Close activity when timer expires
            }
        }.start();
    }

    private void calculatePickupDistance(double pickupLat, double pickupLng, TextView distanceView) {
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

                                        // Update UI with both distance and duration
                                        String distanceText = String.format("%s (%s away)",
                                                distance, duration);
                                        distanceView.setText(distanceText);
                                    } else {
                                        distanceView.setText("Distance unavailable");
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

                    VolleySingleton.getInstance(this).addToRequestQueue(request);
                } else {
                    distanceView.setText("Location unavailable");
                }
            });
        } else {
            distanceView.setText("Location permission required");
        }
    }

    // Add these methods
    private void handleAcceptBooking() {
        if (bookingId == null) {
            Toast.makeText(this, "Invalid booking ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cancel timer and stop sound
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        stopNotificationSound();

        // Show loading dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Use ExecutorService for background operations
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                String driverId = preferenceManager.getStringValue("goods_driver_id");
                String accessToken = AccessToken.getAccessToken();

                if (accessToken == null || accessToken.isEmpty()) {
                    handler.post(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(GoodsBookingAcceptActivity.this,
                                "No Token Found!", Toast.LENGTH_SHORT).show();
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
                            progressDialog.dismiss();
                            // Handle successful response
                            Intent intent = new Intent(GoodsBookingAcceptActivity.this,
                                    NewLiveRideActivity.class);
                            intent.putExtra("booking_id", bookingId);
                            intent.putExtra("FromFCM", true);
                            startActivity(intent);
                            finishAffinity();
                        },
                        error -> {
                            progressDialog.dismiss();
                            handleVolleyError(error);
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
                    VolleySingleton.getInstance(GoodsBookingAcceptActivity.this)
                            .addToRequestQueue(request);
                });

            } catch (Exception e) {
                handler.post(() -> {
                    progressDialog.dismiss();
                    handleError(e);
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
        finish();
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
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
        getSharedPreferences("MyPrefs", MODE_PRIVATE)
                .edit()
                .putString("current_booking_id_assigned", "")
                .apply();

        Toast.makeText(
                this,
                "Already Assigned to Another Driver.\nPlease be quick at receiving ride requests to earn more.",
                Toast.LENGTH_LONG
        ).show();
        finish();
    }

    private void handleDefaultError(VolleyError error) {
        String message = "Something went wrong";
        if (error instanceof NoConnectionError) {
            message = "No internet connection";
        } else if (error instanceof TimeoutError) {
            message = "Request timed out";
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void handleError(Exception e) {
        Log.e(TAG, "Error: " + e.getMessage());
        Toast.makeText(this,
                "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }


    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
}