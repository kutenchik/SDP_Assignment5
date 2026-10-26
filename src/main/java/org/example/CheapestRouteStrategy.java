package org.example;

import java.util.List;

public class CheapestRouteStrategy extends ScoredRouteStrategy {
    public String getStrategyName() { return "Cheapest"; }
    protected String formula() { return "score = base cost + distance * 100 + priority surcharge"; }
    protected double score(Route route, DeliveryRequest request, List<Route> valid) {
        return route.totalCost(request);
    }
}
