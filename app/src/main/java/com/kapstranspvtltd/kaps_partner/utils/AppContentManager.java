package com.kapstranspvtltd.kaps_partner.utils;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bumptech.glide.Glide;
import com.kapstranspvtltd.kaps_partner.models.AppContent;
import com.kapstranspvtltd.kaps_partner.network.APIClient;
import com.kapstranspvtltd.kaps_partner.network.VolleySingleton;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AppContentManager {
    private static final String TAG = "AppContentManager";
    private static AppContentManager instance;
    private PreferenceManager preferenceManager;
    private Map<String, List<AppContent>> contentCache = new HashMap<>();

    private AppContentManager(Context context) {
        preferenceManager = new PreferenceManager(context);
    }

    public static AppContentManager getInstance(Context context) {
        if (instance == null) {
            instance = new AppContentManager(context);
        }
        return instance;
    }

    public interface AppContentCallback {
        void onSuccess(Map<String, List<AppContent>> content);
        void onError(String error);
    }

    public void fetchAppContent(Context context, AppContentCallback callback) {
        // Check if we have cached content and it's recent (less than 1 hour old)
        long lastFetchTime = preferenceManager.getLongValue("app_content_last_fetch", 0);
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastFetchTime < 3600000) { // 1 hour in milliseconds
//                if (currentTime - lastFetchTime < 6000) { // 1 minute in milliseconds
            // Load from cache
            loadFromCache();
            if (!contentCache.isEmpty()) {
                callback.onSuccess(contentCache);
                return;
            }
        }

        // Fetch from server
        try {
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    APIClient.baseUrl + "get_app_content",
                    null,
                    response -> {
                        try {
                            parseAndCacheContent(response, context);
                            preferenceManager.saveLongValue("app_content_last_fetch", System.currentTimeMillis());
                            callback.onSuccess(contentCache);
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing app content: " + e.getMessage());
                            callback.onError("Error parsing content: " + e.getMessage());
                        }
                    },
                    error -> {
                        Log.e(TAG, "Error fetching app content: " + error.getMessage());
                        // Try to load from cache as fallback
                        loadFromCache();
                        if (!contentCache.isEmpty()) {
                            callback.onSuccess(contentCache);
                        } else {
                            callback.onError("Error fetching content: " + error.getMessage());
                        }
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

            VolleySingleton.getInstance(context).addToRequestQueue(request);

        } catch (Exception e) {
            Log.e(TAG, "Error creating request: " + e.getMessage());
            callback.onError("Error creating request: " + e.getMessage());
        }
    }

    private void parseAndCacheContent(JSONObject response, Context context) throws JSONException {
        contentCache.clear();
        
        if (response.has("content")) {
            JSONObject contentObj = response.getJSONObject("content");
            
            // Use Iterator to get all keys from JSONObject
            Iterator<String> keys = contentObj.keys();
            while (keys.hasNext()) {
                String screenName = keys.next();
                JSONArray contentArray = contentObj.getJSONArray(screenName);
                List<AppContent> contentList = new ArrayList<>();
                
                for (int i = 0; i < contentArray.length(); i++) {
                    JSONObject contentItem = contentArray.getJSONObject(i);
                    
                    AppContent content = new AppContent(
                            contentItem.getInt("content_id"),
                            screenName,
                            contentItem.getString("title"),
                            contentItem.getString("description"),
                            contentItem.getString("image_url"),
                            contentItem.getInt("sort_order"),
                            contentItem.getInt("status"),
                            contentItem.getDouble("time_at")
                    );
                    
                    contentList.add(content);
                    
                    // Preload image if it's a URL
                    if (!contentItem.getString("image_url").equals("NA") && 
                        contentItem.getString("image_url").startsWith("http")) {
                        preloadImage(context, contentItem.getString("image_url"));
                    }
                }
                
                contentCache.put(screenName, contentList);
            }
        }
        
        // Save to preferences
        saveToCache();
    }

    private void preloadImage(Context context, String imageUrl) {
        try {
            Glide.with(context)
                    .load(imageUrl)
                    .preload();
        } catch (Exception e) {
            Log.e(TAG, "Error preloading image: " + e.getMessage());
        }
    }

    private void saveToCache() {
        // Save content to preferences as JSON string
        try {
            JSONObject cacheObject = new JSONObject();
            for (Map.Entry<String, List<AppContent>> entry : contentCache.entrySet()) {
                JSONArray contentArray = new JSONArray();
                for (AppContent content : entry.getValue()) {
                    JSONObject contentObj = new JSONObject();
                    contentObj.put("content_id", content.getContentId());
                    contentObj.put("screen_name", content.getScreenName());
                    contentObj.put("title", content.getTitle());
                    contentObj.put("description", content.getDescription());
                    contentObj.put("image_url", content.getImageUrl());
                    contentObj.put("sort_order", content.getSortOrder());
                    contentObj.put("status", content.getStatus());
                    contentObj.put("time_at", content.getTimeAt());
                    contentArray.put(contentObj);
                }
                cacheObject.put(entry.getKey(), contentArray);
            }
            preferenceManager.saveStringValue("app_content_cache", cacheObject.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error saving to cache: " + e.getMessage());
        }
    }

    private void loadFromCache() {
        contentCache.clear();
        String cachedContent = preferenceManager.getStringValue("app_content_cache", "");
        
        if (!cachedContent.isEmpty()) {
            try {
                JSONObject cacheObject = new JSONObject(cachedContent);
                // Use Iterator to get all keys from JSONObject
                Iterator<String> keys = cacheObject.keys();
                while (keys.hasNext()) {
                    String screenName = keys.next();
                    JSONArray contentArray = cacheObject.getJSONArray(screenName);
                    List<AppContent> contentList = new ArrayList<>();
                    
                    for (int i = 0; i < contentArray.length(); i++) {
                        JSONObject contentItem = contentArray.getJSONObject(i);
                        
                        AppContent content = new AppContent(
                                contentItem.getInt("content_id"),
                                screenName,
                                contentItem.getString("title"),
                                contentItem.getString("description"),
                                contentItem.getString("image_url"),
                                contentItem.getInt("sort_order"),
                                contentItem.getInt("status"),
                                contentItem.getDouble("time_at")
                        );
                        
                        contentList.add(content);
                    }
                    
                    contentCache.put(screenName, contentList);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error loading from cache: " + e.getMessage());
            }
        }
    }

    public List<AppContent> getContentForScreen(String screenName) {
        return contentCache.getOrDefault(screenName, new ArrayList<>());
    }

    public AppContent getFirstContentForScreen(String screenName) {
        List<AppContent> contentList = getContentForScreen(screenName);
        return contentList.isEmpty() ? null : contentList.get(0);
    }

    public void clearCache() {
        contentCache.clear();
        preferenceManager.saveStringValue("app_content_cache", "");
        preferenceManager.saveLongValue("app_content_last_fetch", 0);
    }
} 