package com.kapstranspvtltd.kaps_partner.models;

import org.json.JSONException;
import org.json.JSONObject;

public class OrderRatingModel {
    private String customerName;
    private String bookingDate;
    private String rating;
    private String comment;
    private String bookingId;

    public OrderRatingModel(String customerName, String bookingDate, String rating, String comment, String bookingId) {
        this.customerName = customerName;
        this.bookingDate = bookingDate;
        this.rating = rating;
        this.comment = comment;
        this.bookingId = bookingId;
    }

    public static OrderRatingModel fromJson(JSONObject json) throws JSONException {
        return new OrderRatingModel(
                json.optString("customer_name", ""),
                json.optString("booking_date", ""),
                json.optString("ratings", "0"),  // Changed from "rating" to "ratings" to match API
                json.optString("rating_description", ""),
                json.optString("booking_id", "")
        );
    }

    // Getters
    public String getCustomerName() { return customerName; }
    public String getBookingDate() { return bookingDate; }
    public String getRating() { return rating.isEmpty() ? "0" : rating; }
    public String getComment() { return comment; }
    public String getBookingId() { return bookingId; }
}