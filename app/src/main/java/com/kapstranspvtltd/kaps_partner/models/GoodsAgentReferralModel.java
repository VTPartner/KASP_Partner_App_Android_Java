package com.kapstranspvtltd.kaps_partner.models;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GoodsAgentReferralModel {
    private String driverName;
    private String usedAt;
    private String status;
    private double amount;
    private int position;

    public GoodsAgentReferralModel() {
    }

    public GoodsAgentReferralModel(String driverName, String usedAt, String status, double amount, int position) {
        this.driverName = driverName;
        this.usedAt = usedAt;
        this.status = status;
        this.amount = amount;
        this.position = position;
    }

    // Getters and Setters
    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(String usedAt) {
        this.usedAt = usedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    // Helper methods
    public String getFormattedDate() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            
            Date date = inputFormat.parse(usedAt);
            return outputFormat.format(date);
        } catch (ParseException e) {
            // If parsing fails, return original string
            return usedAt;
        }
    }

    public String getRelativeTime() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(usedAt);
            
            long timeDiff = System.currentTimeMillis() - date.getTime();
            long daysDiff = timeDiff / (24 * 60 * 60 * 1000);
            
            if (daysDiff == 0) {
                return "Today";
            } else if (daysDiff == 1) {
                return "Yesterday";
            } else if (daysDiff < 7) {
                return daysDiff + " days ago";
            } else if (daysDiff < 30) {
                long weeksDiff = daysDiff / 7;
                return weeksDiff + " week" + (weeksDiff > 1 ? "s" : "") + " ago";
            } else if (daysDiff < 365) {
                long monthsDiff = daysDiff / 30;
                return monthsDiff + " month" + (monthsDiff > 1 ? "s" : "") + " ago";
            } else {
                long yearsDiff = daysDiff / 365;
                return yearsDiff + " year" + (yearsDiff > 1 ? "s" : "") + " ago";
            }
        } catch (ParseException e) {
            return "Recently";
        }
    }

    public String getFormattedAmount() {
        return "₹" + String.format("%.0f", amount);
    }

    public boolean isCompleted() {
        return "Completed".equalsIgnoreCase(status);
    }

    public boolean isPending() {
        return "Pending".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return "GoodsAgentReferralModel{" +
                "driverName='" + driverName + '\'' +
                ", usedAt='" + usedAt + '\'' +
                ", status='" + status + '\'' +
                ", amount=" + amount +
                ", position=" + position +
                '}';
    }
} 