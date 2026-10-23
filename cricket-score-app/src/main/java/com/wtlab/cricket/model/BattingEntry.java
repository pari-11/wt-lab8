package com.wtlab.cricket.model;

/** One batter's line on the scorecard for a single innings. */
public class BattingEntry {

    private final Player player;
    private int runs;
    private int balls;
    private int fours;
    private int sixes;
    private boolean out;
    private String dismissal = "not out";

    public BattingEntry(Player player) {
        this.player = player;
    }

    public void addRuns(int runsScored, boolean facedBall) {
        runs += runsScored;
        if (facedBall) {
            balls++;
        }
        if (runsScored == 4) {
            fours++;
        } else if (runsScored == 6) {
            sixes++;
        }
    }

    public void dismiss(String description) {
        out = true;
        dismissal = description;
    }

    public Player getPlayer() {
        return player;
    }

    public int getRuns() {
        return runs;
    }

    public int getBalls() {
        return balls;
    }

    public int getFours() {
        return fours;
    }

    public int getSixes() {
        return sixes;
    }

    public boolean isOut() {
        return out;
    }

    public String getDismissal() {
        return dismissal;
    }
}
