package com.kapstranspvtltd.kaps_partner.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OrderModel {
    private final String customerName;
    private final String bookingDate;
    private final String totalPrice;
    private final String customerImage; // Optional, can be null

    public OrderModel(String customerName, String bookingDate, String totalPrice) {
        this(customerName, bookingDate, totalPrice, null);
    }

    public OrderModel(String customerName, String bookingDate, String totalPrice, String customerImage) {
        this.customerName = customerName;
        this.bookingDate = bookingDate;
        this.totalPrice = totalPrice;
        this.customerImage = customerImage;
    }

    // Getters
    public String getCustomerName() {
        return customerName;
    }

    public String getBookingDate() {
        return bookingDate;
    }

    public String getTotalPrice() {
        return totalPrice;
    }

    public String getCustomerImage() {
        return customerImage;
    }

    public String getFormattedPrice() {
        try {
            double price = Double.parseDouble(totalPrice);
            return "₹" + Math.round(price) + " /-";
        } catch (NumberFormatException e) {
            return "₹0 /-";
        }
    }

    public String getFormattedDate() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            Date date = inputFormat.parse(bookingDate);
            return date != null ? outputFormat.format(date) : bookingDate;
        } catch (Exception e) {
            return bookingDate;
        }
    }

    public String getDayFromDate() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
            Date date = inputFormat.parse(bookingDate);
            return date != null ? outputFormat.format(date) : "";
        } catch (Exception e) {
            return "";
        }
    }
}