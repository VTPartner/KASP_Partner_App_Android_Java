package com.kapstranspvtltd.kaps_partner.adapters;

import com.kapstranspvtltd.kaps_partner.models.OrderModel;

import java.util.List;

public class EarningsSummary {
    private double earnings;
    private String timeSpent;
    private int tripsCount;
    private List<OrderModel> orders;

    private String weeklyTimeSpent;

    public EarningsSummary(double earnings, String timeSpent, String weeklyTimeSpent, int tripsCount, List<OrderModel> orders) {
        this.earnings = earnings;
        this.timeSpent = timeSpent;
        this.tripsCount = tripsCount;
        this.weeklyTimeSpent = weeklyTimeSpent;
        this.orders = orders;
    }

    public double getEarnings() { return earnings; }
    public String getTimeSpent() { return timeSpent; }
    public int getTripsCount() { return tripsCount; }
    public List<OrderModel> getOrders() { return orders; }

    public String getWeeklyTimeSpent() { return weeklyTimeSpent; }
}