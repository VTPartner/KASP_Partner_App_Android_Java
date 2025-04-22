package com.kapstranspvtltd.kaps_partner.models;

import org.json.JSONException;
import org.json.JSONObject;

public class RechargeHistory {
    private String rechargeHistoryId;
    private String rechargePlanId;
    private String planExpiryTime;
    private String planTitle;
    private String planDescription;
    private String planDays;
    private String expiryDays;
    private double planPrice;

    private String purchaseTime;

    // Constructor
    public RechargeHistory(JSONObject json) throws JSONException {
        this.rechargeHistoryId = json.getString("recharge_history_id");
        this.rechargePlanId = json.getString("recharge_plan_id");
        this.planExpiryTime = json.getString("plan_expiry_time");
        this.planTitle = json.getString("plan_title");
        this.planDescription = json.getString("plan_description");
        this.planDays = json.getString("plan_days");
        this.expiryDays = json.getString("expiry_days");
        this.planPrice = json.getDouble("plan_price");
        this.purchaseTime = json.getString("recharge_time");
    }

    // Getters
    public String getRechargeHistoryId() { return rechargeHistoryId; }
    public String getRechargePlanId() { return rechargePlanId; }
    public String getPlanExpiryTime() { return planExpiryTime; }
    public String getPlanTitle() { return planTitle; }
    public String getPlanDescription() { return planDescription; }
    public String getPlanDays() { return planDays; }
    public String getExpiryDays() { return expiryDays; }

    public String getPurchasedDateTime() { return purchaseTime; }
    public double getPlanPrice() { return planPrice; }
}