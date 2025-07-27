package com.kapstranspvtltd.kaps_partner.models;

public class AppContent {
    private int contentId;
    private String screenName;
    private String title;
    private String description;
    private String imageUrl;
    private int sortOrder;
    private int status;
    private double timeAt;

    public AppContent(int contentId, String screenName, String title, String description, 
                     String imageUrl, int sortOrder, int status, double timeAt) {
        this.contentId = contentId;
        this.screenName = screenName;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
        this.status = status;
        this.timeAt = timeAt;
    }

    // Getters
    public int getContentId() { return contentId; }
    public String getScreenName() { return screenName; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public int getSortOrder() { return sortOrder; }
    public int getStatus() { return status; }
    public double getTimeAt() { return timeAt; }

    // Setters
    public void setContentId(int contentId) { this.contentId = contentId; }
    public void setScreenName(String screenName) { this.screenName = screenName; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public void setStatus(int status) { this.status = status; }
    public void setTimeAt(double timeAt) { this.timeAt = timeAt; }
} 