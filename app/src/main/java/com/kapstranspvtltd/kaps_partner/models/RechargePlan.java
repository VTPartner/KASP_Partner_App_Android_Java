package com.kapstranspvtltd.kaps_partner.models;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RechargePlan {
    private String planId;
    private String title;
    private String description;
    private String validityDays;
    private String expiryDays;
    private double price;

    // Empty constructor
    public RechargePlan() {
    }

    // Constructor with all fields
    public RechargePlan(String planId, String title, String description, String validityDays, String expiryDays, double price) {
        this.planId = planId;
        this.title = title;
        this.description = description;
        this.validityDays = validityDays;
        this.expiryDays = expiryDays;
        this.price = price;
    }

    // Getters
    public String getPlanId() {
        return planId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getValidityDays() {
        return validityDays;
    }

    public String getExpiryDays() {
        return expiryDays;
    }

    public double getPrice() {
        return price;
    }

    // Setters
    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setValidityDays(String validityDays) {
        this.validityDays = validityDays;
    }

    public void setExpiryDays(String expiryDays) {
        this.expiryDays = expiryDays;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    // fromJson method with proper error handling
    public static RechargePlan fromJson(JSONObject json) throws JSONException {
        if (json == null) {
            throw new JSONException("JSON object is null");
        }

        try {
            RechargePlan plan = new RechargePlan();
            
            // Get values with default fallbacks
            plan.setPlanId(json.optString("recharge_plan_id", ""));
            plan.setTitle(json.optString("plan_title", ""));
            plan.setDescription(json.optString("plan_description", ""));
            plan.setValidityDays(json.optString("plan_days", ""));
            plan.setExpiryDays(json.optString("expiry_days", ""));
            plan.setPrice(json.optDouble("plan_price", 0.0));

            return plan;
        } catch (Exception e) {
            throw new JSONException("Error parsing RechargePlan: " + e.getMessage());
        }
    }

    // And here's how to parse the JSON array safely:
    public static List<RechargePlan> fromJsonArray(JSONArray jsonArray) {
        List<RechargePlan> plansList = new ArrayList<>();
        
        if (jsonArray == null) {
            return plansList;
        }

        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject planJson = jsonArray.optJSONObject(i);
                if (planJson != null) {
                    try {
                        RechargePlan plan = RechargePlan.fromJson(planJson);
                        plansList.add(plan);
                    } catch (JSONException e) {
                        Log.e("RechargePlan", "Error parsing plan at index " + i + ": " + e.getMessage());
                        // Continue parsing other plans even if one fails
                    }
                }
            }
        } catch (Exception e) {
            Log.e("RechargePlan", "Error parsing plans array: " + e.getMessage());
        }

        return plansList;
    }

    // toString method for debugging
    @Override
    public String toString() {
        return "RechargePlan{" +
                "planId='" + planId + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", validityDays=" + validityDays +
                ", expiryDays=" + expiryDays +
                ", price=" + price +
                '}';
    }
}