package com.wtlab.cricket.model;

import com.wtlab.cricket.util.Format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Score state of one innings: runs, wickets, who is batting and bowling, and the full scorecard.
 * Not thread-safe on its own; callers lock the owning {@link Match}.
 */
public class Innings {

    private final int number;
    private final Team battingTeam;
    private final Team bowlingTeam;
    private final int maxOvers;
    private final Integer target;

    private final Map<String, BattingEntry> batting = new LinkedHashMap<>();
    private final Map<String, BowlingEntry> bowling = new LinkedHashMap<>();
    private final List<String> fallOfWickets = new ArrayList<>();
    private final List<Integer> runsPerOver = new ArrayList<>();
    private List<String> thisOver = new ArrayList<>();

    private int runs;
    private int wickets;
    private int legalBalls;
    private int wides;
    private int noBalls;
    private int byes;
    private int legByes;

    private int nextBatterIndex;
    private int bowlerRotation;
    private String strikerId;
    private String nonStrikerId;
    private String bowlerId;
    private boolean overJustCompleted;
    private boolean completed;

    public Innings(int number, Team battingTeam, Team bowlingTeam, int maxOvers, Integer target) {
        this.number = number;
        this.battingTeam = battingTeam;
        this.bowlingTeam = bowlingTeam;
        this.maxOvers = maxOvers;
        this.target = target;
        this.strikerId = bringInNextBatter();
        this.nonStrikerId = bringInNextBatter();
        this.bowlerId = chooseNextBowler(null);
    }

    /**
     * Applies one delivery to the innings.
     *
     * @param extra     type of extra, NONE for a normal ball
     * @param ranRuns   runs off the bat, or runs taken on top of a wide / as byes
     * @param dismissal how the striker got out, or null
     */
    public Delivery recordDelivery(ExtraType extra, int ranRuns, DismissalType dismissal, Random random) {
        if (completed) {
            throw new IllegalStateException("Innings " + number + " is already complete");
        }
        if (overJustCompleted) {
            thisOver = new ArrayList<>();
            overJustCompleted = false;
        }

        BattingEntry striker = batting.get(strikerId);
        BowlingEntry bowler = bowling.get(bowlerId);
        String overLabel = (legalBalls / 6) + "." + (legalBalls % 6 + 1);
        int overIndex = legalBalls / 6;

        int batRuns = 0;
        int extraRuns = 0;
        int chargedToBowler = 0;
        switch (extra) {
            case NONE -> {
                batRuns = ranRuns;
                chargedToBowler = ranRuns;
            }
            case WIDE -> {
                extraRuns = 1 + ranRuns;
                wides += extraRuns;
                chargedToBowler = extraRuns;
            }
            case NO_BALL -> {
                extraRuns = 1;
                noBalls++;
                batRuns = ranRuns;
                chargedToBowler = 1 + ranRuns;
            }
            case BYE -> {
                extraRuns = ranRuns;
                byes += ranRuns;
            }
            case LEG_BYE -> {
                extraRuns = ranRuns;
                legByes += ranRuns;
            }
        }

        int total = batRuns + extraRuns;
        runs += total;
        while (runsPerOver.size() <= overIndex) {
            runsPerOver.add(0);
        }
        runsPerOver.set(overIndex, runsPerOver.get(overIndex) + total);

        boolean faced = extra != ExtraType.WIDE;
        striker.addRuns(batRuns, faced);
        striker.getPlayer().recordBatting(batRuns, faced);

        boolean wicket = dismissal != null;
        boolean bowlerWicket = wicket && dismissal.creditsBowler();
        bowler.addDelivery(extra.isLegal(), chargedToBowler, bowlerWicket);
        bowler.getPlayer().recordBowling(extra.isLegal(), chargedToBowler, bowlerWicket);
        if (extra.isLegal()) {
            legalBalls++;
        }

        String label = label(extra, ranRuns, batRuns, extraRuns, wicket);
        thisOver.add(label);

        Player batter = striker.getPlayer();
        batter.recordInningsScore(striker.getRuns());
        String dismissalText = null;
        if (wicket) {
            wickets++;
            dismissalText = describe(dismissal, bowler.getPlayer(), random);
            striker.dismiss(dismissalText);
            batter.recordDismissal();
            fallOfWickets.add(runs + "-" + wickets + " (" + batter.getName() + ", " + Format.overs(legalBalls) + " ov)");
            strikerId = bringInNextBatter();
        } else if (ranRuns % 2 == 1) {
            swapStrike();
        }

        boolean overComplete = extra.isLegal() && legalBalls % 6 == 0;
        if (overComplete) {
            bowler.endOver();
        }

        if (isFinished()) {
            completed = true;
        } else if (overComplete) {
            swapStrike();
            bowlerId = chooseNextBowler(bowlerId);
            overJustCompleted = true;
        }

        return new Delivery(overLabel, batter, bowler.getPlayer(), extra, batRuns, total, label, wicket, dismissalText);
    }

