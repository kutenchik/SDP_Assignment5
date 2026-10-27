package org.example;

import java.util.List;

public class DemoData {
    public static List<Route> routes() {
        Location depot = new Location("Almaty depot", 43.24, 76.89);
        Location customer = new Location("Customer", 43.25, 76.95);
        return List.of(
                new Route("A", depot, customer, 12, 18, 3500, 4.5, 15, true),
                new Route("B", depot, customer, 15, 22, 4000, 5.0, 2, true),
                new Route("C", depot, customer, 10, 45, 2000, 0.5, 0, true),
                new Route("D", depot, customer, 18, 40, 500, 3.0, 5, true),
                new Route("E", depot, customer, 8, 35, 2200, 1.0, 3, true),
                new Route("F", depot, customer, 20, 28, 2500, 6.0, 4, true),
                new Route("G", depot, customer, 6, 12, 100, 0.1, 0, false),
                new Route("H", depot, customer, 14, 55, 800, 2.0, 10, true));
    }
}
