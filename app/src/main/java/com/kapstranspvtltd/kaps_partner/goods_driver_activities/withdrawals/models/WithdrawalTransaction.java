package com.kapstranspvtltd.kaps_partner.goods_driver_activities.withdrawals.models;

import org.json.JSONException;
import org.json.JSONObject;

public class WithdrawalTransaction {
    private long withdrawalId;
    private double amount;
    private String paymentMethod;
    private String accountNumber;
    private String ifscCode;
    private String accountName;
    private String upiId;
    private String status;
    private String razorpayPayoutId;
    private long createdAt;
    private Long completedAt;
    private String remarks;
    private JSONObject paymentDetails;

    public WithdrawalTransaction(long withdrawalId, double amount, String paymentMethod,
                                 String accountNumber, String ifscCode, String accountName,
                                 String upiId, String status, String razorpayPayoutId,
                                 long createdAt, Long completedAt, String remarks,
                                 JSONObject paymentDetails) {
        this.withdrawalId = withdrawalId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.accountNumber = accountNumber;
        this.ifscCode = ifscCode;
        this.accountName = accountName;
        this.upiId = upiId;
        this.status = status;
        this.razorpayPayoutId = razorpayPayoutId;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.remarks = remarks;
        this.paymentDetails = paymentDetails;
    }

    // Add getters
    public long getWithdrawalId() { return withdrawalId; }
    public double getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getAccountNumber() { return accountNumber; }
    public String getIfscCode() { return ifscCode; }
    public String getAccountName() { return accountName; }
    public String getUpiId() { return upiId; }
    public String getStatus() { return status; }
    public String getRazorpayPayoutId() { return razorpayPayoutId; }
    public long getCreatedAt() { return createdAt; }
    public Long getCompletedAt() { return completedAt; }
    public String getRemarks() { return remarks; }
    public JSONObject getPaymentDetails() { return paymentDetails; }

    public static WithdrawalTransaction fromJson(JSONObject json) throws JSONException {
        return new WithdrawalTransaction(
                json.getLong("withdrawal_id"),
                json.getDouble("amount"),
                json.getString("payment_method"),
                json.optString("account_number", ""),
                json.optString("ifsc_code", ""),
                json.optString("account_name", ""),
                json.optString("upi_id", ""),
                json.getString("status"),
                json.optString("razorpay_payout_id", ""),
                json.getLong("created_at"),
                json.has("completed_at") && !json.isNull("completed_at") ?
                        json.getLong("completed_at") : null,
                json.getString("remarks"),
                json.optJSONObject("payment_details")
        );
    }
}