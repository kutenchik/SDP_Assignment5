package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

// Shared filtering and explanation; concrete strategies only define their score.
public abstract class ScoredRouteStrategy implements RouteStrategy {
    @Override
    public RouteDecision selectRoute(List<Route> routes, DeliveryRequest request) {
        Objects.requireNonNull(routes, "Routes are required");
        Objects.requireNonNull(request, "Request is required");
        List<Route> valid = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        for (Route route : routes) {
            Objects.requireNonNull(route, "Route is required");
            List<String> reasons = new ArrayList<>();
            if (!route.available()) reasons.add("route unavailable");
            if (route.totalCost(request) > request.maxBudget()) reasons.add("budget exceeded");
            if (route.totalMinutes() > request.maxDeliveryMinutes()) reasons.add("deadline exceeded");
            if (reasons.isEmpty()) valid.add(route);
            else rejected.add(route.id() + ": " + String.join(", ", reasons));
        }
        if (valid.isEmpty()) {
            throw new NoSuitableRouteException("No suitable route. "
                    + (routes.isEmpty() ? "Route list is empty." : String.join("; ", rejected)));
        }

        Route best = valid.getFirst();
        double bestScore = score(best, request, valid);
        for (Route route : valid) {
            double candidateScore = score(route, request, valid);
            if (candidateScore < bestScore) {
                best = route;
                bestScore = candidateScore;
            }
        }
        String explanation = String.format(Locale.US,
                "%s; minimum score %.4f. Time: %d + %d traffic = %.0f min; "
                        + "cost: %.2f + %.1f km * 100 + %.0f priority = %.2f; "
                        + "distance: %.1f km; CO2: %.2f kg. Ties keep the first route.",
                formula(), bestScore, best.estimatedMinutes(), best.trafficPenaltyMinutes(),
                best.totalMinutes(), best.baseCost(), best.distanceKm(),
                request.priority() == DeliveryPriority.URGENT ? 500.0 : 0.0,
                best.totalCost(request), best.distanceKm(), best.carbonEmission());
        return new RouteDecision(best, getStrategyName(), bestScore, explanation, rejected);
    }

    protected abstract double score(Route route, DeliveryRequest request, List<Route> valid);
    protected abstract String formula();
}
