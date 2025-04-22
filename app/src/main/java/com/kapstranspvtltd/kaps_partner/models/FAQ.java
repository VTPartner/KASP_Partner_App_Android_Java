package com.kapstranspvtltd.kaps_partner.models;

public class FAQ {
    private long faqId;
    private String question;
    private String answer;
    private double timeAt;
    private long categoryId;

    // Constructor
    public FAQ(long faqId, String question, String answer, double timeAt, long categoryId) {
        this.faqId = faqId;
        this.question = question;
        this.answer = answer;
        this.timeAt = timeAt;
        this.categoryId = categoryId;
    }

    // Getters
    public long getFaqId() { return faqId; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public double getTimeAt() { return timeAt; }
    public long getCategoryId() { return categoryId; }
}