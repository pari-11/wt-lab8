package com.wtlab.cricket.model;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * A match and its innings. All reads and writes of a match's mutable state happen while holding
 * the match's monitor ({@code synchronized (match)}), so concurrent score updates stay consistent.
 */
public class Match {

    private static final int MAX_COMMENTARY = 400;

    private final String id;
    private final Team teamA;
    private final Team teamB;
    private final String venue;
    private final int oversPerInnings;
    private final List<Innings> innings = new ArrayList<>();
    private final Deque<BallEvent> commentary = new ArrayDeque<>();

    private Instant startTime;
    private MatchStatus status = MatchStatus.UPCOMING;
    private String tossInfo;
    private String result;
    private Team winner;
    private String playerOfTheMatch;
    private long sequence;

    public Match(String id, Team teamA, Team teamB, String venue, int oversPerInnings, Instant startTime) {
        this.id = id;
        this.teamA = teamA;
        this.teamB = teamB;
        this.venue = venue;
        this.oversPerInnings = oversPerInnings;
        this.startTime = startTime;
    }

    public String getMatchType() {
        return switch (oversPerInnings) {
            case 20 -> "T20";
            case 50 -> "ODI";
            case 10 -> "T10";
            default -> oversPerInnings + "-over";
        };
    }

    public String getTitle() {
        return teamA.getShortName() + " vs " + teamB.getShortName();
    }

    public Team opponentOf(Team team) {
        return team == teamA ? teamB : teamA;
    }

    public Innings currentInnings() {
        return innings.isEmpty() ? null : innings.get(innings.size() - 1);
    }

    public void addInnings(Innings next) {
        innings.add(next);
    }

    public long nextSequence() {
        return ++sequence;
    }

    public void addCommentary(BallEvent event) {
        commentary.addFirst(event);
        while (commentary.size() > MAX_COMMENTARY) {
            commentary.removeLast();
        }
    }

    public void complete(String result, Team winner, String playerOfTheMatch) {
        this.status = MatchStatus.COMPLETED;
        this.result = result;
        this.winner = winner;
        this.playerOfTheMatch = playerOfTheMatch;
    }

    public String getId() {
        return id;
    }

    public Team getTeamA() {
        return teamA;
    }

    public Team getTeamB() {
        return teamB;
    }

    public String getVenue() {
        return venue;
    }

    public int getOversPerInnings() {
        return oversPerInnings;
    }

    public List<Innings> getInnings() {
        return Collections.unmodifiableList(innings);
    }

    /** Newest first. */
    public Deque<BallEvent> getCommentary() {
        return commentary;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }

    public String getTossInfo() {
        return tossInfo;
    }

    public void setTossInfo(String tossInfo) {
        this.tossInfo = tossInfo;
    }

    public String getResult() {
        return result;
    }

    public Team getWinner() {
        return winner;
    }

    public String getPlayerOfTheMatch() {
        return playerOfTheMatch;
    }
}
