package com.example.safetymap;

import com.google.firebase.database.IgnoreExtraProperties;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@IgnoreExtraProperties
public class MarkerInfo {
    private String category;
    private String description;
    private long reportedDate; // Use long instead of Date for Firebase compatibility
    private double latitude;
    private double longitude;
    private int numberOfCases;
    private String ratingOrUrgency; // For warnings and complaints
    private Map<String, CaseInfo> caseDescriptions; // Changed from List to Map
    private List<String> imageUrls; // Changed from Uri to String

    // Required empty constructor for Firebase
    public MarkerInfo() {
        this.category = "Unknown Category";
        this.description = "No Description";
        this.ratingOrUrgency = "Unknown";
        this.caseDescriptions = new HashMap<>();
        this.imageUrls = new ArrayList<>();
    }

    // Constructor
    public MarkerInfo(String category, String description, long reportedDate, double latitude, double longitude, int numberOfCases, String ratingOrUrgency, List<String> imageUrls) {
        this.category = category != null ? category : "Unknown Category";
        this.description = description != null ? description : "No Description";
        this.reportedDate = reportedDate;
        this.latitude = latitude;
        this.longitude = longitude;
        this.numberOfCases = numberOfCases;
        this.ratingOrUrgency = ratingOrUrgency != null ? ratingOrUrgency : "Unknown";
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
        this.caseDescriptions = new HashMap<>();
        // Initialize with the first case
        String caseId = generateUniqueCaseId();
        this.caseDescriptions.put(caseId, new CaseInfo(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(reportedDate)), description, imageUrls != null && !imageUrls.isEmpty() ? imageUrls.get(0) : null));
    }

    // Method to generate a unique case ID
    private String generateUniqueCaseId() {
        return "case_" + UUID.randomUUID().toString();
    }

    // Getters and Setters

    public String getCategory() {
        return category != null ? category : "Unknown Category";
    }

    public void setCategory(String category) {
        this.category = category != null ? category : "Unknown Category";
    }

    public String getDescription() {
        return description != null ? description : "No Description";
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "No Description";
    }

    public long getReportedDate() {
        return reportedDate;
    }

    public void setReportedDate(long reportedDate) {
        this.reportedDate = reportedDate;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getNumberOfCases() {
        return numberOfCases;
    }

    public void setNumberOfCases(int numberOfCases) {
        this.numberOfCases = numberOfCases;
    }

    public String getRatingOrUrgency() {
        return ratingOrUrgency != null ? ratingOrUrgency : "Unknown";
    }

    public void setRatingOrUrgency(String ratingOrUrgency) {
        this.ratingOrUrgency = ratingOrUrgency != null ? ratingOrUrgency : "Unknown";
    }

    public Map<String, CaseInfo> getCaseDescriptions() {
        return caseDescriptions != null ? caseDescriptions : new HashMap<>();
    }

    public void setCaseDescriptions(Map<String, CaseInfo> caseDescriptions) {
        this.caseDescriptions = caseDescriptions != null ? caseDescriptions : new HashMap<>();
    }

    public List<String> getImageUrls() {
        return imageUrls != null ? imageUrls : new ArrayList<>();
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
    }

    // Method to add a new case
    public void addCase(String description, String imageUrl) {
        String caseId = generateUniqueCaseId();
        String formattedDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
        String caseDescription = description != null ? description : "No Description";
        String caseImageUrl = imageUrl != null ? imageUrl : null;
        CaseInfo newCase = new CaseInfo(formattedDate, caseDescription, caseImageUrl);
        this.caseDescriptions.put(caseId, newCase);
        this.numberOfCases = this.caseDescriptions.size();
    }

    @Override
    public String toString() {
        return "MarkerInfo{" +
                "category='" + getCategory() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", reportedDate=" + reportedDate +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", numberOfCases=" + numberOfCases +
                ", ratingOrUrgency='" + getRatingOrUrgency() + '\'' +
                ", caseDescriptions=" + caseDescriptions +
                ", imageUrls=" + imageUrls +
                '}';
    }
}
