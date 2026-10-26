package org.example;

import java.util.List;

public class EcoFriendlyRouteStrategy extends ScoredRouteStrategy {
    public String getStrategyName() { return "Eco Friendly"; }
    protected String formula() { return "score = CO2 * 0.8 + distance * 0.2"; }
    protected double score(Route route, DeliveryRequest request, List<Route> valid) {
        return route.carbonEmission() * 0.8 + route.distanceKm() * 0.2;
    }
}
