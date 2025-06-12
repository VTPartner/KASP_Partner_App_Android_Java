package com.kapstranspvtltd.kaps_partner.goods_driver_activities;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.POST_NOTIFICATIONS;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.messaging.FirebaseMessaging;
import com.kapstranspvtltd.kaps_partner.BackgroundService;
import com.kapstranspvtltd.kaps_partner.common_activities.DriverTypeActivity;
import com.kapstranspvtltd.kaps_partner.common_activities.LoginActivity;
import com.kapstranspvtltd.kaps_partner.driver_app_activities.settings_pages.DriverAgentWalletActivity;
import com.kapstranspvtltd.kaps_partner.fcm.AccessToken;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.MultipartRequest;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.services.FloatingWindowService;
import com.kapstranspvtltd.kaps_partner.services.LocationUpdateService;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;
import com.kapstranspvtltd.kaps_partner.utils.SwipeButton;
import com.kapstranspvtltd.kaps_partner.utils.Utility;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityHomeBinding;

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
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private ActivityHomeBinding binding;
    Snackbar snackbar;
    private ImageCapture cameraController;
    private PreviewView previewView;
    private PreferenceManager preferenceManager;

    private boolean isLiveRide;
    private LocationManager locationManager;
    private static final String TAG = "HomeActivity";
    private ActionBarDrawerToggle drawerToggle;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;

    private LocationCallback locationCallback;

    private LocationRequest locationRequest;
    private Marker currentLocationMarker;
    private boolean isFirstLocation = true;
