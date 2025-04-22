package com.kapstranspvtltd.kaps_partner.utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

public class PreferenceManager {
    private static final String PREF_NAME = "KAPSPartnerPrefs";
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // String preferences
    public void saveStringValue(String key, String value) {
        editor.putString(key, value);
        editor.apply();
    }

    public String getStringValue(String key) {
        return sharedPreferences.getString(key, "");
    }

    public String getStringValue(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    // Integer preferences
    public void saveIntValue(String key, int value) {
        editor.putInt(key, value);
        editor.apply();
    }



    public int getIntValue(String key) {
        return sharedPreferences.getInt(key, 0);
    }

    public int getIntValue(String key, int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }

    // Boolean preferences
    public void saveBooleanValue(String key, boolean value) {
        editor.putBoolean(key, value);
        editor.apply();
    }

    public boolean getBooleanValue(String key) {
        return sharedPreferences.getBoolean(key, false);
    }

    public boolean getBooleanValue(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    // Long preferences
    public void saveLongValue(String key, long value) {
        editor.putLong(key, value);
        editor.apply();
    }

    public long getLongValue(String key) {
        return sharedPreferences.getLong(key, 0L);
    }

    public long getLongValue(String key, long defaultValue) {
        return sharedPreferences.getLong(key, defaultValue);
    }



    // Float preferences
    public void saveFloatValue(String key, float value) {
        editor.putFloat(key, value);
        editor.apply();
    }

    public float getFloatValue(String key) {
        return sharedPreferences.getFloat(key, 0.0f);
    }

    public float getFloatValue(String key, float defaultValue) {
        return sharedPreferences.getFloat(key, defaultValue);
    }

    // Remove specific preference
    public void removeValue(String key) {
        editor.remove(key);
        editor.apply();
    }

    // Clear all preferences
    public void clearPreferences() {
        editor.clear();
        editor.apply();
    }

    // Check if key exists
    public boolean containsKey(String key) {
        return sharedPreferences.contains(key);
    }

    // Constants for keys
    public static final class Keys {
        public static final String CUSTOMER_ID = "customer_id";
        public static final String CUSTOMER_NAME = "customer_name";
        public static final String PROFILE_PIC = "profile_pic";
        public static final String CUSTOMER_MOBILE = "customer_mobile_no";
        public static final String FULL_ADDRESS = "full_address";
        public static final String EMAIL = "email";
        public static final String GST_NO = "gst_no";
        public static final String GST_ADDRESS = "gst_address";
        public static final String IS_LOGGED_IN = "is_logged_in";

        public static final String SERVICE_START_TIME = "service_start_time";
        public static final String SERVICE_IS_RUNNING = "service_is_running";
        public static final String SERVICE_ELAPSED_TIME = "service_elapsed_time";
        public static final String SERVICE_LAST_PAUSE_TIME = "service_last_pause_time";
    }

    // Helper methods for common operations
    public void saveUserLoginStatus(boolean isLoggedIn) {
        saveBooleanValue(Keys.IS_LOGGED_IN, isLoggedIn);
    }

    public boolean isUserLoggedIn() {
        return getBooleanValue(Keys.IS_LOGGED_IN, false);
    }

    public void saveUserDetails(JSONObject user) {
        try {
            saveStringValue(Keys.CUSTOMER_ID, user.optString("customer_id"));
            saveStringValue(Keys.CUSTOMER_NAME, user.optString("customer_name"));
            saveStringValue(Keys.PROFILE_PIC, user.optString("profile_pic"));
            saveStringValue(Keys.CUSTOMER_MOBILE, user.optString("customer_mobile_no"));
            saveStringValue(Keys.FULL_ADDRESS, user.optString("full_address"));
            saveStringValue(Keys.EMAIL, user.optString("email"));
            saveStringValue(Keys.GST_NO, user.optString("gst_no"));
            saveStringValue(Keys.GST_ADDRESS, user.optString("gst_address"));
            saveUserLoginStatus(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clearUserDetails() {
        removeValue(Keys.CUSTOMER_ID);
        removeValue(Keys.CUSTOMER_NAME);
        removeValue(Keys.PROFILE_PIC);
        removeValue(Keys.CUSTOMER_MOBILE);
        removeValue(Keys.FULL_ADDRESS);
        removeValue(Keys.EMAIL);
        removeValue(Keys.GST_NO);
        removeValue(Keys.GST_ADDRESS);
        saveUserLoginStatus(false);
    }

    public String getUserId() {
        return getStringValue(Keys.CUSTOMER_ID);
    }

    public String getUserName() {
        return getStringValue(Keys.CUSTOMER_NAME);
    }

    public String getUserMobile() {
        return getStringValue(Keys.CUSTOMER_MOBILE);
    }

    public String getUserEmail() {
        return getStringValue(Keys.EMAIL);
    }
}