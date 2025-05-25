package com.kapstranspvtltd.kaps_partner.common_activities.models;

public class WalletTransaction {
    private String transactionId;
    private String type;
    private double amount;
    private String status;
    private String date;
    private String remarks;
    private String paymentMode;

    public String getRazorPayID() {
        return razorPayID;
    }

    public void setRazorPayID(String razorPayID) {
        this.razorPayID = razorPayID;
    }

    private String razorPayID;

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public WalletTransaction(String transactionId, String type, double amount,
                             String status, String date, String remarks, String paymentMode,String razorpayId) {
        this.transactionId = transactionId;
        this.type = type;
        this.amount = amount;
        this.status = status;
        this.date = date;
        this.remarks = remarks;
        this.paymentMode = paymentMode;
        this.razorPayID = razorpayId;
    }

    // Add getters
}