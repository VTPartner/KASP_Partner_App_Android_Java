package com.kapstranspvtltd.kaps_partner.common_activities.models;

import java.util.Date;

public class CalendarDay {
    private String dayName;
    private int dayNumber;
    private Date date;
    private boolean isSelected;

    public CalendarDay(String dayName, int dayNumber, Date date) {
        this.dayName = dayName;
        this.dayNumber = dayNumber;
        this.date = date;
        this.isSelected = false;
    }

    // Getters and setters
    public String getDayName() { return dayName; }
    public int getDayNumber() { return dayNumber; }
    public Date getDate() { return date; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}