package com.example.uiuc_toilet_android;

public class Bathroom {
    private String id;
    private String name;
    private String gender;

    private String openTime;
    private String closeTime;

    private double longitude;
    private double latitude;
    private double distanceFromUser;

    public Bathroom(String id, String name, String gender, String openTime, String closeTime, double latitude, double longitude, double distanceFromUser) {
        this.id = id;
        this.name = name;
        this.gender = gender;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.longitude = longitude;
        this.latitude = latitude;
        this.distanceFromUser = distanceFromUser;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getOpenTime() {
        return openTime;
    }

    public void setOpenTime(String openTime) {
        this.openTime = openTime;
    }

    public String getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(String closeTime) {
        this.closeTime = closeTime;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getDistanceFromUser() {
        return distanceFromUser;
    }

    public void setDistanceFromUser(double distanceFromUser) {
        this.distanceFromUser = distanceFromUser;
    }
}
