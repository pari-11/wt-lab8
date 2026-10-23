package com.wtlab.cricket.model;

import java.time.Instant;

/**
 * A commentary entry. Deliveries carry an over number and a short label ("4", "W", "1wd");
 * system messages (toss, innings break, result) have an empty label.
 */
public record BallEvent(
        long sequence,
        int innings,
        String over,
        String batter,
        String bowler,
        int runs,
        String label,
        boolean wicket,
        boolean boundary,
        String text,
        Instant timestamp) {

    public static BallEvent system(long sequence, int innings, String text) {
        return new BallEvent(sequence, innings, "", "", "", 0, "", false, false, text, Instant.now());
    }
}
