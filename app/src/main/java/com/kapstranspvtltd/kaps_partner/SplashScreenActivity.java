package com.kapstranspvtltd.kaps_partner;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;

import com.kapstranspvtltd.kaps_partner.cab_driver_activities.CabDriverHomeActivity;
import com.kapstranspvtltd.kaps_partner.cab_driver_activities.CabLoginActivity;
import com.kapstranspvtltd.kaps_partner.common_activities.DriverTypeActivity;
import com.kapstranspvtltd.kaps_partner.common_activities.IntroActivity;
import com.kapstranspvtltd.kaps_partner.common_activities.LoginActivity;
import com.kapstranspvtltd.kaps_partner.common_activities.PermissionsActivity;
import com.kapstranspvtltd.kaps_partner.driver_app_activities.DriverAgentHomeActivity;
import com.kapstranspvtltd.kaps_partner.driver_app_activities.DriverAgentLoginScreenActivity;
import com.kapstranspvtltd.kaps_partner.goods_driver_activities.HomeActivity;
import com.kapstranspvtltd.kaps_partner.handyman_agent_activities.HandyManAgentHomeActivity;
import com.kapstranspvtltd.kaps_partner.handyman_agent_activities.HandyManLoginScreenActivity;
import com.kapstranspvtltd.kaps_partner.jcb_crane_agent_activities.JcbCraneAgentLoginScreenActivity;
import com.kapstranspvtltd.kaps_partner.jcb_crane_agent_activities.JcbCraneHomeActivity;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;
import com.kapstranspvtltd.kaps_partner.R;


public class SplashScreenActivity extends AppCompatActivity {

    PreferenceManager preferenceManager;

    private enum DriverType {
        GOODS_DRIVER,
        CAB_DRIVER,
        JCB_PROVIDER,
        ONLY_DRIVER,
        HANDYMAN
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        preferenceManager = new PreferenceManager(this);

//        String goodsDriverID = preferenceManager.getStringValue("goods_driver_id");
//        String goodsDriverName = preferenceManager.getStringValue("goods_driver_name");
        Boolean firstRun = preferenceManager.getBooleanValue("firstRun");

        new Handler().postDelayed(() -> {
            // This method will be executed once the timer is over
            // Start your app main activity
            if (preferenceManager != null && firstRun) {

//                if (goodsDriverID != null && goodsDriverID.isEmpty() == false && goodsDriverName != null && goodsDriverName.isEmpty() == false) {
//                    startActivity(new Intent(SplashScreenActivity.this, HomeActivity.class));
//                    finish();
                // Check permissions after a short delay
                new Handler().postDelayed(() -> checkPermissionsAndProceed(), 1000); // 1 seconds delay
//                } else {
//                    startActivity(new Intent(SplashScreenActivity.this, DriverTypeActivity.class));
//                    finish();
//                }
            } else {
                Intent i = new Intent(SplashScreenActivity.this, IntroActivity.class);
                startActivity(i);
                finish();
            }


        }, 3000);
    }
/*
    private void checkPermissionsAndProceed() {
        if (areAllPermissionsGranted()) {
            // All permissions granted, go to MainActivity
            String roleType = preferenceManager.getStringValue("role_type");
            String goodsDriverID = preferenceManager.getStringValue("goods_driver_id");
            String goodsDriverName = preferenceManager.getStringValue("goods_driver_name");
            String cabDriverID = preferenceManager.getStringValue("cab_driver_id");
            String cabDriverName = preferenceManager.getStringValue("cab_agent_driver_name");
            System.out.println("splashRoleType::"+roleType);
           if (roleType.isEmpty() == false && roleType.equals("GOODS_DRIVER")) {
                if (goodsDriverID != null && goodsDriverID.isEmpty() == false && goodsDriverName != null && goodsDriverName.isEmpty() == false && goodsDriverName.equals("NA")==false) {
                    startActivity(new Intent(this, HomeActivity.class));
                } else {
                    startActivity(new Intent(this, LoginActivity.class));
                }
            }else if (roleType.isEmpty() == false && roleType.equals("CAB_DRIVER")) {
                if (cabDriverID != null && cabDriverID.isEmpty() == false && cabDriverName != null && cabDriverName.isEmpty() == false && cabDriverName.equalsIgnoreCase("NA")==false) {
                    startActivity(new Intent(this, CabDriverHomeActivity.class));
                } else {
                    startActivity(new Intent(this, CabLoginActivity.class));
                }
            }else{
                startActivity(new Intent(this, DriverTypeActivity.class));
            }
        } else {
            // Some permissions missing, go to PermissionsActivity
            startActivity(new Intent(this, PermissionsActivity.class));
        }
        finish();
    }

 */

