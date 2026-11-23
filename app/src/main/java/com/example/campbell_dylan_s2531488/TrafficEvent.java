package com.example.campbell_dylan_s2531488;

import android.location.Location;

public class TrafficEvent {

    private String id;
    private String description;
    private String roadName;
    private String eventType;
    private String severity;
    private String startTime;
    private String endTime;

    private Double latitude;
    private Double longitude;

    private Double fromLatitude;
    private Double fromLongitude;
    private Double toLatitude;
    private Double toLongitude;

    public TrafficEvent() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRoadName() {
        return roadName;
    }

    public void setRoadName(String roadName) {
        this.roadName = roadName;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getFromLatitude() {
        return fromLatitude;
    }

    public void setFromLatitude(Double fromLatitude) {
        this.fromLatitude = fromLatitude;
    }

    public Double getFromLongitude() {
        return fromLongitude;
    }

    public void setFromLongitude(Double fromLongitude) {
        this.fromLongitude = fromLongitude;
    }

    public Double getToLatitude() {
        return toLatitude;
    }

    public void setToLatitude(Double toLatitude) {
        this.toLatitude = toLatitude;
    }

    public Double getToLongitude() {
        return toLongitude;
    }

    public void setToLongitude(Double toLongitude) {
        this.toLongitude = toLongitude;
    }

    public Double getDistanceKmFrom(
            double userLatitude,
            double userLongitude
    ) {

        if (!isValidCoordinates(
                userLatitude,
                userLongitude
        )) {
            return null;
        }

        Double shortestDistance = null;

        if (isValidCoordinates(latitude, longitude)) {

            shortestDistance = calculateDistanceKm(
                    userLatitude,
                    userLongitude,
                    latitude,
                    longitude
            );
        }

        if (isValidCoordinates(
                fromLatitude,
                fromLongitude
        )) {

            double distance = calculateDistanceKm(
                    userLatitude,
                    userLongitude,
                    fromLatitude,
                    fromLongitude
            );

            if (shortestDistance == null
                    || distance < shortestDistance) {

                shortestDistance = distance;
            }
        }

        if (isValidCoordinates(
                toLatitude,
                toLongitude
        )) {

            double distance = calculateDistanceKm(
                    userLatitude,
                    userLongitude,
                    toLatitude,
                    toLongitude
            );

            if (shortestDistance == null
                    || distance < shortestDistance) {

                shortestDistance = distance;
            }
        }

        return shortestDistance;
    }

    private double calculateDistanceKm(
            double startLatitude,
            double startLongitude,
            double endLatitude,
            double endLongitude
    ) {

        float[] results = new float[1];

        Location.distanceBetween(
                startLatitude,
                startLongitude,
                endLatitude,
                endLongitude,
                results
        );

        return results[0] / 1000.0;
    }

    private boolean isValidCoordinates(
            Double latitude,
            Double longitude
    ) {

        if (latitude == null || longitude == null) {
            return false;
        }

        return !Double.isNaN(latitude)
                && !Double.isNaN(longitude)
                && !Double.isInfinite(latitude)
                && !Double.isInfinite(longitude)
                && latitude >= -90
                && latitude <= 90
                && longitude >= -180
                && longitude <= 180;
    }

    @Override
    public String toString() {
        return "TrafficEvent{" +
                "id='" + id + '\'' +
                ", description='" + description + '\'' +
                ", roadName='" + roadName + '\'' +
                ", eventType='" + eventType + '\'' +
                ", severity='" + severity + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", fromLatitude=" + fromLatitude +
                ", fromLongitude=" + fromLongitude +
                ", toLatitude=" + toLatitude +
                ", toLongitude=" + toLongitude +
                '}';
    }
}
