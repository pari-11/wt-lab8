package com.wtlab.cricket.model;

/** One bowler's figures for a single innings. */
public class BowlingEntry {

    private final Player player;
    private int balls;
    private int runs;
    private int wickets;
    private int maidens;
    private int runsThisOver;

    public BowlingEntry(Player player) {
        this.player = player;
    }

    public void addDelivery(boolean legalBall, int runsConceded, boolean tookWicket) {
        if (legalBall) {
            balls++;
        }
        runs += runsConceded;
        runsThisOver += runsConceded;
        if (tookWicket) {
            wickets++;
        }
    }

    public void endOver() {
        if (runsThisOver == 0) {
            maidens++;
        }
        runsThisOver = 0;
    }

    public Player getPlayer() {
        return player;
    }

    public int getBalls() {
        return balls;
    }

    public int getRuns() {
        return runs;
    }

    public int getWickets() {
        return wickets;
    }

    public int getMaidens() {
        return maidens;
    }
}
