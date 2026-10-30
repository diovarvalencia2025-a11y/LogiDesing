package com.example.logidesignai;

public class ChatMessage {
    public static final int TYPE_USER = 1;
    public static final int TYPE_AI = 2;

    private long id = 0;
    private long conversationId = 0;
    private String text;
    private int type;
    private long timestamp = System.currentTimeMillis();
    private boolean isWebsiteReady = false;
    private String niche;
    private String businessName;
    private String phone;
    private String address;
    private String services;
    private String imageUri;

    public ChatMessage(String text, int type) {
        this.text = text;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public ChatMessage(long conversationId, String text, int type) {
        this.conversationId = conversationId;
        this.text = text;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getConversationId() {
        return conversationId;
    }

    public void setConversationId(long conversationId) {
        this.conversationId = conversationId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isWebsiteReady() {
        return isWebsiteReady;
    }

    public void setWebsiteReady(boolean websiteReady) {
        isWebsiteReady = websiteReady;
    }

    public String getNiche() {
        return niche;
    }

    public void setNiche(String niche) {
        this.niche = niche;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getServices() {
        return services;
    }

    public void setServices(String services) {
        this.services = services;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

}
