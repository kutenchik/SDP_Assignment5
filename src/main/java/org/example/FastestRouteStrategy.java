package org.example;

import java.util.List;

public class FastestRouteStrategy extends ScoredRouteStrategy {
    public String getStrategyName() { return "Fastest"; }
    protected String formula() { return "score = estimated minutes + traffic penalty"; }
    protected double score(Route route, DeliveryRequest request, List<Route> valid) {
        return route.totalMinutes();
    }
}
