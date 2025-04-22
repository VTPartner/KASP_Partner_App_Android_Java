package com.kapstranspvtltd.kaps_partner.goods_driver_activities.fragments;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bumptech.glide.Glide;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.messaging.FirebaseMessaging;
import com.kapstranspvtltd.kaps_partner.common_activities.LoginActivity;
import com.kapstranspvtltd.kaps_partner.fcm.AccessToken;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.MultipartRequest;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.services.LocationUpdateService;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.databinding.FragmentHomeBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 */
public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private BroadcastReceiver serviceRestartReceiver;
    private static final int CAMERA_REQUEST_CODE = 101;
    private ImageCapture cameraController;
    private PreviewView previewView;
    private FragmentHomeBinding binding;
    private PreferenceManager preferenceManager;
    private LocationManager locationManager;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isOnline = false;
    private Marker currentLocationMarker;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private VolleySingleton volleySingleton;
    private String driverName = "", profilePic = "", mobileNo = "";
    private double todaysEarnings = 0;
    private int todaysRides = 0;
    private boolean isVerified = false;
    private String currentBalance = "0";
    private boolean isExpired = false;

    private Double latitude = 0.0;

    private Double longitude = 0.0;

    private static final int REQUEST_LOCATION_SETTINGS = 100;


    private boolean isUserAction = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(requireContext());
        volleySingleton = VolleySingleton.getInstance(requireContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        setupLocationCallback();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupViews();
        checkLocationPermission();
        fetchDriverStatus();
        fetchEarnings();
        fetchCurrentBalance();
        getFCMToken();
        return binding.getRoot();
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
                    preferenceManager.saveStringValue("fcm_token", token);
                    updateDriverAuthToken(token);
                });
    }

    private void updateDriverAuthToken(String deviceToken) {
        Log.d("FCMNewTokenFound", "updating goods driver authToken");

        String driverId = preferenceManager.getStringValue("goods_driver_id");
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
                            Log.d("Auth", "Token update response: " + message);
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
                    VolleySingleton.getInstance(getActivity())
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

    private void setupViews() {
        // Add click listener for the status card
        binding.onlineStatusCard.setOnClickListener(v -> {
            if (isOnline) {
                showGoOfflineDialog();
            } else {
                showGoOnlineDialog();
            }
        });

        // Add click listener for the go online button
        binding.goOnlineButton.setOnClickListener(v -> showGoOnlineDialog());

    }

    private void updateDriverOnlineStatus(boolean online) {
//        if (!online && !isOnline) return; // Already offline
//        if (online && isOnline) return;   // Already online

        isOnline = online;
        animateStatusChange(online);

        if (online) {
            binding.statusDot.setBackgroundResource(R.drawable.status_dot_online);
            binding.statusText.setText("Online");
            binding.goOnlineButton.setVisibility(View.GONE);
            updateDriverStatus(true);
        } else {
            binding.statusDot.setBackgroundResource(R.drawable.status_dot_offline);
            binding.statusText.setText("Offline");
            binding.goOnlineButton.setVisibility(isVerified ? View.VISIBLE : View.GONE);
            updateDriverStatus(false);
        }
    }

    private void showGoOnlineDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Go Online")
                .setMessage("Are you ready to start receiving ride requests?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    dialog.dismiss();
                    goOnline();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }

    private void showGoOfflineDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Go Offline")
                .setMessage("Are you sure you want to go offline? You won't receive any new ride requests.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    dialog.dismiss();
                    updateDriverStatus(false);
                    stopLocationUpdates();
                    updateUIForOfflineStatus();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }


    // Separate UI updates from status updates
    private void updateUIForOnlineStatus() {
        if (binding == null) return;

        binding.statusDot.setBackgroundResource(R.drawable.status_dot_online);
        binding.statusText.setText("Online");
        binding.onlineStatusCard.setVisibility(View.VISIBLE);
        binding.goOnlineButton.setVisibility(View.GONE);

        // Animate the status change
        animateStatusChange(true);
    }

    private void updateUIForOfflineStatus() {
        if (binding == null) return;

        binding.statusDot.setBackgroundResource(R.drawable.status_dot_offline);
        binding.statusText.setText("Offline");
        binding.onlineStatusCard.setVisibility(View.VISIBLE);
        binding.goOnlineButton.setVisibility(isVerified ? View.VISIBLE : View.GONE);

        // Animate the status change
        animateStatusChange(false);
    }

    private void animateStatusChange(boolean online) {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(300);
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            if (binding != null) {
                binding.onlineStatusCard.setAlpha(1f - value);
            }
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (binding != null) {
                    binding.onlineStatusCard.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .start();
                }
            }
        });
        animator.start();
    }


    private static final int PERMISSION_REQUEST_CODE = 10090;

    private void goOnline() {

        if (!checkLocationEnabled()) {
            Toast.makeText(requireContext(), "Please enable GPS", Toast.LENGTH_SHORT).show();
            updateDriverOnlineStatus(false);
            return;
        }
        // Showing camera dialog for selfie
        if (checkAndRequestPermissions()) {
            showSelfieDialog();
        } else {
            updateDriverOnlineStatus(false);
        }
    }


    private boolean checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Check Camera Permission
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }

        // Check Notification Permission for Android 13 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            requestPermissions(permissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
            return false;
        }

        return true;
    }


    private boolean checkLocationEnabled() {
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showEnableLocationDialog();
        } else {
            // Location is enabled, proceed with location updates
            checkLocationPermission();
        }
        return true;
    }

    private void showEnableLocationDialog() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(5000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true);

        Task<LocationSettingsResponse> task = LocationServices.getSettingsClient(requireActivity())
                .checkLocationSettings(builder.build());

        task.addOnSuccessListener(requireActivity(), locationSettingsResponse -> {
            // Location settings are satisfied, proceed with location updates
            checkLocationPermission();
        });

        task.addOnFailureListener(requireActivity(), e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    // Show dialog to enable GPS
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(requireActivity(), REQUEST_LOCATION_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    Log.e(TAG, "Error showing location settings dialog", sendEx);
                    showToast("Unable to show location settings");
                }
            } else {
                showToast("Location settings are not satisfied");
            }
        });
    }


    private void goOffline() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Go Offline")
                .setMessage("Are you sure you want to go offline? You won't receive any new ride requests.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    dialog.dismiss();
                    updateDriverStatus(false);
                    stopLocationUpdates();
                    updateDriverOnlineStatus(false);
//                    binding.onlineSwitch.setChecked(false);
                    Toast.makeText(requireContext(), "You are now offline", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    dialog.dismiss();
                    updateDriverOnlineStatus(true);
                })
                .setCancelable(false)
                .show();
    }

    private void addToActiveDriverTable(double latitude, double longitude) {
        String url = APIClient.baseUrl + "add_new_active_goods_driver";

        JSONObject params = new JSONObject();
        try {
            params.put("goods_driver_id", preferenceManager.getStringValue("goods_driver_id"));
            params.put("status", isOnline ? 1 : 0);
            params.put("current_lat", latitude);
            params.put("current_lng", longitude);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> {
                    Toast.makeText(requireContext(), "You are Online now", Toast.LENGTH_SHORT).show();
                    updateUIForOnlineStatus();
                    startLocationUpdates();
                },
                error -> {
                    Log.e(TAG, "Error adding to active drivers: " + error.toString());
                    Toast.makeText(requireContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
                    // Revert UI state
                    updateUIForOfflineStatus();
                }) {
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
    }

    private void deleteFromActiveDriverTable() {
        String url = APIClient.baseUrl + "delete_active_goods_driver";

        JSONObject params = new JSONObject();
        try {
            params.put("goods_driver_id", preferenceManager.getStringValue("goods_driver_id"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> {
                    Snackbar.make(binding.getRoot(), "You are offline now", Snackbar.LENGTH_SHORT).show();

                    // Update UI state
                    isOnline = false;
                    updateUIForOfflineStatus();

                    // Stop location updates after a delay
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        stopLocationUpdates();
                        if (locationCallback != null) {
                            fusedLocationClient.removeLocationUpdates(locationCallback);
                        }
                    }, 1000);
                },
                error -> {
                    Log.e(TAG, "Error removing from active drivers: " + error.toString());
                    Toast.makeText(requireContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
                    // Revert UI state if needed
                    updateUIForOnlineStatus();
                }) {
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
    }


    private void updateDriverStatus(boolean online) {
        String url = APIClient.baseUrl + "goods_driver_update_online_status";

        JSONObject params = new JSONObject();
        try {
            params.put("goods_driver_id", preferenceManager.getStringValue("goods_driver_id"));
            params.put("status", online ? 1 : 0);
            params.put("lat", latitude);
            params.put("lng", longitude);
            params.put("recent_online_pic", preferenceManager.getStringValue("recent_online_pic"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> {
                    isOnline = online;
                    if (online) {
                        addToActiveDriverTable(latitude, longitude);
//                        updateUIForOnlineStatus();
//                        startLocationUpdates();

                    } else {
                        deleteFromActiveDriverTable();
//                        updateUIForOfflineStatus();
                    }
                },
                error -> {
                    Toast.makeText(requireContext(), "Error updating status", Toast.LENGTH_SHORT).show();
                    // Revert UI changes if update failed
                    if (online) {
                        updateUIForOfflineStatus();
                    } else {
                        updateUIForOnlineStatus();
                    }
                });

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        volleySingleton.addToRequestQueue(request);
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                Location location = locationResult.getLastLocation();
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                //updateDriverLocation(location);
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                updateMapLocation(currentLatLng);
            }
        };
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        updateMapLocation(currentLatLng);
                        addOrUpdateMarker(currentLatLng);
                    }
                })
                .addOnFailureListener(e -> {
                    showToast("Unable to get location");
                    Log.e(TAG, "Error getting location", e);
                });
    }

    private void updateMapLocation(LatLng latLng) {
        if (mMap == null) return;

        CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(latLng, 100);
        mMap.animateCamera(yourLocation);


    }

    private void addOrUpdateMarker(LatLng position) {
        if (mMap == null) return;

        // Remove existing marker if any
        if (currentLocationMarker != null) {
            currentLocationMarker.remove();
        }

        // Create custom marker icon
        createCustomMarker(bitmap -> {
            if (bitmap != null) {
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(position)
                        .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                        .anchor(0.5f, 0.5f); // Center the marker

                currentLocationMarker = mMap.addMarker(markerOptions);
            }
        });
    }

    private void createCustomMarker(OnBitmapReadyCallback callback) {
        try {
            // Load vector drawable
            Drawable vectorDrawable = ResourcesCompat.getDrawable(getResources(),
                    R.drawable.ic_truck, null);

            if (vectorDrawable == null) {
                callback.onBitmapReady(null);
                return;
            }

            Bitmap bitmap = Bitmap.createBitmap(
                    vectorDrawable.getIntrinsicWidth(),
                    vectorDrawable.getIntrinsicHeight(),
                    Bitmap.Config.ARGB_8888
            );

            Canvas canvas = new Canvas(bitmap);
            vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            vectorDrawable.draw(canvas);

            callback.onBitmapReady(bitmap);
        } catch (Exception e) {
            Log.e(TAG, "Error creating custom marker", e);
            callback.onBitmapReady(null);
        }
    }

    interface OnBitmapReadyCallback {
        void onBitmapReady(Bitmap bitmap);
    }

    private void updateDriverLocation(Location location) {
        // Update driver location in backend
        String url = APIClient.baseUrl + "update_goods_drivers_current_location";

        JSONObject params = new JSONObject();
        try {
            params.put("goods_driver_id", preferenceManager.getStringValue("goods_driver_id"));
            params.put("lat", location.getLatitude());
            params.put("lng", location.getLongitude());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> {
                    // Handle success
                },
                error -> {
                    // Handle error
                });

        volleySingleton.addToRequestQueue(request);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        if (checkLocationPermission()) {
//            mMap.setMyLocationEnabled(true);
            getCurrentLocation();
        }
    }

    private boolean checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
//            startLocationUpdates();
            return true;
        }
        return false;
    }

    // Handle the result from location settings dialog
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_LOCATION_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                // User enabled location
                checkLocationPermission();
            } else {
                // User didn't enable location
                showLocationRequiredDialog();
            }
        }
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Check permissions again after returning from settings
//            if (checkAndRequestPermissions()) {
//                showSelfieDialog();
//            } else {
//                binding.onlineSwitch.setChecked(false);
//            }
        }
    }

    private void showLocationRequiredDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Location Required")
                .setMessage("This app requires location services to function properly. Please enable GPS to continue.")
                .setCancelable(false)
                .setPositiveButton("Enable GPS", (dialog, which) -> {
                    dialog.dismiss();
                    checkLocationEnabled();
                })
                .setNegativeButton("Exit", (dialog, which) -> {
                    dialog.dismiss();
                    requireActivity().finish();
                })
                .show();
    }

    private void fetchDriverStatus() {
        String url = APIClient.baseUrl + "goods_driver_online_status";
        String driverId = preferenceManager.getStringValue("goods_driver_id");

        if (driverId.isEmpty()) {
            // Navigate to login
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
            return;
        }

        JSONObject params = new JSONObject();
        try {
            params.put("goods_driver_id", driverId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        if (results.length() > 0) {
                            JSONObject data = results.getJSONObject(0);

                            // Update UI with driver status
                            profilePic = data.getString("profile_pic");
                            mobileNo = data.getString("mobile_no");
                            int is_online_status = data.getInt("is_online");
                            if (is_online_status == 1) isOnline = true;
                            else isOnline = false;
                            String status = data.getString("status");
                            String recentOnlinePic = data.getString("recent_online_pic");

                            preferenceManager.saveStringValue("recent_online_pic", recentOnlinePic);

                            // Update verification status
                            switch (status) {
                                case "0":
                                    isVerified = false;
                                    updateVerificationStatus("You are not yet verified");
                                    break;
                                case "2":
                                    isVerified = false;
                                    updateVerificationStatus("You are blocked");
                                    break;
                                case "3":
                                    isVerified = false;
                                    updateVerificationStatus("You are rejected");
                                    break;
                                default:
                                    isVerified = true;
                                    startLocationUpdates();
                                    break;
                            }

                            driverName = data.getString("driver_first_name");
                            updateUI();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    String message = "Something went wrong";
                    if (error instanceof NoConnectionError) {
                        message = "No internet connection";
                    } else if (error instanceof TimeoutError) {
                        message = "Request timed out";
                    }
                    showToast(message);
                });

        volleySingleton.addToRequestQueue(request);
    }

    private void updateVerificationStatus(String youAreNotYetVerified) {
        binding.expiryAlert.setVisibility(View.VISIBLE);
        binding.expiryTxt.setText(youAreNotYetVerified);
        binding.onlineStatusCard.setVisibility(View.GONE);
    }

    private void fetchEarnings() {
        String url = APIClient.baseUrl + "goods_driver_todays_earnings";
        String driverId = preferenceManager.getStringValue("goods_driver_id");

        if (driverId.isEmpty()) return;

        JSONObject params = new JSONObject();
        try {
            params.put("driver_id", driverId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        if (results.length() > 0) {
                            JSONObject data = results.getJSONObject(0);
                            todaysEarnings = data.getDouble("todays_earnings");
                            todaysRides = data.getInt("todays_rides");
                            updateEarningsUI();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    if (error.toString().contains("No Data Found")) {
                        showToast("No earnings data found");
                    }
                });

        volleySingleton.addToRequestQueue(request);
    }

    private void fetchCurrentBalance() {
        String url = APIClient.baseUrl + "get_goods_driver_current_recharge_details";
        String driverId = preferenceManager.getStringValue("goods_driver_id");

        JSONObject params = new JSONObject();
        try {
            params.put("driver_id", driverId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        if (results.length() > 0) {
                            JSONObject data = results.getJSONObject(0);

                            // Handle balance and negative points
                            currentBalance = data.getString("remaining_points");
                            double negativePoints = data.getDouble("negative_points");

                            if (negativePoints > 0) {
                                currentBalance = String.valueOf(negativePoints);
                                double limitExceededBalance = Double.parseDouble(currentBalance);
                                currentBalance = "-" + currentBalance;
//TODO:check recharge expiry here later
//                                if (!isShowingLiveRideDetails()) {
//                                    goOffline();
//                                    showRechargeAlert();
//                                }
                            }

                            // Check expiry date
                            String validTillDate = data.getString("valid_till_date");
                            checkExpiryDate(validTillDate);

                            updateBalanceUI();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    if (error.toString().contains("No Data Found")) {
                        showToast("Not Yet Subscribed to any Top-Up Recharge plan");
                    }
                });

        volleySingleton.addToRequestQueue(request);
    }

    // Helper methods for UI updates
    private void updateUI() {
        if (binding == null) return;

        // Update driver image
        Glide.with(this)
                .load(profilePic)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .circleCrop()
                .into(binding.driverImage);

        // Only update UI elements without changing the actual online status
        if (isOnline) {
            binding.statusDot.setBackgroundResource(R.drawable.status_dot_online);
            binding.statusText.setText("Online");
            binding.goOnlineButton.setVisibility(View.GONE);
        } else {
            binding.statusDot.setBackgroundResource(R.drawable.status_dot_offline);
            binding.statusText.setText("Offline");
            binding.goOnlineButton.setVisibility(isVerified ? View.VISIBLE : View.GONE);
        }

        binding.onlineStatusCard.setVisibility(isVerified ? View.VISIBLE : View.GONE);
    }

    private void updateEarningsUI() {
        if (binding == null) return;

        binding.ridesAndEarnings.setText(String.format("%d Rides", todaysRides) + " | " + String.format("₹%.2f", todaysEarnings));

    }

    private void updateBalanceUI() {
        if (binding == null) return;

        binding.currentBalance.setText(String.format("Current Balance: ₹%s", currentBalance));
        binding.expiryAlert.setVisibility(isExpired ? View.VISIBLE : View.GONE);
    }

    private void checkExpiryDate(String validTillDateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date validTillDate = sdf.parse(validTillDateStr);
            Date currentDate = new Date();

            if (validTillDate != null && !validTillDate.after(currentDate)) {
                isExpired = true;
                showToast("Your previous plan has expired. Please recharge promptly to continue receiving ride requests.");
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void showRechargeAlert() {
        Snackbar.make(binding.getRoot(),
                "To continue receiving ride requests, please ensure your account is recharged.",
                Snackbar.LENGTH_LONG).show();
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    // Location updates
    private void startLocationUpdates() {
        LocationUpdateService.isStoppedManually = false; // Reset the flag
        Intent serviceIntent = new Intent(requireContext(), LocationUpdateService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(serviceIntent);
        } else {
            requireContext().startService(serviceIntent);
        }
        // Register broadcast receiver for service restart
        if (serviceRestartReceiver == null) {
            serviceRestartReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (!LocationUpdateService.isStoppedManually) {
                        // Restart service
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            requireContext().startForegroundService(new Intent(getActivity(),
                                    LocationUpdateService.class));
                        } else {
                            requireContext().startService(new Intent(getActivity(),
                                    LocationUpdateService.class));
                        }
                    }
                }
            };

            getActivity().registerReceiver(serviceRestartReceiver,
                    new IntentFilter("com.vtpartnertranspvtltd.vt_partner.RESTART_SERVICE"));
        }
    }

    private void stopLocationUpdates() {
        LocationUpdateService.isStoppedManually = true; // Set the flag
        requireContext().stopService(new Intent(requireContext(), LocationUpdateService.class));

        // Unregister the receiver
        if (serviceRestartReceiver != null) {
            try {
                getActivity().unregisterReceiver(serviceRestartReceiver);
                serviceRestartReceiver = null;
            } catch (IllegalArgumentException e) {
                // Receiver not registered
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                startLocationUpdates();
            } else {
                showToast("Location permission is required");
            }
        }
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            Map<String, Boolean> permissionResults = new HashMap<>();

            for (int i = 0; i < permissions.length; i++) {
                permissionResults.put(permissions[i], grantResults[i] == PackageManager.PERMISSION_GRANTED);
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                }
            }

            if (allPermissionsGranted) {
                // All permissions granted, proceed with selfie
                showSelfieDialog();
            } else {
                // Check which permission was denied
                if (permissionResults.containsKey(Manifest.permission.CAMERA)
                        && !permissionResults.get(Manifest.permission.CAMERA)) {
                    showPermissionExplanationDialog("Camera",
                            "Camera permission is required to take selfie for going online. " +
                                    "Please grant camera permission from settings.");
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                        && permissionResults.containsKey(Manifest.permission.POST_NOTIFICATIONS)
                        && !permissionResults.get(Manifest.permission.POST_NOTIFICATIONS)) {
                    showPermissionExplanationDialog("Notifications",
                            "Notification permission is required to receive booking alerts. " +
                                    "Please grant notification permission from settings.");
                }
                updateDriverOnlineStatus(false);
            }
        }
    }

    private void showPermissionExplanationDialog(String permissionType, String message) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(permissionType + " Permission Required")
                .setMessage(message)
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    dialog.dismiss();
                    openAppSettings();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                    updateDriverOnlineStatus(false);
                })
                .setCancelable(false)
                .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopLocationUpdates();
        binding = null;
    }

    private void showSelfieDialog() {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_camera_preview);

        previewView = dialog.findViewById(R.id.previewView);
        ImageButton captureButton = dialog.findViewById(R.id.captureButton);
