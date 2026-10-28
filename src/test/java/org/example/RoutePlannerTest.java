package org.example;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoutePlannerTest {
    private final Location location = new Location("Depot", 43, 76);
    private final DeliveryRequest standard = new DeliveryRequest(DeliveryPriority.STANDARD, 10000, 90, false);

    private Route route(String id, double distance, int minutes, double base, double co2,
                        int traffic, boolean available) {
        return new Route(id, location, location, distance, minutes, base, co2, traffic, available);
    }

    private List<RouteStrategy> strategies() {
        return List.of(new FastestRouteStrategy(), new CheapestRouteStrategy(),
                new EcoFriendlyRouteStrategy(), new BalancedRouteStrategy(0.4, 0.3, 0.1, 0.2),
                new DistanceFirstRouteStrategy());
    }

    @Test
    void fastestIncludesTrafficInsteadOfUsingBaseTime() {
        Route a = route("A", 10, 10, 1000, 1, 20, true);
        Route b = route("B", 10, 20, 1000, 1, 0, true);
        RouteDecision result = new FastestRouteStrategy().selectRoute(List.of(a, b), standard);
        assertEquals("B", result.route().id());
        assertEquals(20, result.score());
    }

    @Test
    void cheapestUsesDistanceCostNotOnlyBaseCost() {
        Route a = route("A", 20, 20, 100, 1, 0, true);
        Route b = route("B", 5, 20, 1000, 1, 0, true);
        RouteDecision result = new CheapestRouteStrategy().selectRoute(List.of(a, b), standard);
        assertEquals("B", result.route().id());
        assertEquals(1500, result.score());
    }

    @Test
    void urgentCostIncludesSurcharge() {
        Route a = route("A", 5, 20, 1000, 1, 0, true);
        DeliveryRequest urgent = new DeliveryRequest(DeliveryPriority.URGENT, 10000, 90, false);
        assertEquals(2000, a.totalCost(urgent));
        assertEquals(1500, a.totalCost(standard));
    }

    @Test
    void ecoUsesBothEmissionsAndDistance() {
        Route a = route("A", 20, 20, 1000, 0.1, 0, true);
        Route b = route("B", 5, 20, 1000, 1, 0, true);
        RouteDecision result = new EcoFriendlyRouteStrategy().selectRoute(List.of(a, b), standard);
        assertEquals("B", result.route().id());
        assertEquals(1.8, result.score(), 0.000001);
    }

    @Test
    void balancedCalculatesNormalizedWeightedScore() {
        Route a = route("A", 10, 10, 1000, 1, 0, true);
        Route b = route("B", 20, 20, 0, 2, 0, true);
        RouteDecision result = new BalancedRouteStrategy(0.4, 0.3, 0.1, 0.2)
                .selectRoute(List.of(a, b), standard);
        assertEquals("A", result.route().id());
        assertEquals(0.65, result.score(), 0.000001);
    }

    @Test
    void balancedWeightsChangeTheWinner() {
        List<Route> routes = DemoData.routes();
        assertEquals("B", new BalancedRouteStrategy(1, 0, 0, 0).selectRoute(routes, standard).route().id());
        assertEquals("H", new BalancedRouteStrategy(0, 1, 0, 0).selectRoute(routes, standard).route().id());
    }

    @Test
    void balancedHandlesZeroCostDistanceAndEmission() {
        Route zero = route("Zero", 0, 10, 0, 0, 0, true);
        RouteDecision result = new BalancedRouteStrategy(0.4, 0.3, 0.1, 0.2)
                .selectRoute(List.of(zero), standard);
        assertEquals(0.4, result.score(), 0.000001);
    }

    @Test
    void invalidWeightSumIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BalancedRouteStrategy(0.4, 0.4, 0.4, 0.4));
    }

    @Test
    void negativeAndNonfiniteWeightsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BalancedRouteStrategy(-0.1, 0.5, 0.3, 0.3));
        assertThrows(IllegalArgumentException.class, () -> new BalancedRouteStrategy(Double.NaN, 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new BalancedRouteStrategy(Double.POSITIVE_INFINITY, 0, 0, 0));
    }

    @Test
    void samePlannerSupportsFourRuntimeSwitches() {
        RoutePlanner planner = new RoutePlanner(new FastestRouteStrategy());
        String[] expected = {"B", "H", "C", "E", "E"};
        List<RouteStrategy> algorithms = strategies();
        for (int i = 0; i < algorithms.size(); i++) {
            planner.setStrategy(algorithms.get(i));
            assertEquals(expected[i], planner.plan(DemoData.routes(), standard).route().id());
        }
    }

    @Test
    void selectorMapsEveryPriority() {
        StrategySelector selector = new StrategySelector();
        Class<?>[] expected = {FastestRouteStrategy.class, CheapestRouteStrategy.class,
                EcoFriendlyRouteStrategy.class, BalancedRouteStrategy.class};
        DeliveryPriority[] priorities = DeliveryPriority.values();
        for (int i = 0; i < priorities.length; i++) {
            assertEquals(expected[i], selector.select(new DeliveryRequest(priorities[i], 10000, 90, false)).getClass());
        }
    }

    @Test
    void ecoPreferenceOverridesStandardButUrgencyComesFirst() {
        StrategySelector selector = new StrategySelector();
        assertInstanceOf(EcoFriendlyRouteStrategy.class,
                selector.select(new DeliveryRequest(DeliveryPriority.STANDARD, 10000, 90, true)));
        assertInstanceOf(FastestRouteStrategy.class,
                selector.select(new DeliveryRequest(DeliveryPriority.URGENT, 10000, 90, true)));
    }

    @Test
    void selectorRegistrationCanBeReplacedWithoutChangingPlanner() {
        StrategySelector selector = new StrategySelector();
        selector.register(DeliveryPriority.STANDARD, new DistanceFirstRouteStrategy());
        RoutePlanner planner = new RoutePlanner(selector.select(standard));
        assertEquals("E", planner.plan(DemoData.routes(), standard).route().id());
    }

    @Test
    void everyStrategyRejectsUnavailableRouteEvenWithBetterMetrics() {
        Route closed = route("Closed", 1, 1, 0, 0, 0, false);
        Route open = route("Open", 10, 20, 1000, 2, 0, true);
        for (RouteStrategy strategy : strategies()) {
            RouteDecision result = strategy.selectRoute(List.of(closed, open), standard);
            assertEquals("Open", result.route().id());
            assertEquals(List.of("Closed: route unavailable"), result.rejectedRoutes());
        }
    }

    @Test
    void everyStrategyFiltersBudgetBeforeScoring() {
        Route expensive = route("Expensive", 1, 1, 5000, 0, 0, true);
        Route affordable = route("Affordable", 5, 20, 500, 1, 0, true);
        DeliveryRequest request = new DeliveryRequest(DeliveryPriority.STANDARD, 1000, 90, false);
        for (RouteStrategy strategy : strategies()) {
            assertEquals("Affordable", strategy.selectRoute(List.of(expensive, affordable), request).route().id());
        }
    }

    @Test
    void everyStrategyFiltersDeadlineIncludingTraffic() {
        Route late = route("Late", 1, 10, 0, 0, 30, true);
        Route onTime = route("On time", 5, 20, 500, 1, 0, true);
        DeliveryRequest request = new DeliveryRequest(DeliveryPriority.STANDARD, 10000, 20, false);
        for (RouteStrategy strategy : strategies()) {
            assertEquals("On time", strategy.selectRoute(List.of(late, onTime), request).route().id());
        }
    }

    @Test
    void budgetAndDeadlineEqualityAreAllowed() {
        Route exact = route("Exact", 5, 15, 500, 1, 5, true);
        DeliveryRequest request = new DeliveryRequest(DeliveryPriority.STANDARD, 1000, 20, false);
        for (RouteStrategy strategy : strategies()) {
            assertEquals(exact, strategy.selectRoute(List.of(exact), request).route());
        }
    }

    @Test
    void noValidRouteHasExplicitFailureAndReasons() {
        DeliveryRequest impossible = new DeliveryRequest(DeliveryPriority.STANDARD, 1, 1, false);
        for (RouteStrategy strategy : strategies()) {
            NoSuitableRouteException error = assertThrows(NoSuitableRouteException.class,
                    () -> strategy.selectRoute(DemoData.routes(), impossible));
            assertTrue(error.getMessage().contains("budget exceeded"));
            assertTrue(error.getMessage().contains("deadline exceeded"));
        }
    }

    @Test
    void emptyRoutesFailExplicitly() {
        assertThrows(NoSuitableRouteException.class,
                () -> new FastestRouteStrategy().selectRoute(List.of(), standard));
    }

    @Test
    void fifthStrategySelectsShortestAvailableRoute() {
        RouteDecision result = new DistanceFirstRouteStrategy().selectRoute(DemoData.routes(), standard);
        assertEquals("E", result.route().id());
        assertEquals(8, result.score());
    }

    @Test
    void comparisonUsesSameRequestWithDifferentResults() {
        List<Route> routes = DemoData.routes();
        RouteDecision fastest = new FastestRouteStrategy().selectRoute(routes, standard);
        RouteDecision cheapest = new CheapestRouteStrategy().selectRoute(routes, standard);
        assertEquals("B", fastest.route().id());
        assertEquals("H", cheapest.route().id());
        assertTrue(fastest.route().totalMinutes() < cheapest.route().totalMinutes());
        assertTrue(cheapest.route().totalCost(standard) < fastest.route().totalCost(standard));
    }

    @Test
    void tighterBudgetChangesSameStrategyResult() {
        RouteStrategy fastest = new FastestRouteStrategy();
        assertEquals("B", fastest.selectRoute(DemoData.routes(), standard).route().id());
        DeliveryRequest limited = new DeliveryRequest(DeliveryPriority.STANDARD, 5000, 90, false);
        assertEquals("F", fastest.selectRoute(DemoData.routes(), limited).route().id());
    }

    @Test
    void explanationIncludesTrafficFormulaAndRejections() {
        RouteDecision result = new FastestRouteStrategy().selectRoute(DemoData.routes(), standard);
        assertTrue(result.explanation().contains("22 + 2 traffic = 24"));
        assertTrue(result.explanation().contains("score = estimated minutes + traffic penalty"));
        assertTrue(result.rejectedRoutes().contains("G: route unavailable"));
        assertThrows(UnsupportedOperationException.class, () -> result.rejectedRoutes().add("Changed"));
    }

    @Test
    void equalScoresKeepFirstInputRoute() {
        Route a = route("First", 5, 10, 500, 1, 0, true);
        Route b = route("Second", 5, 10, 500, 1, 0, true);
        for (RouteStrategy strategy : strategies()) {
            assertEquals(a, strategy.selectRoute(List.of(a, b), standard).route());
        }
    }

    @Test
    void invalidDomainDataIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> route("Bad", -1, 10, 100, 1, 0, true));
        assertThrows(IllegalArgumentException.class, () -> route("Bad", 1, 10, -100, 1, 0, true));
        assertThrows(IllegalArgumentException.class, () -> route("Bad", 1, 10, 100, Double.NaN, 0, true));
        assertThrows(IllegalArgumentException.class, () -> new Location("Bad", 91, 0));
        assertThrows(IllegalArgumentException.class, () -> new DeliveryRequest(null, 100, 10, false));
        assertThrows(IllegalArgumentException.class,
                () -> new DeliveryRequest(DeliveryPriority.STANDARD, -1, 10, false));
    }

    @Test
    void missingRequestAndStrategyAreRejected() {
        assertThrows(NullPointerException.class, () -> new RoutePlanner(null));
        RoutePlanner planner = new RoutePlanner(new FastestRouteStrategy());
        assertThrows(NullPointerException.class, () -> planner.setStrategy(null));
        assertThrows(NullPointerException.class, () -> planner.plan(DemoData.routes(), null));
        assertThrows(NullPointerException.class, () -> planner.plan(null, standard));
    }

    @Test
    void balancedNormalizationExcludesRejectedCandidates() {
        Route a = route("A", 10, 10, 1000, 1, 0, true);
        Route b = route("B", 20, 20, 0, 2, 0, true);
        Route closed = route("Closed", 1000, 1000, 100000, 1000, 0, false);
        RouteDecision result = new BalancedRouteStrategy(0.4, 0.3, 0.1, 0.2)
                .selectRoute(List.of(a, b, closed), standard);
        assertEquals("A", result.route().id());
        assertEquals(0.65, result.score(), 0.000001);
    }
}
