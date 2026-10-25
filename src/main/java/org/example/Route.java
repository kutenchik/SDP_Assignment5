package org.example;

public record Route(String id, Location origin, Location destination,
                    double distanceKm, int estimatedMinutes, double baseCost,
                    double carbonEmission, int trafficPenaltyMinutes, boolean available) {
    public Route {
        if (id == null || id.isBlank() || origin == null || destination == null
                || !Double.isFinite(distanceKm) || distanceKm < 0
                || estimatedMinutes <= 0 || !Double.isFinite(baseCost) || baseCost < 0
                || !Double.isFinite(carbonEmission) || carbonEmission < 0
                || trafficPenaltyMinutes < 0) {
            throw new IllegalArgumentException("Invalid route data");
        }
    }

    public double totalMinutes() {
        return (double) estimatedMinutes + trafficPenaltyMinutes;
    }

    public double totalCost(DeliveryRequest request) {
        double prioritySurcharge = request.priority() == DeliveryPriority.URGENT ? 500 : 0;
        return baseCost + distanceKm * 100 + prioritySurcharge;
    }
}