//        ImageButton closeButton = dialog.findViewById(R.id.closeButton);

//        closeButton.setOnClickListener(v -> {
//            dialog.dismiss();
//            updateDriverOnlineStatus(false);
//        });

        captureButton.setOnClickListener(v -> {
            takePicture(dialog);
        });

        setupCamera(dialog);
        dialog.show();
    }

    private void setupCamera(Dialog dialog) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider, dialog);
            } catch (Exception e) {
                Log.e(TAG, "Error setting up camera", e);
                dialog.dismiss();
                updateDriverOnlineStatus(false);
                Toast.makeText(requireContext(), "Failed to setup camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider, Dialog dialog) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageCapture imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            cameraController = imageCapture;
        } catch (Exception e) {
            Log.e(TAG, "Error binding camera", e);
            dialog.dismiss();
            updateDriverOnlineStatus(false);
            Toast.makeText(requireContext(), "Failed to start camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void takePicture(Dialog dialog) {
        if (cameraController == null) return;

        File photoFile = createImageFile();
        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        ((ImageCapture) cameraController).takePicture(outputFileOptions,
                ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        dialog.dismiss();
                        uploadImage(photoFile);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        dialog.dismiss();
                        updateDriverOnlineStatus(false);
                        Toast.makeText(requireContext(),
                                "Failed to capture image", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "SELFIE_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            Log.e(TAG, "Error creating image file", e);
            return null;
        }
    }

    private void showConfirmationDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Go Online")
                .setMessage("Are you sure you want to go online?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    dialog.dismiss();

                    updateDriverStatus(true);
                })
                .setNegativeButton("No", (dialog, which) -> {
                    dialog.dismiss();
                    updateDriverOnlineStatus(false);
                })
                .setCancelable(false)
                .show();
    }

    private void uploadImageNew(File imageFile) {


        String url = APIClient.baseImageUrl + "/upload";

        // Show loading dialog
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Uploading selfie...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Create multipart request
        File finalImageFile = imageFile;
        MultipartRequest multipartRequest = new MultipartRequest(
                Request.Method.POST,
                url,
                response -> {
                    progressDialog.dismiss();
                    try {
                        JSONObject jsonObject = new JSONObject(new String(response));
                        String imageUrl = jsonObject.getString("image_url");

                        // Save to preferences
                        preferenceManager.saveStringValue("recent_online_pic", imageUrl);
                        System.out.println("imageUrl::" + imageUrl);

                        Toast.makeText(requireContext(),
                                "Selfie image uploaded successfully", Toast.LENGTH_SHORT).show();

                        // Update driver status
                        updateDriverStatus(true);

                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing response", e);
                        onUploadError("Failed to process response");
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    onUploadError("Failed to upload image");
                }
        ) {
            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                try {
                    byte[] imageBytes = readFileToBytes(finalImageFile); // ✅ Use compressed image
                    params.put("image", new DataPart("selfie.jpg", imageBytes, "image/jpeg"));
                } catch (IOException e) {
                    Log.e(TAG, "Error reading image file", e);
                }
                return params;
            }

            // Helper method to read file to bytes
            private byte[] readFileToBytes(File file) throws IOException {
                int size = (int) file.length();
                byte[] bytes = new byte[size];

                try (BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file))) {
                    buf.read(bytes, 0, bytes.length);
                }

                return bytes;
            }

            @Override
            protected Map<String, String> getParams() {
                return new HashMap<>(); // Add any additional fields if needed
            }
        };

        // Add retry policy
        multipartRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000, // 30 seconds timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        // Add to request queue
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(multipartRequest);
    }

    private void uploadImageOld(File imageFile) {

        String url = APIClient.baseImageUrl + "/upload";

        // Show loading dialog
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Uploading selfie...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Create multipart request
        MultipartRequest multipartRequest = new MultipartRequest(
                Request.Method.POST,
                url,
                response -> {
                    progressDialog.dismiss();
                    try {
                        JSONObject jsonObject = new JSONObject(new String(response));
                        String imageUrl = jsonObject.getString("image_url");

                        // Save to preferences
                        preferenceManager.saveStringValue("recent_online_pic", imageUrl);
                        System.out.println("imageUrl::" + imageUrl);

                        Toast.makeText(requireContext(),
                                "Selfie image uploaded successfully", Toast.LENGTH_SHORT).show();

                        // Update driver status
//                        showConfirmationDialog();
                        updateDriverStatus(true);


                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing response", e);
                        onUploadError("Failed to process response");
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    onUploadError("Failed to upload image");
                }
        ) {
            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                try {
                    byte[] imageBytes = readFileToBytes(imageFile);
                    params.put("image", new DataPart("selfie.jpg", imageBytes, "image/jpeg"));
                } catch (IOException e) {
                    Log.e(TAG, "Error reading image file", e);
                }
                return params;
            }

            // Helper method to read file to bytes
            private byte[] readFileToBytes(File file) throws IOException {
                int size = (int) file.length();
                byte[] bytes = new byte[size];

                try (BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file))) {
                    buf.read(bytes, 0, bytes.length);
                }

                return bytes;
            }

            @Override
            protected Map<String, String> getParams() {
                return new HashMap<>(); // Add any additional fields if needed
            }
        };

        // Add retry policy
        multipartRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000, // 30 seconds timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        // Add to request queue
        VolleySingleton.getInstance(requireContext()).addToRequestQueue(multipartRequest);
    }

    private File compressImage(File imageFile) {
        try {
            // Decode the image file to a Bitmap
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, 1024, 1024); // Max width/height

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

            // Create a new compressed file
            File compressedFile = new File(requireContext().getCacheDir(), "compressed_" + imageFile.getName());
            FileOutputStream fos = new FileOutputStream(compressedFile);

            // Start with quality 90
            int quality = 90;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos);

            // Keep reducing quality until file size is less than 500KB
            while (compressedFile.length() > 500 * 1024 && quality > 10) {
                quality -= 10;
                fos = new FileOutputStream(compressedFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos);
                fos.flush();
                fos.close();
            }

            // Recycle the bitmap to free memory
            bitmap.recycle();

            return compressedFile;
        } catch (IOException e) {
            Log.e(TAG, "Error compressing image", e);
            return imageFile; // Return original file if compression fails
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private void uploadImage(File imageFile) {
        // First compress the image
        File compressedFile = compressImage(imageFile);

        String url = APIClient.baseImageUrl + "/upload";

        // Show loading dialog
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Uploading selfie...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Create multipart request
        MultipartRequest multipartRequest = new MultipartRequest(
                Request.Method.POST,
                url,
                response -> {
                    progressDialog.dismiss();
                    try {
                        JSONObject jsonObject = new JSONObject(new String(response));
                        String imageUrl = jsonObject.getString("image_url");

                        // Save to preferences
                        preferenceManager.saveStringValue("recent_online_pic", imageUrl);
                        System.out.println("imageUrl::" + imageUrl);

                        Toast.makeText(requireContext(),
                                "Selfie image uploaded successfully", Toast.LENGTH_SHORT).show();

                        updateDriverStatus(true);

                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing response", e);
                        onUploadError("Failed to process response");
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    onUploadError("Failed to upload image");
                }
        ) {
            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                try {
                    byte[] imageBytes = readFileToBytes(compressedFile); // Use compressed file
                    params.put("image", new DataPart("selfie.jpg", imageBytes, "image/jpeg"));
                } catch (IOException e) {
                    Log.e(TAG, "Error reading image file", e);
                }
                return params;
            }

            private byte[] readFileToBytes(File file) throws IOException {
                int size = (int) file.length();
                byte[] bytes = new byte[size];

                try (BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file))) {
                    buf.read(bytes, 0, bytes.length);
                }

                return bytes;
            }

            @Override
            protected Map<String, String> getParams() {
                return new HashMap<>();
            }
        };

        multipartRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        VolleySingleton.getInstance(requireContext()).addToRequestQueue(multipartRequest);
    }

    private void onUploadError(String message) {
        updateDriverOnlineStatus(false);
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

}