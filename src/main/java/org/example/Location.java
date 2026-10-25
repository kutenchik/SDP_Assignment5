package org.example;

public record Location(String name, double latitude, double longitude) {
    public Location {
        if (name == null || name.isBlank() || !Double.isFinite(latitude)
                || !Double.isFinite(longitude) || Math.abs(latitude) > 90
                || Math.abs(longitude) > 180) {
            throw new IllegalArgumentException("Invalid location");
        }
    }
}
