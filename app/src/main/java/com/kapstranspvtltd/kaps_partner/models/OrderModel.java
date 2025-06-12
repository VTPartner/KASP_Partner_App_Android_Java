package com.kapstranspvtltd.kaps_partner.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OrderModel {
    private final String customerName;
    private final String bookingDate;
    private final String totalPrice;
    private final String customerImage; // Optional, can be null



    private final Double bookingTiming;

    public OrderModel(String customerName, String bookingDate, String totalPrice) {
        this(customerName, bookingDate, totalPrice, null, null); // bookingTiming = null
    }

    public OrderModel(String customerName, String bookingDate, String totalPrice,Double bookingTiming) {
        this(customerName, bookingDate, totalPrice,null,bookingTiming);
    }

    public OrderModel(String customerName, String bookingDate, String totalPrice, String customerImage,Double bookingTiming) {
        this.customerName = customerName;
        this.bookingDate = bookingDate;
        this.totalPrice = totalPrice;
        this.customerImage = customerImage;
        this.bookingTiming = bookingTiming;
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
            return "+₹" + Math.round(price) + " /-";
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

    public String getFormattedBookingTime() {
        try {
            // Split the fractional part if present
            double epochDouble = bookingTiming;
            long epochMillis = (long) (epochDouble * 1000); // Convert to milliseconds

            Date date = new Date(epochMillis);
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault());
            return dateTimeFormat.format(date);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return "N/A";
        }
    }




    public String getDayFromDate() {
        try {
            // First try the full datetime format
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(bookingDate);

            // If parsing fails, try date-only format
            if (date == null) {
                inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                date = inputFormat.parse(bookingDate);
            }

            if (date != null) {
                SimpleDateFormat outputFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
                return outputFormat.format(date);
            }

            // Add logging to help debug the issue
            System.out.println("Failed to parse date: " + bookingDate);
            return "";

        } catch (Exception e) {
            // Log the error for debugging
            System.out.println("Error parsing date: " + bookingDate);
            e.printStackTrace();
            return "";
        }
    }
}