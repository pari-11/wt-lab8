package com.wtlab.cricket.dto;

import com.wtlab.cricket.model.BallEvent;
import com.wtlab.cricket.model.MatchStatus;

import java.time.Instant;
import java.util.List;

/**
 * Read-only JSON views returned by the REST API and pushed over the live stream.
 * They are immutable snapshots, so they can be serialized without holding any locks.
 */
public final class Views {

    private Views() {
    }

    public record TeamView(String id, String name, String shortName, String color) {
    }

    public record BatterView(String playerId, String name, int runs, int balls, int fours, int sixes,
                             double strikeRate, String dismissal, boolean out, boolean onStrike) {
    }

    public record BowlerView(String playerId, String name, String overs, int maidens, int runs, int wickets,
                             double economy) {
    }

    public record ExtrasView(int total, int wides, int noBalls, int byes, int legByes) {
    }

    public record InningsView(int number, TeamView battingTeam, TeamView bowlingTeam, int runs, int wickets,
                              String overs, double runRate, ExtrasView extras, List<BatterView> batting,
                              List<BowlerView> bowling, List<String> fallOfWickets, List<Integer> runsPerOver,
                              Integer target, boolean completed) {
    }

    public record LiveState(BatterView striker, BatterView nonStriker, BowlerView bowler, double currentRunRate,
                            Double requiredRunRate, Integer target, Integer runsNeeded, int ballsRemaining,
                            List<String> thisOver) {
    }

    public record MatchView(String id, String title, String matchType, String venue, MatchStatus status,
                            int oversPerInnings, Instant startTime, TeamView teamA, TeamView teamB, String tossInfo,
                            String statusText, String result, String playerOfTheMatch, int currentInnings,
                            List<InningsView> innings, LiveState live, List<BallEvent> commentary) {
    }

    public record TopPerformer(String name, String team, String figures, int value) {
    }

    public record MatchSummaryView(String matchId, String title, MatchStatus status, String result,
                                   String playerOfTheMatch, List<String> scores, List<TopPerformer> topBatters,
                                   List<TopPerformer> topBowlers) {
    }

    public record PlayerStatsView(String id, String name, String team, String teamColor, String role, int matches,
                                  int runs, int ballsFaced, int highestScore, int fours, int sixes,
                                  Double battingAverage, double strikeRate, int wickets, String overs,
                                  int runsConceded, Double economy) {
    }

    public record SimulatorStatus(boolean running, long intervalMs) {
    }
}