    private boolean isFinished() {
        return wickets >= 10
                || strikerId == null
                || legalBalls >= maxOvers * 6
                || (target != null && runs >= target);
    }

    private void swapStrike() {
        String tmp = strikerId;
        strikerId = nonStrikerId;
        nonStrikerId = tmp;
    }

    private String bringInNextBatter() {
        List<Player> order = battingTeam.getPlayers();
        if (nextBatterIndex >= order.size()) {
            return null;
        }
        Player next = order.get(nextBatterIndex++);
        batting.put(next.getId(), new BattingEntry(next));
        return next.getId();
    }

    /** Rotates through the bowling options, never the same bowler twice in a row, max overs/5 overs each. */
    private String chooseNextBowler(String previousId) {
        List<Player> options = bowlingTeam.bowlers();
        int maxBallsPerBowler = Math.max(1, (int) Math.ceil(maxOvers / 5.0)) * 6;
        for (int i = 0; i < options.size(); i++) {
            Player candidate = options.get((bowlerRotation + i) % options.size());
            BowlingEntry figures = bowling.get(candidate.getId());
            if (candidate.getId().equals(previousId) || (figures != null && figures.getBalls() >= maxBallsPerBowler)) {
                continue;
            }
            bowlerRotation = (bowlerRotation + i + 1) % options.size();
            bowling.computeIfAbsent(candidate.getId(), id -> new BowlingEntry(candidate));
            return candidate.getId();
        }
        // Everyone has bowled their quota: fall back to any fielder who did not bowl the last over.
        for (Player fielder : bowlingTeam.getPlayers()) {
            if (!fielder.getId().equals(previousId)) {
                bowling.computeIfAbsent(fielder.getId(), id -> new BowlingEntry(fielder));
                return fielder.getId();
            }
        }
        return previousId;
    }

    private String describe(DismissalType type, Player bowler, Random random) {
        List<Player> fielders = bowlingTeam.getPlayers();
        Player fielder = fielders.get(random.nextInt(fielders.size()));
        String b = bowler.getShortName();
        return switch (type) {
            case BOWLED -> "b " + b;
            case LBW -> "lbw b " + b;
            case CAUGHT -> fielder == bowler ? "c & b " + b : "c " + fielder.getShortName() + " b " + b;
            case STUMPED -> "st " + bowlingTeam.wicketKeeper().getShortName() + " b " + b;
            case RUN_OUT -> "run out (" + fielder.getShortName() + ")";
        };
    }

    private static String label(ExtraType extra, int ranRuns, int batRuns, int extraRuns, boolean wicket) {
        return switch (extra) {
            case NONE -> wicket ? (ranRuns > 0 ? ranRuns + "W" : "W") : String.valueOf(ranRuns);
            case WIDE -> extraRuns == 1 ? "wd" : extraRuns + "wd";
            case NO_BALL -> batRuns == 0 ? "nb" : batRuns + "nb";
            case BYE -> ranRuns + "b";
            case LEG_BYE -> ranRuns + "lb";
        };
    }

    public int getExtras() {
        return wides + noBalls + byes + legByes;
    }

    public int getNumber() {
        return number;
    }

    public Team getBattingTeam() {
        return battingTeam;
    }

    public Team getBowlingTeam() {
        return bowlingTeam;
    }

    public int getMaxOvers() {
        return maxOvers;
    }

    public Integer getTarget() {
        return target;
    }

    public Map<String, BattingEntry> getBatting() {
        return Collections.unmodifiableMap(batting);
    }

    public Map<String, BowlingEntry> getBowling() {
        return Collections.unmodifiableMap(bowling);
    }

    public List<String> getFallOfWickets() {
        return Collections.unmodifiableList(fallOfWickets);
    }

    public List<Integer> getRunsPerOver() {
        return Collections.unmodifiableList(runsPerOver);
    }

    public List<String> getThisOver() {
        return Collections.unmodifiableList(thisOver);
    }

    public int getRuns() {
        return runs;
    }

    public int getWickets() {
        return wickets;
    }

    public int getLegalBalls() {
        return legalBalls;
    }

    public int getWides() {
        return wides;
    }

    public int getNoBalls() {
        return noBalls;
    }

    public int getByes() {
        return byes;
    }

    public int getLegByes() {
        return legByes;
    }

    public String getStrikerId() {
        return strikerId;
    }

    public String getNonStrikerId() {
        return nonStrikerId;
    }

    public String getBowlerId() {
        return bowlerId;
    }

    public boolean isCompleted() {
        return completed;
    }
}
