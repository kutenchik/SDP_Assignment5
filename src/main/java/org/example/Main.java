package org.example;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<Route> routes = DemoData.routes();
        DeliveryRequest request = new DeliveryRequest(DeliveryPriority.STANDARD, 10000, 90, false);
        List<RouteStrategy> strategies = List.of(new FastestRouteStrategy(),
                new CheapestRouteStrategy(), new EcoFriendlyRouteStrategy(),
                new BalancedRouteStrategy(0.4, 0.3, 0.1, 0.2),
                new DistanceFirstRouteStrategy());

        System.out.println("COMPARISON AND RUNTIME SWITCHING (same request and planner)");
        RoutePlanner planner = new RoutePlanner(strategies.get(0));
        for (RouteStrategy strategy : strategies) {
            planner.setStrategy(strategy);
            print(planner.plan(routes, request));
        }

        System.out.println("\nAUTOMATIC SELECTION");
        StrategySelector selector = new StrategySelector();
        List<DeliveryRequest> requests = List.of(
                new DeliveryRequest(DeliveryPriority.URGENT, 10000, 30, false),
                new DeliveryRequest(DeliveryPriority.ECONOMY, 2500, 90, false),
                new DeliveryRequest(DeliveryPriority.ECO, 4000, 60, true),
                new DeliveryRequest(DeliveryPriority.STANDARD, 10000, 90, false),
                new DeliveryRequest(DeliveryPriority.URGENT, 5500, 40, false));
        for (DeliveryRequest delivery : requests) {
            planner.setStrategy(selector.select(delivery));
            System.out.println("Request: " + delivery);
            print(planner.plan(routes, delivery));
        }

        System.out.println("\nFAILURE SCENARIOS");
        try {
            planner.plan(List.of(), request);
        } catch (NoSuitableRouteException e) {
            System.out.println(e.getMessage());
        }
        try {
            planner.plan(routes, new DeliveryRequest(DeliveryPriority.STANDARD, 1, 1, false));
        } catch (NoSuitableRouteException e) {
            System.out.println(e.getMessage());
        }
        try {
            new BalancedRouteStrategy(0.5, 0.5, 0.5, 0.5);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void print(RouteDecision decision) {
        System.out.println(decision.strategyName() + " -> " + decision.route().id());
        System.out.println("Reason: " + decision.explanation());
        for (String rejection : decision.rejectedRoutes()) {
            System.out.println("Rejected " + rejection);
        }
    }
}