    private void checkPermissionsAndProceed() {
        if (areAllPermissionsGranted()) {
            // All permissions granted, check user type and redirect
            String roleType = preferenceManager.getStringValue("role_type");
            redirectBasedOnUserType(roleType);
        } else {
            // Some permissions missing, go to PermissionsActivity
            startActivity(new Intent(this, PermissionsActivity.class));
        }
        finish();
    }

    private void redirectBasedOnUserType(String roleType) {
        try {
            DriverType driverType = getDriverTypeFromRole(roleType);

            switch (driverType) {
                case GOODS_DRIVER:
                    handleGoodsDriver();
                    break;
                case CAB_DRIVER:
                    handleCabDriver();
                    break;
                case JCB_PROVIDER:
                    handleJcbProvider();
                    break;
                case ONLY_DRIVER:
                    handleOnlyDriver();
                    break;
                case HANDYMAN:
                    handleHandyman();
                    break;
                default:
                    startActivity(new Intent(this, DriverTypeActivity.class));
                    break;
            }
        } catch (IllegalArgumentException e) {
            // If role_type is empty or invalid, go to DriverTypeActivity
            startActivity(new Intent(this, DriverTypeActivity.class));
        }
    }

    private DriverType getDriverTypeFromRole(String roleType) {
        if (roleType == null || roleType.isEmpty()) {
            throw new IllegalArgumentException("Invalid role type");
        }

        switch (roleType.toUpperCase()) {
            case "GOODS_DRIVER":
                return DriverType.GOODS_DRIVER;
            case "CAB_DRIVER":
                return DriverType.CAB_DRIVER;
            case "JCB_PROVIDER":
                return DriverType.JCB_PROVIDER;
            case "ONLY_DRIVER":
                return DriverType.ONLY_DRIVER;
            case "HANDYMAN":
                return DriverType.HANDYMAN;
            default:
                throw new IllegalArgumentException("Unknown role type");
        }
    }

    private void handleGoodsDriver() {
        String driverId = preferenceManager.getStringValue("goods_driver_id");
        String driverName = preferenceManager.getStringValue("goods_driver_name");

        if (isValidCredentials(driverId, driverName)) {
            startActivity(new Intent(this, HomeActivity.class));
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
    }

    private void handleCabDriver() {
        String driverId = preferenceManager.getStringValue("cab_driver_id");
        String driverName = preferenceManager.getStringValue("cab_agent_driver_name");

        if (isValidCredentials(driverId, driverName)) {
            startActivity(new Intent(this, CabDriverHomeActivity.class));
        } else {
            startActivity(new Intent(this, CabLoginActivity.class));
        }
    }

    private void handleJcbProvider() {
        String driverId = preferenceManager.getStringValue("jcb_crane_agent_id");
        String driverName = preferenceManager.getStringValue("jcb_crane_agent_name");

        if (isValidCredentials(driverId, driverName)) {
            startActivity(new Intent(this, JcbCraneHomeActivity.class));
        } else {
            startActivity(new Intent(this, JcbCraneAgentLoginScreenActivity.class));
        }
    }

    private void handleOnlyDriver() {
        String driverId = preferenceManager.getStringValue("other_driver_id");
        String driverName = preferenceManager.getStringValue("other_driver_name");

        if (isValidCredentials(driverId, driverName)) {
            startActivity(new Intent(this, DriverAgentHomeActivity.class));
        } else {
            startActivity(new Intent(this, DriverAgentLoginScreenActivity.class));
        }
    }

    private void handleHandyman() {
        String driverId = preferenceManager.getStringValue("handyman_agent_id");
        String driverName = preferenceManager.getStringValue("handyman_agent_name");

        if (isValidCredentials(driverId, driverName)) {
            startActivity(new Intent(this, HandyManAgentHomeActivity.class));
        } else {
            startActivity(new Intent(this, HandyManLoginScreenActivity.class));
        }
    }

    private boolean isValidCredentials(String id, String name) {
        return id != null && !id.isEmpty() &&
                name != null && !name.isEmpty() &&
                !name.equalsIgnoreCase("NA");
    }

    private boolean areAllPermissionsGranted() {
        return checkLocationPermissions() &&
                isIgnoringBatteryOptimizations() &&
                Settings.canDrawOverlays(this) &&
                checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                checkNotificationPermission();
    }

    private boolean checkLocationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED ;
        }
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isIgnoringBatteryOptimizations() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    private boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Always return true for Android < 13
    }
}