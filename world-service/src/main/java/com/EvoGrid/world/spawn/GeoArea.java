package com.EvoGrid.world.spawn;

import java.util.concurrent.ThreadLocalRandom;

record GeoArea(double lat, double lon, double radiusMeters) {

    private static final double METERS_PER_DEGREE = 111_320;

    double latDelta() {
        return radiusMeters / METERS_PER_DEGREE;
    }

    double lonDelta() {
        return radiusMeters / (METERS_PER_DEGREE * Math.cos(Math.toRadians(lat)));
    }

    double[] randomPoint() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double distance = radiusMeters * Math.sqrt(random.nextDouble());
        double angle = random.nextDouble(2 * Math.PI);
        double dLat = distance * Math.cos(angle) / METERS_PER_DEGREE;
        double dLon = distance * Math.sin(angle) / (METERS_PER_DEGREE * Math.cos(Math.toRadians(lat)));
        return new double[] {lat + dLat, lon + dLon};
    }
}
