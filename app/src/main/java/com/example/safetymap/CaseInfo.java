package com.example.safetymap;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class CaseInfo {
    private String date;
    private String description;
    private String imageUrl;

    // Required empty constructor for Firebase
    public CaseInfo() {
        this.date = "Unknown Date";
        this.description = "No Description";
        this.imageUrl = null;
    }

    public CaseInfo(String date, String description, String imageUrl) {
        this.date = date != null ? date : "Unknown Date";
        this.description = description != null ? description : "No Description";
        this.imageUrl = imageUrl; // Can be null
    }

    // Getters and Setters

    public String getDate() {
        return date != null ? date : "Unknown Date";
    }

    public void setDate(String date) {
        this.date = date != null ? date : "Unknown Date";
    }

    public String getDescription() {
        return description != null ? description : "No Description";
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "No Description";
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    public String toString() {
        return "CaseInfo{" +
                "date='" + getDate() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}
