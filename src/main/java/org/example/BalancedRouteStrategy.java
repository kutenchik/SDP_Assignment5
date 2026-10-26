package org.example;

import java.util.List;
import java.util.function.ToDoubleFunction;

public class BalancedRouteStrategy extends ScoredRouteStrategy {
    private final double timeWeight;
    private final double costWeight;
    private final double distanceWeight;
    private final double emissionWeight;

    public BalancedRouteStrategy(double time, double cost, double distance, double emission) {
        double sum = time + cost + distance + emission;
        if (!Double.isFinite(sum) || time < 0 || cost < 0 || distance < 0 || emission < 0
                || Math.abs(sum - 1.0) > 0.000001) {
            throw new IllegalArgumentException("Weights must be finite, nonnegative and sum to 1");
        }
        timeWeight = time;
        costWeight = cost;
        distanceWeight = distance;
        emissionWeight = emission;
    }

    public String getStrategyName() { return "Balanced"; }

    protected String formula() {
        return "score = " + timeWeight + " * normalized time + " + costWeight
                + " * normalized cost + " + distanceWeight + " * normalized distance + "
                + emissionWeight + " * normalized CO2 (each value / feasible maximum)";
    }

    protected double score(Route route, DeliveryRequest request, List<Route> valid) {
        return timeWeight * normalize(route.totalMinutes(), valid, Route::totalMinutes)
                + costWeight * normalize(route.totalCost(request), valid, r -> r.totalCost(request))
                + distanceWeight * normalize(route.distanceKm(), valid, Route::distanceKm)
                + emissionWeight * normalize(route.carbonEmission(), valid, Route::carbonEmission);
    }

    private double normalize(double value, List<Route> routes, ToDoubleFunction<Route> metric) {
        double maximum = 0;
        for (Route route : routes) maximum = Math.max(maximum, metric.applyAsDouble(route));
        return maximum == 0 ? 0 : value / maximum;
    }
}
