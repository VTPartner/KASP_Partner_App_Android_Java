package com.kapstranspvtltd.kaps_partner.common_activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.kapstranspvtltd.kaps_partner.cab_driver_activities.CabDriverHomeActivity;
import com.kapstranspvtltd.kaps_partner.cab_driver_activities.CabLoginActivity;
import com.kapstranspvtltd.kaps_partner.driver_app_activities.DriverAgentHomeActivity;
import com.kapstranspvtltd.kaps_partner.driver_app_activities.DriverAgentLoginScreenActivity;
import com.kapstranspvtltd.kaps_partner.goods_driver_activities.HomeActivity;
import com.kapstranspvtltd.kaps_partner.handyman_agent_activities.HandyManAgentHomeActivity;
import com.kapstranspvtltd.kaps_partner.handyman_agent_activities.HandyManLoginScreenActivity;
import com.kapstranspvtltd.kaps_partner.jcb_crane_agent_activities.JcbCraneAgentLoginScreenActivity;
import com.kapstranspvtltd.kaps_partner.jcb_crane_agent_activities.JcbCraneHomeActivity;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityDriverTypeBinding;

public class DriverTypeActivity extends AppCompatActivity {

    private long backPressedTime = 0;
    private ActivityDriverTypeBinding binding;
    private PreferenceManager preferenceManager;
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 1234;

    public enum DriverType {
        GOODS_DRIVER,
        CAB_DRIVER,
        JCB_PROVIDER,
        ONLY_DRIVER,
        HANDYMAN
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupFullScreen();
        binding = ActivityDriverTypeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(this);

        requestNotificationPermission();
        setupAnimations();
        setupClickListeners();
    }

    private void setupAnimations() {
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);

        binding.goodsDriverCard.startAnimation(fadeIn);
        binding.cabDriverCard.startAnimation(fadeIn);
        binding.jcbProviderCard.startAnimation(fadeIn);
        binding.onlyDriverCard.startAnimation(fadeIn);
        binding.handymanCard.startAnimation(fadeIn);
    }

    private void setupClickListeners() {
        binding.goodsDriverCard.setOnClickListener(v -> animateCardClick(v, DriverType.GOODS_DRIVER));
        binding.cabDriverCard.setOnClickListener(v -> animateCardClick(v, DriverType.CAB_DRIVER));
        binding.jcbProviderCard.setOnClickListener(v -> animateCardClick(v, DriverType.JCB_PROVIDER));
        binding.onlyDriverCard.setOnClickListener(v -> animateCardClick(v, DriverType.ONLY_DRIVER));
        binding.handymanCard.setOnClickListener(v -> animateCardClick(v, DriverType.HANDYMAN));
    }

    private void animateCardClick(View view, DriverType driverType) {
        view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start();
                    handleDriverTypeSelection(driverType);
                }).start();
    }

    private void handleDriverTypeSelection(DriverType driverType) {
        preferenceManager.saveStringValue("role_type",driverType.name());
        System.out.println("role_type::"+driverType.name());

        if (driverType == DriverType.GOODS_DRIVER) {
            String goodsDriverID = preferenceManager.getStringValue("goods_driver_id");
            String goodsDriverName = preferenceManager.getStringValue("goods_driver_name");

            if (goodsDriverID != null && !goodsDriverID.isEmpty() &&
                    goodsDriverName != null && !goodsDriverName.isEmpty() &&
                    !goodsDriverName.equals("NA")) {
                startActivity(new Intent(this, HomeActivity.class));
            } else {
                startActivity(new Intent(this, LoginActivity.class));
            }
        } else if (driverType == DriverType.CAB_DRIVER) {
            String cabDriverID = preferenceManager.getStringValue("cab_driver_id");
            String cabDriverName = preferenceManager.getStringValue("cab_agent_driver_name");

            if (cabDriverID != null && !cabDriverID.isEmpty() &&
                    cabDriverName != null && !cabDriverName.isEmpty() &&
                    !cabDriverName.equals("NA")) {
                startActivity(new Intent(this, CabDriverHomeActivity.class));
            } else {
                startActivity(new Intent(this, CabLoginActivity.class));
            }
        } else if (driverType == DriverType.JCB_PROVIDER) {
            String jcbCraneDriverID = preferenceManager.getStringValue("jcb_crane_agent_id");
            String jcbCraneDriverName = preferenceManager.getStringValue("jcb_crane_agent_name");

            if (jcbCraneDriverID != null && !jcbCraneDriverID.isEmpty() &&
                    jcbCraneDriverName != null && !jcbCraneDriverName.isEmpty() &&
                    !jcbCraneDriverName.equals("NA")) {
                startActivity(new Intent(this, JcbCraneHomeActivity.class));
            } else {
                startActivity(new Intent(this, JcbCraneAgentLoginScreenActivity.class));
            }
        }else if (driverType == DriverType.ONLY_DRIVER) {
            String otherDriverID = preferenceManager.getStringValue("other_driver_id");
            String otherDriverName = preferenceManager.getStringValue("other_driver_name");

            if (otherDriverID != null && !otherDriverID.isEmpty() &&
                    otherDriverName != null && !otherDriverName.isEmpty() &&
                    !otherDriverName.equals("NA")) {
                startActivity(new Intent(this, DriverAgentHomeActivity.class));
            } else {
                startActivity(new Intent(this, DriverAgentLoginScreenActivity.class));
            }
        }else if (driverType == DriverType.HANDYMAN) {
            String handymanID = preferenceManager.getStringValue("handyman_agent_id");
            String handymanName = preferenceManager.getStringValue("handyman_agent_name");

            if (handymanID != null && !handymanID.isEmpty() &&
                    handymanName != null && !handymanName.isEmpty() &&
                    !handymanName.equals("NA")) {
                startActivity(new Intent(this, HandyManAgentHomeActivity.class));
            } else {
                startActivity(new Intent(this, HandyManLoginScreenActivity.class));
            }
        }

        else {
            Toast.makeText(this,
                    "NO Such feature allowed",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        101
                );
            }
        }
    }

    private void setupFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
    }

    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed();
        } else {
            showSnackbar("Press back once again to exit");
            backPressedTime = System.currentTimeMillis();
        }
    }

    private void showSnackbar(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}