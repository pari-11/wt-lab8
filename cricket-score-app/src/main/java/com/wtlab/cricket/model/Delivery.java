package com.wtlab.cricket.model;

/** What happened on a single delivery, as worked out by {@link Innings#recordDelivery}. */
public record Delivery(
        String over,
        Player batter,
        Player bowler,
        ExtraType extra,
        int batRuns,
        int totalRuns,
        String label,
        boolean wicket,
        String dismissal) {

    public boolean isBoundary() {
        return extra != ExtraType.WIDE && (batRuns == 4 || batRuns == 6);
    }
}