//    private SwitchMaterial dutySwitch;

    private Double latitude = 0.0;

    private Double longitude = 0.0;

    String profilePic = "", mobileNo = "", driverName = "";

    int vehicleId =3;
    boolean isVerified = false, isOnline = false;

    Double todaysEarnings = 0.0, totalEarnings = 0.0;
    int todaysRides = 0, totalRides = 0;

    private VolleySingleton volleySingleton;

    private boolean isUserAction = true;

    boolean planExpired = false;
    boolean noPlanYet = false;

    private boolean isRecreating = false;

    private SwipeButton dutySwipeButton;


    public void restartApp(Context context) {
        Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage(context.getPackageName());

        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
            Runtime.getRuntime().exit(0); // Forcefully kill current process
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        View headerView = binding.navView.getHeaderView(0);
        TextView editProfileBtn = headerView.findViewById(R.id.edit_profile);
        editProfileBtn.setOnClickListener(v -> goToEditProfilePage());

        preferenceManager = new PreferenceManager(this);
        volleySingleton = VolleySingleton.getInstance(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Initialize location request
        createLocationRequest();

        // Initialize location callback
        createLocationCallback();

        //
        checkBatteryOptimization();
        checkAndRequestPermissions();


        //setupLocationCallback();
        initializeViews();
        setupNavigationDrawer();
        setupMap();
        setupLocationServices();

        // Add this check
        if (savedInstanceState != null) {
            isRecreating = savedInstanceState.getBoolean("is_recreating", false);
        }

        getFCMToken();



    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("is_recreating", isRecreating);
    }

    private void showLocationTypeDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_location_type, null);
        dialog.setContentView(sheetView);

        LinearLayout bothBooking = sheetView.findViewById(R.id.both_booking);
        LinearLayout localBooking = sheetView.findViewById(R.id.local_booking);
        LinearLayout outstationBooking = sheetView.findViewById(R.id.outstation_booking);

        ImageView bothCheck = sheetView.findViewById(R.id.both_check);
        ImageView localCheck = sheetView.findViewById(R.id.local_check);
        ImageView outstationCheck = sheetView.findViewById(R.id.outstation_check);
        Button saveButton = sheetView.findViewById(R.id.save_location_type);

        // Get current location preference from preferences
        int currentPreference = preferenceManager.getIntValue("location_preference", 0);

        // Initialize checkmarks based on current selection
        updateLocationTypeCheckmarks(currentPreference, bothCheck, localCheck, outstationCheck);

        final int[] selectedPreference = {currentPreference};

        bothBooking.setOnClickListener(v -> {
            selectedPreference[0] = 0;
            updateLocationTypeCheckmarks(0, bothCheck, localCheck, outstationCheck);
        });

        localBooking.setOnClickListener(v -> {
            selectedPreference[0] = 1;
            updateLocationTypeCheckmarks(1, bothCheck, localCheck, outstationCheck);
        });

        outstationBooking.setOnClickListener(v -> {
            selectedPreference[0] = 2;
            updateLocationTypeCheckmarks(2, bothCheck, localCheck, outstationCheck);
        });

        saveButton.setOnClickListener(v -> {
            updateDriverLocationPreference(selectedPreference[0]);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateLocationTypeCheckmarks(int preference, ImageView bothCheck, ImageView localCheck, ImageView outstationCheck) {
        bothCheck.setVisibility(preference == 0 ? View.VISIBLE : View.GONE);
        localCheck.setVisibility(preference == 1 ? View.VISIBLE : View.GONE);
        outstationCheck.setVisibility(preference == 2 ? View.VISIBLE : View.GONE);
    }

    private void updateDriverLocationPreference(int preference) {
        String driverId = preferenceManager.getStringValue("goods_driver_id");
        String token = preferenceManager.getStringValue("goods_driver_token");

        String url = APIClient.baseUrl + "update_goods_driver_location_preference";

        JSONObject params = new JSONObject();
        try {
            params.put("goods_driver_id", preferenceManager.getStringValue("goods_driver_id"));
            params.put("location_preference", preference);
            params.put("driver_unique_id", driverId);
            params.put("auth", token);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                params,
                response -> {
                    try {
                        String message = response.getString("message");
                        Toast.makeText(this, "Location preference updated successfully", Toast.LENGTH_SHORT).show();
                        preferenceManager.saveIntValue("location_preference", preference);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error updating location preference", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(this, "Error updating location preference", Toast.LENGTH_SHORT).show();
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
    }
    private void showBodyTypeSelection() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_body_type_selection, null);
        dialog.setContentView(sheetView);

        LinearLayout anyBodyType = sheetView.findViewById(R.id.any_body_type);
        LinearLayout openBodyType = sheetView.findViewById(R.id.open_body_type);
        LinearLayout closeBodyType = sheetView.findViewById(R.id.close_body_type);

        ImageView anyCheck = sheetView.findViewById(R.id.any_check);
        ImageView openCheck = sheetView.findViewById(R.id.open_check);
        ImageView closeCheck = sheetView.findViewById(R.id.close_check);

        Button saveButton = sheetView.findViewById(R.id.save_body_type);

        // Get current body type from preferences
        String currentBodyType = preferenceManager.getStringValue("body_type", "Any");

        // Initialize checkmarks based on current selection
        updateBodyTypeCheckmarks(currentBodyType, anyCheck, openCheck, closeCheck);

        final String[] selectedBodyType = {currentBodyType};

        anyBodyType.setOnClickListener(v -> {
            selectedBodyType[0] = "Any";
            updateBodyTypeCheckmarks("Any", anyCheck, openCheck, closeCheck);
        });

        openBodyType.setOnClickListener(v -> {
            selectedBodyType[0] = "Open Body";
            updateBodyTypeCheckmarks("Open Body", anyCheck, openCheck, closeCheck);
        });

        closeBodyType.setOnClickListener(v -> {
            selectedBodyType[0] = "Close Body";
            updateBodyTypeCheckmarks("Close Body", anyCheck, openCheck, closeCheck);
        });

        saveButton.setOnClickListener(v -> {
            updateDriverBodyType(selectedBodyType[0]);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateBodyTypeCheckmarks(String bodyType, ImageView anyCheck, ImageView openCheck, ImageView closeCheck) {
        anyCheck.setVisibility(bodyType.equals("Any") ? View.VISIBLE : View.GONE);
        openCheck.setVisibility(bodyType.equals("Open Body") ? View.VISIBLE : View.GONE);
        closeCheck.setVisibility(bodyType.equals("Close Body") ? View.VISIBLE : View.GONE);
    }

    private void updateDriverBodyType(String bodyType) {
        String driverId = preferenceManager.getStringValue("goods_driver_id");
        String token = preferenceManager.getStringValue("goods_driver_token");

        String url = APIClient.baseUrl + "update_goods_driver_body_type";

        JSONObject params = new JSONObject();
        try {
            params.put("goods_driver_id", preferenceManager.getStringValue("goods_driver_id"));
            params.put("body_type", bodyType);
            params.put("driver_unique_id", driverId);
            params.put("auth", token);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                params,
                response -> {
                    try {
                        String message = response.getString("message");
                        Toast.makeText(this, "Body type updated successfully", Toast.LENGTH_SHORT).show();
                        preferenceManager.saveStringValue("body_type", bodyType);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error updating body type", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(this, "Error updating body type", Toast.LENGTH_SHORT).show();
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
    }

    private void startBackgroundService() {
        Log.d(TAG, "Starting background service");
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void stopBackgroundService() {
        Log.d(TAG, "Stopping background service");
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        stopService(serviceIntent);
    }

    private void goToEditProfilePage() {
        Intent intent = new Intent(this, GoodsDriverProfileDetailsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void initializeViews() {
        preferenceManager = new PreferenceManager(this);
        isLiveRide = preferenceManager.getBooleanValue("isLiveRide", false);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Initialize toolbar views
//        dutySwitch = binding.toolbar.findViewById(R.id.duty_switch);
        ImageView menuIcon = binding.toolbar.findViewById(R.id.menu_icon);

        ImageView notificationIcon = binding.toolbar.findViewById(R.id.notification_icon);

        FloatingActionButton liveOrderFab = binding.liveOrderFab;

// Load GIF using Glide
        Glide.with(this)
                .asGif()
                .load(R.drawable.live_ride)
                .into(liveOrderFab);

// Click listener
        liveOrderFab.setOnClickListener(v -> {
            startActivity(new Intent(this, NewLiveRideActivity.class));
        });


        // Setup click listeners
        menuIcon.setOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.START));
        dutySwipeButton = binding.dutySwipeButton;
        binding.dutySwipeButton.setOnSlideCompleteListener(() -> {
            if (!isUserAction || isRecreating) {
                return;
            }
            if (noPlanYet) {
                showError("No active recharge plan found. Please recharge now");
                dutySwipeButton.reset();
                return;
            }
            if (isOnline) {
                showGoOfflineDialog();
            } else {
                showGoOnlineDialog();
            }
        });

        // Setup duty switch with user action tracking
/*
        dutySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUserAction || isRecreating) {
                return; // Skip if this is a programmatic change or recreation
            }
            if (noPlanYet) {
                isUserAction = false;
                dutySwitch.setChecked(false);
                showError("No active recharge plan found.Please recharge now");
                isUserAction = true; // Reset after handling
                return;
            }
//            if(isLiveRide){
//                isUserAction = false;
//                dutySwitch.setChecked(false);
//                showError("You are not allowed to go offline while are on a live order");
//                return;
//            }
            if (isUserAction) {
                if (isChecked) {
                    showGoOnlineDialog();
                } else {
                    showGoOfflineDialog();
                }
            }
            // Reset the flag after handling the event
            isUserAction = true;
        });
*/



//        notificationIcon.setOnClickListener(v -> {
//            startActivity(new Intent(this, GoodsNotificationsActivity.class));
//        });

        ImageView bodyTypeButton = binding.toolbar.findViewById(R.id.body_type);
        ImageView locationTypeButton = binding.toolbar.findViewById(R.id.location_type);
        bodyTypeButton.setOnClickListener(v -> showBodyTypeSelection());
        locationTypeButton.setOnClickListener(v-> showLocationTypeDialog());

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
                            Log.d("Auth", "Token update response: " + message);
                            fetchControlSettings();
                            fetchCurrentPlanDetails();
                            fetchDriverStatus();
                            fetchEarnings();
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

    private void fetchControlSettings() {
        String url = APIClient.baseUrl + "get_control_settings";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                null, // No body needed for this request
                response -> {
                    try {
                        if (response.has("settings")) {
                            JSONObject settings = response.getJSONObject("settings");

                            // Save each setting to preferences
                            preferenceManager.saveStringValue("booking_timeout",
                                    settings.optString("booking_timeout", "30"));

                            preferenceManager.saveStringValue("multiple_drops",
                                    settings.optString("multiple_drops", "3"));

                            preferenceManager.saveStringValue("agent_recharge_expiry_show",
                                    settings.optString("agent_recharge_expiry_show", "No"));

                            preferenceManager.saveStringValue("hike_price_show",
                                    settings.optString("hike_price_show", "No"));

                            preferenceManager.saveStringValue("agent_cancel_button_show",
                                    settings.optString("agent_cancel_button_show", "No"));

                            // Save last updated times if needed
                            if (response.has("last_updated")) {
                                JSONObject lastUpdated = response.getJSONObject("last_updated");
                                preferenceManager.saveStringValue("settings_last_updated",
                                        lastUpdated.optString("booking_timeout", "0"));
                            }
                        }
                    } catch (Exception e) {
                        Log.e("ControlSettings", "Error parsing settings: " + e.getMessage());
                    }
                },
                error -> Log.e("ControlSettings", "Error fetching settings: " + error.getMessage())
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
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupLocationServices() {
        if (checkLocationPermission()) {
//            startLocationUpdates();
        } else {
            requestLocationPermission();
        }
    }

    private boolean checkLocationPermission() {
        return ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void startLocationUpdates() {
        System.out.println("Start Location Updates Goods Driver");
        // Start LocationService
        Intent serviceIntent = new Intent(this, LocationUpdateService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
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

    private void updateMapLocation(LatLng latLng) {
        if (mMap == null) return;

        CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(latLng, 100);
        mMap.animateCamera(yourLocation);


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void createLocationRequest() {
        locationRequest = LocationRequest.create()
                .setInterval(5000)        // Update location every 5 seconds
                .setFastestInterval(3000) // Fastest updates interval
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    updateLocationMarker(location);
                }
            }
        };
    }

    private void updateLocationMarker(Location location) {
        if (mMap != null) {
            LatLng currentLocation = new LatLng(
                    location.getLatitude(),
                    location.getLongitude()
            );

            // Update or create marker
            if (currentLocationMarker == null) {
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(currentLocation)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_current_long))
                        .anchor(0.5f, 0.5f); // Center the marker icon
                currentLocationMarker = mMap.addMarker(markerOptions);
            } else {
                currentLocationMarker.setPosition(currentLocation);
            }

            // Move camera only on first location update or as needed
            if (isFirstLocation) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 90f));
                isFirstLocation = false;
            } else {
                // Smoothly move camera to new position
                mMap.animateCamera(CameraUpdateFactory.newLatLng(currentLocation));
            }
        }
    }

    private void startHomeLocationUpdates() {
        if (checkLocationPermission()) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reset recreation flag after resume
        if (isRecreating) {
            isRecreating = false;
            // Restore previous online state without showing dialog
            if (isOnline) {
                isUserAction = false;
//                dutySwitch.setChecked(true);
                isUserAction = true;
            }
        }

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            String packageName = getPackageName();
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Log.d(TAG, "Battery optimization is not disabled, showing settings");
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(android.net.Uri.parse("package:" + packageName));
                startActivity(intent);
            } else {
                checkNotificationPermission();
            }
        }
        startHomeLocationUpdates();
    }

    private void checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 and above, use the new notification permission
            if (ActivityCompat.checkSelfPermission(this, POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Requesting notification permission");
                requestPermissionLauncher.launch(new String[]{POST_NOTIFICATIONS});
            } else {
                Log.d(TAG, "Notification permission already granted");
                checkOverlayPermission();
            }
        } else {
            // For older Android versions, check if notifications are enabled
            if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                Log.d(TAG, "Notifications are not enabled, showing settings");
                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                startActivity(intent);
                Toast.makeText(this, "Please enable notifications for this app", Toast.LENGTH_LONG).show();
            } else {
                Log.d(TAG, "Notifications are enabled, checking overlay permission");
                checkOverlayPermission();
            }
        }
    }

    private void checkAndRequestPermissions() {
        String[] permissions = {
                ACCESS_FINE_LOCATION,
                ACCESS_COARSE_LOCATION,
                POST_NOTIFICATIONS
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions = new String[]{
                    ACCESS_FINE_LOCATION,
                    ACCESS_COARSE_LOCATION,
                    ACCESS_BACKGROUND_LOCATION,
                    POST_NOTIFICATIONS
            };
        }

        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            requestPermissionLauncher.launch(permissions);
        } else {
            checkOverlayPermission();
        }
    }


    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.d(TAG, "Requesting overlay permission");
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            overlayPermissionLauncher.launch(intent);
        } else {
            Log.d(TAG, "Overlay permission already granted");
//            startBackgroundService();
        }
    }

    // Register the permissions callback
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                boolean allGranted = true;
                for (Boolean isGranted : permissions.values()) {
                    allGranted = allGranted && isGranted;
                }
                if (allGranted) {
                    Log.d(TAG, "All permissions granted");
                    checkOverlayPermission();
                } else {
                    Log.d(TAG, "Some permissions denied");
                    // Toast.makeText(this, "Required permissions are needed for the background service to work properly", Toast.LENGTH_LONG).show();
                }
            });

    // Register the overlay permission callback
    private final ActivityResultLauncher<Intent> overlayPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Settings.canDrawOverlays(this)) {
                        Log.d(TAG, "Overlay permission granted");
//                        startBackgroundService();
                    } else {
                        Log.d(TAG, "Overlay permission denied");
                        Toast.makeText(this, "Overlay permission is required for the floating window", Toast.LENGTH_LONG).show();
                    }
                }
            });


    private void checkBatteryOptimization() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            String packageName = getPackageName();
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Log.d(TAG, "Battery optimization is not disabled, showing settings");
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(android.net.Uri.parse("package:" + packageName));
                startActivity(intent);
            } else {
                Log.d(TAG, "Battery optimization is already disabled");
                checkNotificationPermission();
            }
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        stopHomeLocationUpdates();
    }

    private void stopHomeLocationUpdates() {
        if (fusedLocationClient != null)
            fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        // Improve map settings
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.setBuildingsEnabled(true);
        mMap.setMaxZoomPreference(20);
        mMap.setMinZoomPreference(10);

        // Get current location and move camera
        if (checkLocationPermission()) {
//            fusedLocationClient.getLastLocation()
//                    .addOnSuccessListener(this, location -> {
//                        if (location != null) {
//                            LatLng currentLocation = new LatLng(
//                                    location.getLatitude(),
//                                    location.getLongitude()
//                            );
//                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
//                                    currentLocation, 100f));
//                        }
//                    });
            startHomeLocationUpdates();
        }
    }

    private void setupNavigationDrawer() {
        binding.navView.setNavigationItemSelectedListener(item -> {
            Intent intent = null;

            int itemId = item.getItemId();

            // First group items
            if (itemId == R.id.nav_home) {
                if (!isGPSEnabled()) {
                    showGPSAlert();
                    return false;
                }
                binding.drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }

            else if (itemId == R.id.nav_wallet) {
                intent = new Intent(this, GoodsAgentWalletActivity.class);
            }
            // Check GPS for Live Ride
            else if (itemId == R.id.nav_live_ride) {
                if (isGPSEnabled()) {
                    intent = new Intent(this, NewLiveRideActivity.class);
                } else {
                    showGPSAlert();
                    return false;
                }
            }

            // Regular activities
            else if (itemId == R.id.nav_change_language) {
                showLanguageBottomSheet();
//                bottomLanguageList();
            } else if (itemId == R.id.nav_rides) {
                intent = new Intent(this, MyRidesActivity.class);
            } else if (itemId == R.id.nav_earnings) {
                intent = new Intent(this, MyEarningsActivity.class);
            } else if (itemId == R.id.nav_recharge) {
                intent = new Intent(this, MyRechargeHomeActivity.class);
            } else if (itemId == R.id.nav_recharge_history) {
                intent = new Intent(this, MyRechargeHistoryActivity.class);
            } else if (itemId == R.id.nav_ratings) {
                intent = new Intent(this, MyRatingsActivity.class);
            } else if (itemId == R.id.nav_faqs) {
                intent = new Intent(this, GoodsFAQSActivity.class);
            } else if (itemId == R.id.nav_help) {
//                intent = new Intent(this, HelpAndSupportActivity.class);
                openWebUrl("https://vtpartner.org/terms&conditions");
            }

            // Second group items (Account actions)
            else if (itemId == R.id.nav_delete_account) {
                showDeleteAccountDialog();
                binding.drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            } else if (itemId == R.id.nav_logout) {
                showLogoutDialog();
                binding.drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }

            // Handle activity start
            if (intent != null) {
                startActivity(intent);
                binding.drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
            return false;
        });

        // Optional: Set item background color when selected
//        binding.navView.setItemBackgroundResource(R.drawable.nav_item_background_selector);
    }

    private BottomSheetDialog languageBottomSheet;
    private void showLanguageBottomSheet() {
        if (languageBottomSheet == null) {
            languageBottomSheet = new BottomSheetDialog(this);
            View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_language, null);
            languageBottomSheet.setContentView(sheetView);

            RadioGroup languageGroup = sheetView.findViewById(R.id.languageRadioGroup);
            MaterialButton applyButton = sheetView.findViewById(R.id.btnApply);

            // Set current language as checked
            String currentLang = preferenceManager.getStringValue("language", "en");
            switch (currentLang) {
                case "hi":
                    languageGroup.check(R.id.radioHindi);
                    break;
                case "mr":
                    languageGroup.check(R.id.radioMarathi);
                    break;
                case "kn":
                    languageGroup.check(R.id.radioKannada);
                    break;
                default:
                    languageGroup.check(R.id.radioEnglish);
            }

            applyButton.setOnClickListener(v -> {
                int selectedId = languageGroup.getCheckedRadioButtonId();
                String langCode;

                if (selectedId == R.id.radioHindi) {
                    langCode = "hi";
                } else if (selectedId == R.id.radioMarathi) {
                    langCode = "mr";
                } else if (selectedId == R.id.radioKannada) {
                    langCode = "kn";
                } else {
                    langCode = "en";
                }

                preferenceManager.saveStringValue("language", langCode);
                setLocale(langCode);
                languageBottomSheet.dismiss();
                recreate();
            });
        }
        languageBottomSheet.show();
    }

    private void setLocale(String langCode) {
        // Set flag before recreation
        isRecreating = true;

        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    public void bottomLanguageList() {
        BottomSheetDialog mBottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.custome_launguage, null);
        LinearLayout lvlenglish = sheetView.findViewById(R.id.lvl_english);
//        LinearLayout lvlGujrati = sheetView.findViewById(R.id.lvl_gujrati);
//        LinearLayout lvlarb = sheetView.findViewById(R.id.lvl_arb);
//        LinearLayout lvlhind = sheetView.findViewById(R.id.lvl_hind);
//        LinearLayout lvlIndonesiya = sheetView.findViewById(R.id.lvl_indonesiya);
//        LinearLayout lvlBangali = sheetView.findViewById(R.id.lvl_bangali);
//        LinearLayout lvlAfrikan = sheetView.findViewById(R.id.lvl_afrikan);

//        lvlIndonesiya.setOnClickListener(v -> {
//
//            sessionManager.setStringData(language, "in");
//            startActivity(new Intent(getActivity(), HomeActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
//            getActivity().finish();
//        });
//
//        lvlBangali.setOnClickListener(v -> {
//
//            sessionManager.setStringData(language, "bn");
//            startActivity(new Intent(getActivity(), HomeActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
//            getActivity().finish();
//        });
//
//        lvlAfrikan.setOnClickListener(v -> {
//
//            sessionManager.setStringData(language, "af");
//            startActivity(new Intent(getActivity(), HomeActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
//            getActivity().finish();
//        });
//
//        lvlenglish.setOnClickListener(v -> {
//
//            sessionManager.setStringData(language, "en");
//            startActivity(new Intent(getActivity(), HomeActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
//            getActivity().finish();
//        });
//
//        lvlGujrati.setOnClickListener(v -> {
//
//            sessionManager.setStringData(language, "es");
//            startActivity(new Intent(getActivity(), HomeActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
//            getActivity().finish();
//
//
//        });
//
//        lvlarb.setOnClickListener(v -> {
//
//            sessionManager.setStringData(language, "ar");
//            startActivity(new Intent(getActivity(), HomeActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
//            getActivity().finish();
//
//
//        });
//
//        lvlhind.setOnClickListener(v -> {
//
//            sessionManager.setStringData(language, "hi");
//            startActivity(new Intent(getActivity(), HomeActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
//            getActivity().finish();
//
//
//        });


        mBottomSheetDialog.setContentView(sheetView);
        mBottomSheetDialog.show();
    }

    private void openWebUrl(String url) {
        try {
            // Show loading if needed
            // progressDialog.show();

            Intent browserIntent = new Intent(Intent.ACTION_VIEW);
            browserIntent.setData(Uri.parse(url));

            // Verify that there's an app to handle this intent
            if (browserIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(browserIntent);
            } else {
                Toast.makeText(this, "No web browser found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open website", Toast.LENGTH_SHORT).show();
        }
    }

    private void showGoOnlineDialog() {

        // Check plan expiry first
        if (planExpired) {
            isUserAction = false;
//            dutySwitch.setChecked(false);
            dutySwipeButton.reset();
            showDutyStatusCheckBox(true, false);
            isUserAction = true;
            showError("Please recharge before you go online");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Go On Duty")
                .setMessage("Are you sure you want to go duty?.\nTo go On Duty you have to provide recent selfie with your vehicle.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (checkLocationPermission()) {
//                        startLocationUpdates();
                    } else {
                        requestLocationPermission();
                    }

                    showSelfieDialog();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    isUserAction = false;
                    dutySwipeButton.reset();
//                    dutySwitch.setChecked(false);
                    stopLocationUpdates();
                    showDutyStatusCheckBox(true, false);
                    isUserAction = true; // Reset after handling

                })
                .setCancelable(false)

                .show();
    }

    private void stopLocationUpdates() {
        LocationUpdateService.isStoppedManually = true;
        Intent serviceIntent = new Intent(this, LocationUpdateService.class);
        stopService(serviceIntent);
        Intent floatingIntent = new Intent(this, FloatingWindowService.class);
        stopService(floatingIntent);
    }

    private void showSelfieDialog() {
        dutySwipeButton.reset();
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
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
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider, dialog);
            } catch (Exception e) {
                Log.e(TAG, "Error setting up camera", e);
                dialog.dismiss();

                Toast.makeText(this, "Failed to setup camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
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

            Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void takePicture(Dialog dialog) {
        if (cameraController == null) return;

        File photoFile = createImageFile();
        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        ((ImageCapture) cameraController).takePicture(outputFileOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        dialog.dismiss();
                        uploadImage(photoFile);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        dialog.dismiss();
                        showDutyStatusCheckBox(true, isOnline);
                        Toast.makeText(HomeActivity.this,
                                "Failed to capture image", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "SELFIE_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            Log.e(TAG, "Error creating image file", e);
            return null;
        }
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
            File compressedFile = new File(this.getCacheDir(), "compressed_" + imageFile.getName());
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
        ProgressDialog progressDialog = new ProgressDialog(this);
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

                        Toast.makeText(this,
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

        VolleySingleton.getInstance(this).addToRequestQueue(multipartRequest);
    }

    private void onUploadError(String message) {

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showGoOfflineDialog() {

        new AlertDialog.Builder(this)
                .setTitle("Go Off Duty")
                .setMessage("Are you sure you want to go offline?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    updateDriverStatus(false);
                })
                .setNegativeButton("No", (dialog, which) -> {
                    isUserAction = false;
//                    dutySwitch.setChecked(true);
                    dutySwipeButton.reset();
                    showDutyStatusCheckBox(true, isOnline);
                })
                .setCancelable(false)
                .show();
    }


    private void fetchCurrentPlanDetails() {
        //showLoading(true);
        String driverId = preferenceManager.getStringValue("goods_driver_id");
        String token = preferenceManager.getStringValue("goods_driver_token");

        try {
            JSONObject params = new JSONObject();
            params.put("driver_id", preferenceManager.getStringValue("goods_driver_id"));
            params.put("driver_unique_id", driverId);
            params.put("auth", token);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    APIClient.baseUrl + "goods_driver_current_new_recharge_details",
                    params,
                    response -> {
                        //showLoading(false);

                        handleCurrentPlanResponse(response);
                    },
                    error -> {
                        //showLoading(false);
                        System.out.println("error_new_recharge::" + error);
                        handleError(error);
                        binding.planDetailsLyt.setVisibility(View.GONE);
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

        } catch (Exception e) {
//            showLoading(false);
            showError("Error fetching plan details");
        }
    }

    private void handleError(VolleyError error) {
        System.out.println("VolleyError_error::" + error);
        String message;

        // Check if there's a network response
        if (error.networkResponse != null) {
            int statusCode = error.networkResponse.statusCode;

            switch (statusCode) {

                case 404:
                    noPlanYet = true;
                    message = "No active recharge plan found";
                    binding.planDetailsLyt.setVisibility(View.GONE);
                    break;
                case 400:
                    message = "Bad request";
                    break;
                case 500:
                    message = "Server error";
                    break;
                default:
                    message = "Error fetching plan details";
                    break;
            }
        } else {
            // Handle cases where there's no network response
            if (error instanceof NetworkError) {
                message = "No internet connection";
            } else if (error instanceof TimeoutError) {
                message = "Request timed out";
            } else if (error instanceof ServerError) {
                message = "Server error";
            } else {
                message = "Error fetching plan details";
            }
        }

        showError(message);
    }

    private void handleCurrentPlanResponse(JSONObject response) {
        try {
            JSONArray results = response.optJSONArray("results");
            if (results != null && results.length() > 0) {
                JSONObject planDetails = results.getJSONObject(0);
                updateCurrentPlanUI(planDetails);
                binding.planDetailsLyt.setVisibility(View.VISIBLE);
            } else {
                noPlanYet = true;
                binding.planDetailsLyt.setVisibility(View.GONE);
                showNoPlanUI();
            }
        } catch (JSONException e) {
            Log.e("RechargeHome", "Error parsing response: " + e.getMessage());
            showError("Error loading plan details");
        }
    }

    private void updateCurrentPlanUI(JSONObject planDetails) {
        try {
            // Update plan title
            String planTitle = planDetails.getString("plan_title");
            binding.currentPlanTitle.setText(planTitle);

            // Update plan description if you have a TextView for it
            String planDescription = planDetails.getString("plan_description");
            // binding.currentPlanDescription.setText(planDescription);

            // Update plan price
            double planPrice = planDetails.getDouble("plan_price");
            binding.planPrice.setText(String.format(Locale.US, "₹%.2f", planPrice));

            // Parse and format expiry time
            String expiryTime = planDetails.getString("expiry_time");
            updateExpiryStatus(expiryTime);

        } catch (JSONException e) {
            Log.e("RechargeHome", "Error updating UI: " + e.getMessage());
            showError("Error displaying plan details");
        }
    }

    private void updateExpiryStatus(String expiryTimeStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US);

            Date expiryDate = inputFormat.parse(expiryTimeStr);
            Date currentDate = new Date();

            boolean isExpired = currentDate.after(expiryDate);

            if (isExpired) {
//                if (isLiveRide == false) {
//                    updateDriverStatus(false);
//                }
                planExpired = true;
                binding.currentPlanValidity.setText("Plan Expired");

                showPurchasePlanButton();

                binding.currentPlanValidity.setTextColor(getResources().getColor(R.color.colorerror));
            } else {
                // Calculate remaining time
                long diffInMillis = expiryDate.getTime() - currentDate.getTime();
                String remainingTime = formatRemainingTime(diffInMillis);

                binding.currentPlanValidity.setText(String.format("Valid till: %s\n%s",
                        outputFormat.format(expiryDate),
                        remainingTime));
                binding.btnBuyPlan.setVisibility(View.GONE);
                binding.currentPlanValidity.setTextColor(getResources().getColor(R.color.green));
            }
        } catch (ParseException e) {
            Log.e("RechargeHome", "Error parsing date: " + e.getMessage());
            binding.currentPlanValidity.setText("Expiry: " + expiryTimeStr);
        }
    }

    private String formatRemainingTime(long milliseconds) {
        long days = TimeUnit.MILLISECONDS.toDays(milliseconds);
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds) % 24;

        if (days > 0) {
            return String.format(Locale.US, "%d days %d hours remaining", days, hours);
        } else if (hours > 0) {
            return String.format(Locale.US, "%d hours remaining", hours);
        } else {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60;
            return String.format(Locale.US, "%d minutes remaining", minutes);
        }
    }

    private void showNoPlanUI() {
        // Update UI to show no active plan
        binding.currentPlanTitle.setText("No Active Plan");
        binding.currentPlanValidity.setText("Purchase a plan to continue");
        binding.planPrice.setText("₹0.00");

        // Optionally show a button to purchase plan
        showPurchasePlanButton();
    }

    private void showPurchasePlanButton() {
        // If you have a purchase button in your layout
        if (binding.btnBuyPlan != null) {
            binding.btnBuyPlan.setVisibility(View.VISIBLE);
            binding.btnBuyPlan.setOnClickListener(v -> {
                // Navigate to purchase plan screen
                 startActivity(new Intent(this, MyRechargeHomeActivity.class));
            });
        }
    }


    private void showError(String message) {
        snackbar = Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG)
                .setAction("DISMISS", view -> {
                    // Dismiss the Snackbar
                    snackbar.dismiss();
                });

        // Get the Snackbar view
        View snackbarView = snackbar.getView();

        // Set background color to red
        snackbarView.setBackgroundColor(ContextCompat.getColor(this, R.color.colorerror));

        // Get the text view within Snackbar
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);

        // Set text color to white
        textView.setTextColor(Color.WHITE);

        // Style the action button
        snackbar.setActionTextColor(Color.WHITE);

        // Make text size appropriate
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

        // Allow multiple lines
        textView.setMaxLines(5);  // Increased max lines
        textView.setSingleLine(false);  // Allow multiple lines
        textView.setEllipsize(null);  // Remove ellipsis

        // Set layout parameters to wrap content
        ViewGroup.LayoutParams params = textView.getLayoutParams();
        if (params instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) params).width = ViewGroup.LayoutParams.MATCH_PARENT;
            textView.setLayoutParams(params);
        }

        // Center align the text
        textView.setGravity(Gravity.CENTER_VERTICAL);

        // Increase Snackbar duration for longer messages
        snackbar.setDuration(Snackbar.LENGTH_LONG);

        // Add padding to the Snackbar view
        snackbarView.setPadding(
                snackbarView.getPaddingLeft() + 12,
                snackbarView.getPaddingTop(),
                snackbarView.getPaddingRight() + 12,
                snackbarView.getPaddingBottom()
        );

        // Optional: Increase the min height of the Snackbar
