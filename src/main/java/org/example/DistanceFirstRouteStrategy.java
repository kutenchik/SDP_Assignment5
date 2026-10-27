package org.example;

import java.util.List;

public class DistanceFirstRouteStrategy extends ScoredRouteStrategy {
    public String getStrategyName() { return "Distance First"; }
    protected String formula() { return "score = distance in km"; }
    protected double score(Route route, DeliveryRequest request, List<Route> valid) {
        return route.distanceKm();
    }
}
