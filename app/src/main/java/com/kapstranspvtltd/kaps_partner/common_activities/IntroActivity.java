package com.kapstranspvtltd.kaps_partner.common_activities;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.kapstranspvtltd.kaps_partner.R;
import com.kapstranspvtltd.kaps_partner.goods_driver_activities.fragments.Info1Fragment;
import com.kapstranspvtltd.kaps_partner.goods_driver_activities.fragments.Info2Fragment;
import com.kapstranspvtltd.kaps_partner.goods_driver_activities.fragments.Info3Fragment;
import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;
import com.kapstranspvtltd.kaps_partner.databinding.ActivityIntroBinding;

import java.util.Locale;

public class IntroActivity extends AppCompatActivity {
    private ActivityIntroBinding binding;
    public static ViewPager vpPager;
    private MyPagerAdapter adapterViewPager;

    PreferenceManager preferenceManager;

    private BottomSheetDialog languageBottomSheet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityIntroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize ViewPager
        preferenceManager = new PreferenceManager(this);
        vpPager = binding.vpPager;
        adapterViewPager = new MyPagerAdapter(getSupportFragmentManager());


        // Setup ViewPager and DotsIndicator
        vpPager.setAdapter(adapterViewPager);
        binding.dotsIndicator.setViewPager(vpPager);

        // Setup click listener
        binding.lvlPhone.setOnClickListener(v -> {
            preferenceManager.saveBooleanValue("firstRun", true);
            startActivity(new Intent(IntroActivity.this, PermissionsActivity.class));
        });

        // Setup page change listener
        vpPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset,
                                       int positionOffsetPixels) {
                Log.e("data", "jsadlj");
            }

            @Override
            public void onPageSelected(int position) {
                // Handle page selection if needed
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                Log.e("sjlkj", "sjahdal");
            }
        });

        binding.languageButton.setOnClickListener(v -> showLanguageBottomSheet());
    }

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

    public class MyPagerAdapter extends FragmentPagerAdapter {
        private static final int NUM_ITEMS = 3;

        public MyPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return Info1Fragment.newInstance();
                case 1:
                    return Info2Fragment.newInstance();
                case 2:
                    return Info3Fragment.newInstance();
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Log.e("page", String.valueOf(position));
            return "Page " + position;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            return super.instantiateItem(container, position);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}