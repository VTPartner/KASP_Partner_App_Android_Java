package com.kapstranspvtltd.kaps_partner.driver_app_activities;

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
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;

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
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.messaging.FirebaseMessaging;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.cab_driver_activities.CabDriverHomeActivity;

import com.kapstranspvtltd.kaps_partner.common_activities.DriverTypeActivity;
import com.kapstranspvtltd.kaps_partner.common_activities.LoginActivity;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityCabDriverHomeBinding;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityDriverAgentHomeBinding;
import com.kapstranspvtltd.kaps_partner.driver_app_activities.settings_pages.DriverAgentAllRidesActivity;
import com.kapstranspvtltd.kaps_partner.driver_app_activities.settings_pages.DriverAgentEarningsActivity;
import com.kapstranspvtltd.kaps_partner.driver_app_activities.settings_pages.DriverAgentEditProfileActivity;
import com.kapstranspvtltd.kaps_partner.driver_app_activities.settings_pages.DriverAgentFAQSActivity;
import com.kapstranspvtltd.kaps_partner.driver_app_activities.settings_pages.DriverAgentNewLiveRideActivity;
import com.kapstranspvtltd.kaps_partner.driver_app_activities.settings_pages.DriverAgentRatingsActivity;
import com.kapstranspvtltd.kaps_partner.driver_app_activities.settings_pages.DriverAgentRechargeHistoryActivity;
import com.kapstranspvtltd.kaps_partner.driver_app_activities.settings_pages.DriverAgentRechargeHomeActivity;
import com.kapstranspvtltd.kaps_partner.driver_app_activities.settings_pages.DriverAgentWalletActivity;
import com.kapstranspvtltd.kaps_partner.fcm.AccessToken;
import com.kapstranspvtltd.kaps_partner.handyman_agent_activities.settings_pages.HandyManAgentWalletActivity;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.MultipartRequest;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;
import com.kapstranspvtltd.kaps_partner.services.CabLocationUpdateService;
import com.kapstranspvtltd.kaps_partner.services.FloatingWindowService;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;
import com.kapstranspvtltd.kaps_partner.utils.Utility;

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

