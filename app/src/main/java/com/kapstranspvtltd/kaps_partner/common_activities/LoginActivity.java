package com.kapstranspvtltd.kaps_partner.common_activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.kapstranspvtltd.kaps_partner.models.AppContent;
import com.kapstranspvtltd.kaps_partner.models.CountryCodeItem;
import com.kapstranspvtltd.kaps_partner.utils.AppContentManager;
import com.kapstranspvtltd.kaps_partner.utils.CustPrograssbar;
import com.kapstranspvtltd.kaps_partner.utils.Utility;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityLoginBinding;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private List<CountryCodeItem> cCodes = new ArrayList<>();
    private String codeSelect;

    private CustPrograssbar custPrograssbar;
    private static final int DEFAULT_INDIA_POSITION = 0; // Will be updated when we get codes

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Load dynamic content
        loadDynamicContent();

        setupClickListeners();
        setupLocationServices();
        getCountryCodes();
        setupMobileValidation();
        updateContinueButtonAppearance(false);
    }

    private void loadDynamicContent() {
        AppContent loginContent = AppContentManager.getInstance(this)
                .getFirstContentForScreen("login");

        if (loginContent != null) {
            // Load login screen content
            ImageView loginImage = binding.loginImage;
            TextView loginDescription = binding.loginDescription;

            // Set description
            if (loginDescription != null && !loginContent.getDescription().equals("NA")) {
                loginDescription.setText(loginContent.getDescription());
            }

            // Set image
            if (loginImage != null && !loginContent.getImageUrl().equals("NA")) {
                if (loginContent.getImageUrl().startsWith("http")) {
                    com.bumptech.glide.Glide.with(this)
                            .load(loginContent.getImageUrl())
                            .placeholder(R.drawable.logo)
                            .error(R.drawable.logo)
                            .into(loginImage);
                } else {
                    // Handle local drawable resources
                    try {
                        int resourceId = getResources().getIdentifier(
                                loginContent.getImageUrl().replace("@drawable/", ""),
                                "drawable",
                                getPackageName()
                        );
                        if (resourceId != 0) {
                            loginImage.setImageResource(resourceId);
                        }
                    } catch (Exception e) {
                        // Fallback to default image
                        loginImage.setImageResource(R.drawable.logo);
                    }
                }
            }

            System.out.println("Login content loaded: " + loginContent.getTitle());
        }
    }

    private void setupLocationServices() {
        requestPermissions(new String[]{Manifest.permission.CALL_PHONE, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 101);
        final LocationManager manager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && Utility.hasGPSDevice(this)) {
            Toast.makeText(this, "Gps not enabled", Toast.LENGTH_SHORT).show();
            Utility.enableLoc(this);
        }
    }

    private void setupMobileValidation() {
        binding.edMobile.addTextChangedListener(new TextWatcher() {
            private boolean isProcessing = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Skip if we're already processing to avoid recursion
                if (isProcessing) return;

                // Handle pasted text
                if (count > 1) {
                    isProcessing = true;
                    String input = s.toString();

                    // Remove common country code prefixes
                    input = input.replaceAll("^\\+?91|^0091|^91", "");

                    // Remove any non-digit characters
                    input = input.replaceAll("[^0-9]", "");

                    // Take last 10 digits if longer
                    if (input.length() > 10) {
                        input = input.substring(input.length() - 10);
                    }

                    // Update text field
                    binding.edMobile.setText(input);
                    binding.edMobile.setSelection(input.length());

                    isProcessing = false;
                    return;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isProcessing) return;

                isProcessing = true;

                // Handle single character input
                String filtered = s.toString().replaceAll("[^0-9]", "");

                if (filtered.length() > 10) {
                    filtered = filtered.substring(0, 10);
                }

                if (!filtered.equals(s.toString())) {
                    binding.edMobile.setText(filtered);
                    binding.edMobile.setSelection(filtered.length());
                }

                isProcessing = false;
            }
        });
    }

    private void getCountryCodes() {
        cCodes.clear();


        cCodes.add(new CountryCodeItem("IN", "+91", "India", R.drawable.ic_flag_india));

        // Create adapter for spinner
        CountryCodeAdapter adapter = new CountryCodeAdapter(this, cCodes);
        binding.spinner.setAdapter(adapter);

        // Set India as default
        binding.spinner.setSelection(DEFAULT_INDIA_POSITION);
    }

    private void setupClickListeners() {
        binding.imgBack.setOnClickListener(v -> finish());

        // Terms and Conditions click listener
        binding.txtTermsConditions.setOnClickListener(v -> {
            openUrl("https://www.kaps9.in/terms&conditions");
        });



        // Checkbox listener
        binding.checkboxTerms.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.txtContinue.setEnabled(isChecked);
            updateContinueButtonAppearance(isChecked);
        });

        binding.txtContinue.setOnClickListener(v -> {
            if (!binding.checkboxTerms.isChecked()) {
                Toast.makeText(this, "Please accept the terms and conditions", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isValidMobileNumber(binding.edMobile.getText().toString())) {
                String mobileNumber = binding.edMobile.getText().toString();
                String countryCode = codeSelect; // Get selected country code

                // Create intent for OTP screen
                Intent intent = new Intent(LoginActivity.this, OtpVerificationActivity.class);
                intent.putExtra("mobile", mobileNumber);
                intent.putExtra("countryCode", countryCode);
                startActivity(intent);
            }
        });
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateContinueButtonAppearance(boolean isEnabled) {
        if (isEnabled) {
            binding.txtContinue.setBackground(getResources().getDrawable(R.drawable.rounded_button));
            binding.txtContinue.setTextColor(getResources().getColor(R.color.white));
        } else {
            binding.txtContinue.setBackground(getResources().getDrawable(R.drawable.rounded_button_disabled));
            binding.txtContinue.setTextColor(getResources().getColor(R.color.white));
        }
    }


    private boolean isValidMobileNumber(String mobile) {
        if (TextUtils.isEmpty(mobile)) {
            binding.edMobile.setError("Please enter mobile number");
            return false;
        }

        if (mobile.length() != 10) {
            binding.edMobile.setError("Mobile number must be 10 digits");
            return false;
        }

        if (!mobile.matches("^[0-9]*$")) {
            binding.edMobile.setError("Invalid mobile number");
            return false;
        }

        return true;
    }

    // Create CountryCodeAdapter class
    private class CountryCodeAdapter extends ArrayAdapter<CountryCodeItem> {
        public CountryCodeAdapter(Context context, List<CountryCodeItem> codes) {
            super(context, R.layout.item_country_code, codes);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(getContext())
                        .inflate(R.layout.item_country_code, parent, false);
            }

            CountryCodeItem item = getItem(position);
            if (item != null) {
                ImageView flagImage = view.findViewById(R.id.flag_image);
                TextView codeText = view.findViewById(R.id.code_text);

                flagImage.setImageResource(item.getFlagResource());
                codeText.setText(item.getCode());
            }

            return view;
        }

        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return getView(position, convertView, parent);
        }
    }

}