//        snackbarView.setMinimumHeight(
//                getResources().getDimensionPixelSize(R.dimen.snackbar_min_height)
//        );

        snackbar.show();
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Show confirmation dialog
                    new AlertDialog.Builder(this)
                            .setTitle("Request Sent")
                            .setMessage("Your account deletion request has been sent to our team. " +
                                    "We will process it within 24-48 hours.")
                            .setPositiveButton("OK", (innerDialog, innerWhich) -> {
                                // Optional: Send API request to backend about deletion request
//                                sendDeleteAccountRequest();
                            })
                            .show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    // Clear all preferences
                    clearAllUserData();

                    // Navigate to DriverTypeActivity
                    Intent intent = new Intent(this, DriverTypeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearAllUserData() {
        // Clear all relevant preferences
        preferenceManager.clearPreferences();
        preferenceManager.saveBooleanValue("firstRun",true);
        // Stop any ongoing services
        stopLocationUpdates();

        // Stop any foreground services if running
        Intent serviceIntent = new Intent(this, LocationUpdateService.class);
        stopService(serviceIntent);

        // Clear any cached data
        try {
            File cacheDir = getCacheDir();
            deleteDir(cacheDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        return dir.delete();
    }

    private boolean isGPSEnabled() {
        if (locationManager == null) return false;
        try {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            Log.e(TAG, "Error checking GPS status: " + e.getMessage());
            return false;
        }
    }

    private void showGPSAlert() {
        if (Utility.hasGPSDevice(this)) {
            Toast.makeText(this, "Please enable GPS to continue", Toast.LENGTH_SHORT).show();
            Utility.enableLoc(this);
        } else {
            Toast.makeText(this, "GPS device not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateDriverStatus(boolean online) {
        String driverId = preferenceManager.getStringValue("goods_driver_id");
        String token = preferenceManager.getStringValue("goods_driver_token");

        String url = APIClient.baseUrl + "goods_driver_update_online_status";

        JSONObject params = new JSONObject();
        try {
            params.put("goods_driver_id", preferenceManager.getStringValue("goods_driver_id"));
            params.put("status", online ? 1 : 0);
            params.put("lat", latitude);
            params.put("lng", longitude);
            params.put("recent_online_pic", preferenceManager.getStringValue("recent_online_pic"));
            params.put("driver_unique_id", driverId);
            params.put("auth", token);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> {
                    System.out.println("updating is online ::"+isOnline);
                    isOnline = online;
                    if (online) {
                        addToActiveDriverTable(latitude, longitude);
                    } else {
                        deleteFromActiveDriverTable();
                    }
                },
                error -> {
                    Toast.makeText(this, "Error updating status", Toast.LENGTH_SHORT).show();
                    // Revert UI changes if update failed
                    if (online) {
                        showDutyStatusCheckBox(true, online);
                    } else {
                        showDutyStatusCheckBox(false, online);
                    }
                });

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        volleySingleton.addToRequestQueue(request);
    }

    private void addToActiveDriverTable(double latitude, double longitude) {
        String driverId = preferenceManager.getStringValue("goods_driver_id");
        String token = preferenceManager.getStringValue("goods_driver_token");

        String url = APIClient.baseUrl + "add_new_active_goods_driver";

        JSONObject params = new JSONObject();
        try {
            params.put("goods_driver_id", preferenceManager.getStringValue("goods_driver_id"));
            params.put("status", isOnline ? 1 : 0);
            params.put("current_lat", latitude);
            params.put("current_lng", longitude);
            params.put("driver_unique_id", driverId);
            params.put("auth", token);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> {
                    Toast.makeText(this, "You are Online now", Toast.LENGTH_SHORT).show();
                    showDutyStatusCheckBox(true, isOnline);
                    startLocationUpdates();
                },
                error -> {
                    Log.e(TAG, "Error adding to active drivers: " + error.toString());
                    Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
                    // Revert UI state
                    showDutyStatusCheckBox(true, isOnline);
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
        String driverId = preferenceManager.getStringValue("goods_driver_id");
        String token = preferenceManager.getStringValue("goods_driver_token");

        String url = APIClient.baseUrl + "delete_active_goods_driver";

        JSONObject params = new JSONObject();
        try {
            params.put("goods_driver_id", preferenceManager.getStringValue("goods_driver_id"));
            params.put("driver_unique_id", driverId);
            params.put("auth", token);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> {
                    Snackbar.make(binding.getRoot(), "You are offline now", Snackbar.LENGTH_SHORT).show();

                    // Update UI state
                    isOnline = false;
                    showDutyStatusCheckBox(true, isOnline);

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
                    Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
                    showDutyStatusCheckBox(true, isOnline);
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


    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void fetchDriverStatus() {
        String driverId = preferenceManager.getStringValue("goods_driver_id");
        String token = preferenceManager.getStringValue("goods_driver_token");
        String url = APIClient.baseUrl + "goods_driver_online_status";


        if (driverId.isEmpty()) {
            // Navigate to login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        JSONObject params = new JSONObject();
        try {
            params.put("goods_driver_id", driverId);
            params.put("driver_unique_id", driverId);
            params.put("auth", token);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        if (results.length() > 0) {
                            JSONObject data = results.getJSONObject(0);


                            profilePic = data.getString("profile_pic");
                            mobileNo = data.getString("mobile_no");
                            int is_online_status = data.getInt("is_online");
                            if (is_online_status == 1) isOnline = true;
                            else isOnline = false;
                            String status = data.getString("status");
                            String recentOnlinePic = data.getString("recent_online_pic");
                            String vehicleName = data.getString("vehicle_name");
                            String vehicleImage = data.getString("vehicle_image");
                            vehicleId = data.getInt("vehicle_id");
                            String bodyType = data.getString("body_type");
                            preferenceManager.saveStringValue("body_type", bodyType);
                            if (isOnline == false) {
                                stopLocationUpdates();
                            }
                            preferenceManager.saveStringValue("recent_online_pic", recentOnlinePic);
                            if(vehicleId == 2){
                                //HIde open body option here
                                binding.bodyType.setVisibility(View.GONE);
                            }
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
                                    showDutyStatusCheckBox(true, isOnline);
                                    if (isOnline)
                                        startLocationUpdates();
                                    break;
                            }

                            driverName = data.getString("driver_first_name");
                            // Update UI with driver status
                            View headerView = binding.navView.getHeaderView(0);

                            // Load profile image using Glide
                            Glide.with(HomeActivity.this)
                                    .load(profilePic)
                                    .placeholder(R.drawable.ic_image_placeholder)
                                    .error(R.drawable.ic_image_placeholder)
                                    .override(100, 100)
                                    .into((ImageView) headerView.findViewById(R.id.profile_image));

                            Glide.with(HomeActivity.this)
                                    .load(vehicleImage)
                                    .placeholder(R.drawable.ic_image_placeholder)
                                    .error(R.drawable.ic_image_placeholder)
                                    .override(100, 100)
                                    .into((ImageView) headerView.findViewById(R.id.vehicle_image));

                            TextView driverNameTextView = (TextView) headerView.findViewById(R.id.driver_name);
                            TextView driverPhoneTextView = (TextView) headerView.findViewById(R.id.driver_phone);
                            TextView driverVehicleNameTextView = (TextView) headerView.findViewById(R.id.driver_vehicle_name);
                            driverNameTextView.setText(driverName);
                            driverPhoneTextView.setText(mobileNo);
                            driverVehicleNameTextView.setText(vehicleName);
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

    private void showDutyStatusCheckBox(boolean visible, boolean isOnline) {
        isUserAction = false;
// Reset the swipe button first
        dutySwipeButton.reset();
        if (visible) {
            dutySwipeButton.setVisibility(View.VISIBLE);
            if (isOnline) {
                dutySwipeButton.setText("Slide to Go Off Duty");
                dutySwipeButton.setColors(
                        getResources().getColor(R.color.colorerror),
                        Color.WHITE
                );
            } else {
                dutySwipeButton.setText("Slide to Go On Duty");
                dutySwipeButton.setColors(
                        getResources().getColor(R.color.green),
                        Color.WHITE
                );
            }
        } else {
            dutySwipeButton.setVisibility(View.GONE);
        }

        new Handler().postDelayed(() -> {
            isUserAction = !isRecreating;
        }, 100);
    }

    /*private void showDutyStatusCheckBox(boolean visible, boolean isOnline) {
        // Temporarily disable user action tracking
        isUserAction = false;

        if (visible) {
            binding.dutySwitch.setVisibility(View.VISIBLE);
            binding.dutySwitch.setChecked(isOnline);
        } else {
            binding.dutySwitch.setVisibility(View.GONE);
            binding.dutySwitch.setChecked(false);
        }

        binding.dutySwitch.setText(isOnline ? "Go Off Duty" : "Go On Duty");
        // Reset the flag after a short delay to ensure the switch state is set
        new Handler().postDelayed(() -> {
            isUserAction = !isRecreating; // Only enable user actions if not recreating
        }, 100);
    }*/


    private void fetchEarnings() {
        String url = APIClient.baseUrl + "goods_driver_todays_earnings";
        String driverId = preferenceManager.getStringValue("goods_driver_id");
        String token = preferenceManager.getStringValue("goods_driver_token");

        if (driverId.isEmpty()) return;

        JSONObject params = new JSONObject();
        try {
            params.put("driver_id", driverId);
            params.put("driver_unique_id", driverId);
            params.put("auth", token);
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

                            totalEarnings = data.getDouble("total_earnings");
                            totalRides = data.getInt("total_rides");

                            binding.totalRides.setText(todaysRides + "");
                            binding.walletBalance.setText("₹" + todaysEarnings + "/-");
                            View headerView = binding.navView.getHeaderView(0);

                            binding.rideDetailsLyt.setVisibility(View.VISIBLE);
                            TextView totalRidesTextView = (TextView) headerView.findViewById(R.id.total_rides);
                            TextView earningsTextView = (TextView) headerView.findViewById(R.id.wallet_balance);
                            totalRidesTextView.setText(totalRides + "");
                            earningsTextView.setText("₹" + totalEarnings + "/-");
                            updateEarningsUI();

                        }
                    } catch (JSONException e) {
                        binding.rideDetailsLyt.setVisibility(View.GONE);
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

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void updateEarningsUI() {
//        binding.navView.
    }

//    private void fetchCurrentBalance() {
//        String url = APIClient.baseUrl + "get_goods_driver_current_recharge_details";
//        String driverId = preferenceManager.getStringValue("goods_driver_id");
//
//        JSONObject params = new JSONObject();
//        try {
//            params.put("driver_id", driverId);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
//                response -> {
//                    try {
//                        JSONArray results = response.getJSONArray("results");
//                        if (results.length() > 0) {
//                            JSONObject data = results.getJSONObject(0);
//
//                            // Handle balance and negative points
//                            currentBalance = data.getString("remaining_points");
//                            double negativePoints = data.getDouble("negative_points");
//
//                            if (negativePoints > 0) {
//                                currentBalance = String.valueOf(negativePoints);
//                                double limitExceededBalance = Double.parseDouble(currentBalance);
//                                currentBalance = "-" + currentBalance;
////TODO:check recharge expiry here later
////                                if (!isShowingLiveRideDetails()) {
////                                    goOffline();
////                                    showRechargeAlert();
////                                }
//                            }
//
//                            // Check expiry date
//                            String validTillDate = data.getString("valid_till_date");
//                            checkExpiryDate(validTillDate);
//
//                            updateBalanceUI();
//                        }
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                },
//                error -> {
//                    if (error.toString().contains("No Data Found")) {
//                        showToast("Not Yet Subscribed to any Top-Up Recharge plan");
//                    }
//                });
//
//        volleySingleton.addToRequestQueue(request);
//    }

    private void updateVerificationStatus(String statusText) {
        //Show the not verified status here
        binding.verificationStatus.setVisibility(View.VISIBLE);
//        binding.dutySwitch.setVisibility(View.GONE);
        binding.dutySwipeButton.setVisibility(View.GONE);
        binding.verificationStatus.setText(statusText);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        stopHomeLocationUpdates();
    }
}