public class DriverAgentHomeActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private ActivityDriverAgentHomeBinding binding;
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
    private SwitchMaterial dutySwitch;

    private Double latitude = 0.0;

    private Double longitude = 0.0;

    String profilePic = "", mobileNo = "", driverName = "";
    boolean isVerified = false, isOnline = false;

    Double todaysEarnings = 0.0, totalEarnings = 0.0;
    int todaysRides = 0, totalRides = 0;

    private VolleySingleton volleySingleton;

    private boolean isUserAction = true;

    boolean planExpired = false;
    boolean noPlanYet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDriverAgentHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        View headerView = binding.navViewDriver.getHeaderView(0);
        TextView editProfileBtn = headerView.findViewById(R.id.edit_profile);
        editProfileBtn.setOnClickListener(v -> goToEditProfilePage());

        preferenceManager = new PreferenceManager(this);
        volleySingleton = VolleySingleton.getInstance(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Initialize location request
        createLocationRequest();

        // Initialize location callback
        createLocationCallback();



        //setupLocationCallback();
        initializeViews();
        setupNavigationDrawer();
        setupMap();
        setupLocationServices();
        getFCMToken();
//        fetchCurrentPlanDetails();
//        fetchDriverStatus();
//        fetchEarnings();
    }

    private void goToEditProfilePage() {
        Intent intent = new Intent(this, DriverAgentEditProfileActivity.class);
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
        dutySwitch = binding.toolbar.findViewById(R.id.duty_switch);
        ImageView menuIcon = binding.toolbar.findViewById(R.id.menu_icon);
        ImageView liveOrderIcon = binding.toolbar.findViewById(R.id.live_order_icon);
        ImageView notificationIcon = binding.toolbar.findViewById(R.id.notification_icon);

        Glide.with(this)
                .asGif()
                .load(R.drawable.live_ride)
                .into(liveOrderIcon);
        // Setup click listeners
        menuIcon.setOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.START));

        // Setup duty switch with user action tracking
        dutySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUserAction) {
                return; // Skip if this is a programmatic change
            }

            if (noPlanYet) {
                isUserAction = false;
                dutySwitch.setChecked(false);
                showError("No active recharge plan found.Please recharge now");
                isUserAction = true;
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

        liveOrderIcon.setOnClickListener(v -> {
            startActivity(new Intent(this, DriverAgentNewLiveRideActivity.class));
        });

//        notificationIcon.setOnClickListener(v -> {
//            startActivity(new Intent(this, GoodsNotificationsActivity.class));
//        });
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
                    preferenceManager.saveStringValue("other_driver_token", token);
                    updateDriverAuthToken(token);
                });
    }

    private void updateDriverAuthToken(String deviceToken) {
        Log.d("FCMNewTokenFound", "updating goods driver authToken");

        String driverId = preferenceManager.getStringValue("other_driver_id");

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
                String url = APIClient.baseUrl + "update_firebase_other_driver_token";

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("other_driver_id", driverId);
                jsonBody.put("authToken", deviceToken);

                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        jsonBody,
                        response -> {
                            String message = response.optString("message");
                            Log.d("Auth", "Token update response: " + message);
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
        // Start LocationService
        Intent serviceIntent = new Intent(this, CabLocationUpdateService.class);
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
        startHomeLocationUpdates();
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
        binding.navViewDriver.setNavigationItemSelectedListener(item -> {
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
                intent = new Intent(this, DriverAgentWalletActivity.class);
            }
            // Check GPS for Live Ride
            else if (itemId == R.id.nav_live_ride) {
                if (isGPSEnabled()) {
                    intent = new Intent(this, DriverAgentNewLiveRideActivity.class);
                } else {
                    showGPSAlert();
                    return false;
                }
            }

            // Regular activities
            else if (itemId == R.id.nav_change_language) {
//                bottomLanguageList();
                showLanguageBottomSheet();
            } else if (itemId == R.id.nav_rides) {
                intent = new Intent(this, DriverAgentAllRidesActivity.class);
            } else if (itemId == R.id.nav_earnings) {
                intent = new Intent(this, DriverAgentEarningsActivity.class);
            } else if (itemId == R.id.nav_recharge) {
                intent = new Intent(this, DriverAgentRechargeHomeActivity.class);
            } else if (itemId == R.id.nav_recharge_history) {
                intent = new Intent(this, DriverAgentRechargeHistoryActivity.class);
            } else if (itemId == R.id.nav_ratings) {
                intent = new Intent(this, DriverAgentRatingsActivity.class);
            } else if (itemId == R.id.nav_faqs) {
                intent = new Intent(this, DriverAgentFAQSActivity.class);
            } else if (itemId == R.id.nav_help) {
//                intent = new Intent(this, HelpAndSupportActivity.class);
                openWebUrl("https://www.kaps9.in/terms&conditions");
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
                    dutySwitch.setChecked(false);
                    stopLocationUpdates();
                    showDutyStatusCheckBox(true, false);
                    isUserAction = true;
                })
                .show();
    }

    private void stopLocationUpdates() {
        CabLocationUpdateService.isStoppedManually = true;
        Intent serviceIntent = new Intent(this, CabLocationUpdateService.class);
        stopService(serviceIntent);
        Intent floatingIntent = new Intent(this, FloatingWindowService.class);
        stopService(floatingIntent);
    }

    private void showSelfieDialog() {
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
                        Toast.makeText(DriverAgentHomeActivity.this,
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
                    dutySwitch.setChecked(true);
                    showDutyStatusCheckBox(true, isOnline);
                })
                .show();
    }


    private void fetchCurrentPlanDetails() {
        //showLoading(true);
        String driverId = preferenceManager.getStringValue("other_driver_id");
        String token = preferenceManager.getStringValue("other_driver_token");
        try {
            JSONObject params = new JSONObject();
            params.put("driver_id", preferenceManager.getStringValue("other_driver_id"));
            params.put("driver_unique_id", driverId);
            params.put("auth", token);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    APIClient.baseUrl + "other_driver_current_new_recharge_details",
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
                binding.currentPlanValidity.setTextColor(getResources().getColor(R.color.colorerror));
            } else {
                // Calculate remaining time
                long diffInMillis = expiryDate.getTime() - currentDate.getTime();
                String remainingTime = formatRemainingTime(diffInMillis);

                binding.currentPlanValidity.setText(String.format("Valid till: %s\n%s",
                        outputFormat.format(expiryDate),
                        remainingTime));
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
        if (binding.btnPurchasePlan != null) {
            binding.btnPurchasePlan.setVisibility(View.VISIBLE);
            binding.btnPurchasePlan.setOnClickListener(v -> {
                // Navigate to purchase plan screen
                // startActivity(new Intent(this, PurchasePlanActivity.class));
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
        Intent serviceIntent = new Intent(this, CabLocationUpdateService.class);
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
        String driverId = preferenceManager.getStringValue("other_driver_id");
        String token = preferenceManager.getStringValue("other_driver_token");

        String url = APIClient.baseUrl + "other_driver_update_online_status";

        JSONObject params = new JSONObject();
        try {
            params.put("other_driver_id", preferenceManager.getStringValue("other_driver_id"));
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
        String driverId = preferenceManager.getStringValue("other_driver_id");
        String token = preferenceManager.getStringValue("other_driver_token");

        String url = APIClient.baseUrl + "add_new_active_other_driver";

        JSONObject params = new JSONObject();
        try {
            params.put("other_driver_id", preferenceManager.getStringValue("other_driver_id"));
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
        String driverId = preferenceManager.getStringValue("other_driver_id");
        String token = preferenceManager.getStringValue("other_driver_token");

        String url = APIClient.baseUrl + "delete_active_other_driver";

        JSONObject params = new JSONObject();
        try {
            params.put("other_driver_id", preferenceManager.getStringValue("other_driver_id"));
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

        String url = APIClient.baseUrl + "other_driver_online_status";
        String driverId = preferenceManager.getStringValue("other_driver_id");
        String token = preferenceManager.getStringValue("other_driver_token");

        if (driverId.isEmpty()) {
            // Navigate to login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        JSONObject params = new JSONObject();
        try {
            params.put("other_driver_id", driverId);
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
                            String subCatID = data.getString("sub_cat_id");
                            String subCatName = data.getString("sub_cat_name");
                            String serviceId = data.getString("service_id");
                            String serviceName = data.getString("service_name");
                            if (isOnline == false) {
                                stopLocationUpdates();
                            }
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
                                    showDutyStatusCheckBox(true, isOnline);
                                    if (isOnline)
                                        startLocationUpdates();
                                    break;
                            }

                            driverName = data.getString("driver_first_name");
                            // Update UI with driver status
                            View headerView = binding.navViewDriver.getHeaderView(0);

                            // Load profile image using Glide
                            Glide.with(DriverAgentHomeActivity.this)
                                    .load(profilePic)
                                    .placeholder(R.drawable.ic_image_placeholder)
                                    .error(R.drawable.ic_image_placeholder)
                                    .override(100, 100)
                                    .into((ImageView) headerView.findViewById(R.id.profile_image));

//                            Glide.with(DriverAgentHomeActivity.this)
//                                    .load(vehicleImage)
//                                    .placeholder(R.drawable.ic_image_placeholder)
//                                    .error(R.drawable.ic_image_placeholder)
//                                    .into((ImageView) headerView.findViewById(R.id.vehicle_image));

                            TextView driverNameTextView = (TextView) headerView.findViewById(R.id.driver_name);
                            TextView driverPhoneTextView = (TextView) headerView.findViewById(R.id.driver_phone);
                            TextView driverVehicleNameTextView = (TextView) headerView.findViewById(R.id.driver_vehicle_name);
                            driverNameTextView.setText(driverName);
                            driverPhoneTextView.setText(mobileNo);
                            driverVehicleNameTextView.setText(serviceName.equalsIgnoreCase("NA")==false ? subCatName+"/"+serviceName:subCatName);
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
            isUserAction = true;
        }, 100);
    }


    private void fetchEarnings() {
        String driverId = preferenceManager.getStringValue("other_driver_id");
        String token = preferenceManager.getStringValue("other_driver_token");

        String url = APIClient.baseUrl + "other_driver_todays_earnings";


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
                            View headerView = binding.navViewDriver.getHeaderView(0);

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



    private void updateVerificationStatus(String statusText) {
        //Show the not verified status here
        binding.verificationStatus.setVisibility(View.VISIBLE);
        binding.dutySwitch.setVisibility(View.GONE);
        binding.verificationStatus.setText(statusText);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
        stopHomeLocationUpdates();
    }
}