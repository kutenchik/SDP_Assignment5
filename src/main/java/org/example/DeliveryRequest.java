package org.example;

public record DeliveryRequest(DeliveryPriority priority, double maxBudget,
                              int maxDeliveryMinutes, boolean ecoPreferred) {
    public DeliveryRequest {
        if (priority == null || !Double.isFinite(maxBudget)
                || maxBudget < 0 || maxDeliveryMinutes <= 0) {
            throw new IllegalArgumentException("Invalid delivery request");
        }
    }
}
