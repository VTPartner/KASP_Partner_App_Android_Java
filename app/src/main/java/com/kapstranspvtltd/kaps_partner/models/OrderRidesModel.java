package com.kapstranspvtltd.kaps_partner.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONObject;

public class OrderRidesModel implements Parcelable {
    private final String customerName;
    private final double bookingTiming;
    private final double totalPrice;

    public double getPenaltyAmount() {
        return penaltyAmount;
    }

    private final double penaltyAmount;
    private final String pickupAddress;
    private final String dropAddress;
    private final String customerImage;

    // Constructor
    public OrderRidesModel(String customerName, double bookingTiming, double totalPrice,
                           double penaltyAmount, String pickupAddress, String dropAddress, String customerImage) {
        this.customerName = customerName;
        this.bookingTiming = bookingTiming;
        this.totalPrice = totalPrice;
        this.penaltyAmount = penaltyAmount;
        this.pickupAddress = pickupAddress;
        this.dropAddress = dropAddress;
        this.customerImage = customerImage;
    }

    // Parcelable constructor
    protected OrderRidesModel(Parcel in) {
        customerName = in.readString();
        bookingTiming = in.readDouble();
        totalPrice = in.readDouble();
        penaltyAmount = in.readDouble();
        pickupAddress = in.readString();
        dropAddress = in.readString();
        customerImage = in.readString();
    }

    // Factory method to create from JSON
    public static OrderRidesModel fromJson(JSONObject json) {
        try {
            return new OrderRidesModel(
                json.getString("customer_name"),
                Double.parseDouble(json.getString("booking_timing")),
                Double.parseDouble(json.getString("total_price")),
                    Double.parseDouble(json.getString("penalty_amount")),
                json.getString("pickup_address"),
                json.getString("drop_address"),
                json.optString("customer_image")
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Getters
    public String getCustomerName() {
        return customerName;
    }

    public double getBookingTiming() {
        return bookingTiming;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public String getDropAddress() {
        return dropAddress;
    }

    public String getCustomerImage() {
        return customerImage;
    }

    // Parcelable implementation
    public static final Creator<OrderRidesModel> CREATOR = new Creator<OrderRidesModel>() {
        @Override
        public OrderRidesModel createFromParcel(Parcel in) {
            return new OrderRidesModel(in);
        }

        @Override
        public OrderRidesModel[] newArray(int size) {
            return new OrderRidesModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(customerName);
        dest.writeDouble(bookingTiming);
        dest.writeDouble(totalPrice);
        dest.writeDouble(penaltyAmount);
        dest.writeString(pickupAddress);
        dest.writeString(dropAddress);
        dest.writeString(customerImage);
    }

    // Optional: Override equals and hashCode for proper object comparison
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderRidesModel that = (OrderRidesModel) o;
        return Double.compare(that.bookingTiming, bookingTiming) == 0 &&
               Double.compare(that.totalPrice, totalPrice) == 0 &&
                Double.compare(that.penaltyAmount, penaltyAmount) == 0 &&
               customerName.equals(that.customerName) &&
               pickupAddress.equals(that.pickupAddress) &&
               dropAddress.equals(that.dropAddress) &&
               (customerImage != null ? customerImage.equals(that.customerImage) 
                                    : that.customerImage == null);
    }

    @Override
    public int hashCode() {
        int result = customerName.hashCode();
        result = 31 * result + Double.hashCode(bookingTiming);
        result = 31 * result + Double.hashCode(totalPrice);
        result = 31 * result + Double.hashCode(penaltyAmount);
        result = 31 * result + pickupAddress.hashCode();
        result = 31 * result + dropAddress.hashCode();
        result = 31 * result + (customerImage != null ? customerImage.hashCode() : 0);
        return result;
    }

    // Optional: Override toString for better debugging
    @Override
    public String toString() {
        return "OrderRidesModel{" +
               "customerName='" + customerName + '\'' +
               ", bookingTiming=" + bookingTiming +
               ", totalPrice=" + totalPrice +
                ", penaltyAmount=" + penaltyAmount +
               ", pickupAddress='" + pickupAddress + '\'' +
               ", dropAddress='" + dropAddress + '\'' +
               ", customerImage='" + customerImage + '\'' +
               '}';
    }
}