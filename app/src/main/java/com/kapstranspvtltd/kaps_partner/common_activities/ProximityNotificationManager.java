package com.kapstranspvtltd.kaps_partner.common_activities;

import android.content.Context;

import com.kapstranspvtltd.kaps_partner.utils.PreferenceManager;

public class ProximityNotificationManager {
    private static final String PREF_PREFIX = "proximity_notification_";
    private final PreferenceManager preferenceManager;
    
    public ProximityNotificationManager(Context context) {
        this.preferenceManager = new PreferenceManager(context);
    }
    
    public boolean hasNotificationBeenSent(String bookingId, String type, int dropIndex) {
        String key = PREF_PREFIX + bookingId + "_" + type + "_" + dropIndex;
        return preferenceManager.getBooleanValue(key, false);
    }
    
    public void markNotificationSent(String bookingId, String type, int dropIndex) {
        String key = PREF_PREFIX + bookingId + "_" + type + "_" + dropIndex;
        preferenceManager.saveBooleanValue(key, true);
    }
    
    public void clearNotifications(String bookingId) {
        // Clear pickup notification
        String pickupKey = PREF_PREFIX + bookingId + "_pickup_0";
        preferenceManager.removeValue(pickupKey);
        
        // Clear all drop notifications (assume max 10 drops)
        for (int i = 0; i < 10; i++) {
            String dropKey = PREF_PREFIX + bookingId + "_drop_" + i;
            preferenceManager.removeValue(dropKey);
        }
    }
}