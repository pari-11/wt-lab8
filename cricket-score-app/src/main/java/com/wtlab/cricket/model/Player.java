package com.wtlab.cricket.model;

import com.wtlab.cricket.util.Format;

/**
 * A player with career statistics that are updated ball by ball while matches are played.
 * Mutators are synchronized because the simulator and the REST API can update stats concurrently.
 */
public class Player {

    private final String id;
    private final String name;
    private final String teamId;
    private final PlayerRole role;

    private int matches;
    private int runs;
    private int ballsFaced;
    private int fours;
    private int sixes;
    private int highestScore;
    private int dismissals;
    private int ballsBowled;
    private int runsConceded;
    private int wickets;

    public Player(String id, String name, String teamId, PlayerRole role) {
        this.id = id;
        this.name = name;
        this.teamId = teamId;
        this.role = role;
    }

    public synchronized void recordMatchPlayed() {
        matches++;
    }

    public synchronized void recordBatting(int runsScored, boolean facedBall) {
        runs += runsScored;
        if (facedBall) {
            ballsFaced++;
        }
        if (runsScored == 4) {
            fours++;
        } else if (runsScored == 6) {
            sixes++;
        }
    }

    public synchronized void recordInningsScore(int inningsRuns) {
        highestScore = Math.max(highestScore, inningsRuns);
    }

    public synchronized void recordDismissal() {
        dismissals++;
    }

    public synchronized void recordBowling(boolean legalBall, int runsGiven, boolean tookWicket) {
        if (legalBall) {
            ballsBowled++;
        }
        runsConceded += runsGiven;
        if (tookWicket) {
            wickets++;
        }
    }

    public String getShortName() {
        return Format.surname(name);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTeamId() {
        return teamId;
    }

    public PlayerRole getRole() {
        return role;
    }

    public synchronized int getMatches() {
        return matches;
    }

    public synchronized int getRuns() {
        return runs;
    }

    public synchronized int getBallsFaced() {
        return ballsFaced;
    }

    public synchronized int getFours() {
        return fours;
    }

    public synchronized int getSixes() {
        return sixes;
    }

    public synchronized int getHighestScore() {
        return highestScore;
    }

    public synchronized int getDismissals() {
        return dismissals;
    }

    public synchronized int getBallsBowled() {
        return ballsBowled;
    }

    public synchronized int getRunsConceded() {
        return runsConceded;
    }

    public synchronized int getWickets() {
        return wickets;
    }
}
