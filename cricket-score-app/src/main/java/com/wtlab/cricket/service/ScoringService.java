package com.wtlab.cricket.service;

import com.wtlab.cricket.model.BallEvent;
import com.wtlab.cricket.model.BattingEntry;
import com.wtlab.cricket.model.BowlingEntry;
import com.wtlab.cricket.model.Delivery;
import com.wtlab.cricket.model.DismissalType;
import com.wtlab.cricket.model.ExtraType;
import com.wtlab.cricket.model.Innings;
import com.wtlab.cricket.model.Match;
import com.wtlab.cricket.model.MatchStatus;
import com.wtlab.cricket.model.Player;
import com.wtlab.cricket.model.Team;
import com.wtlab.cricket.util.Format;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** The scoring engine: starts matches, applies deliveries, and handles innings breaks and results. */
@Service
public class ScoringService {

    private final CommentaryGenerator commentary;
    private final Random random = new Random();

    public ScoringService(CommentaryGenerator commentary) {
        this.commentary = commentary;
    }

    public void startMatch(Match match) {
        synchronized (match) {
            if (match.getStatus() != MatchStatus.UPCOMING) {
                throw new IllegalStateException("Match " + match.getId() + " has already started");
            }
            Team tossWinner = random.nextBoolean() ? match.getTeamA() : match.getTeamB();
            boolean electedToBat = random.nextBoolean();
            Team battingFirst = electedToBat ? tossWinner : match.opponentOf(tossWinner);

            match.setTossInfo(tossWinner.getName() + " won the toss and elected to " + (electedToBat ? "bat" : "bowl"));
            match.addInnings(new Innings(1, battingFirst, match.opponentOf(battingFirst), match.getOversPerInnings(), null));
            match.setStatus(MatchStatus.LIVE);
            if (match.getStartTime() == null || match.getStartTime().isAfter(Instant.now())) {
                match.setStartTime(Instant.now());
            }
            match.getTeamA().getPlayers().forEach(Player::recordMatchPlayed);
            match.getTeamB().getPlayers().forEach(Player::recordMatchPlayed);
            match.addCommentary(BallEvent.system(match.nextSequence(), 1, "Toss: " + match.getTossInfo() + "."));
        }
    }

    public BallEvent recordBall(Match match, ExtraType extra, int runs, DismissalType dismissal) {
        validate(extra, runs, dismissal);
        synchronized (match) {
            if (match.getStatus() != MatchStatus.LIVE) {
                throw new IllegalStateException("Match " + match.getId() + " is not live (status: " + match.getStatus() + ")");
            }
            Innings innings = match.currentInnings();
            Delivery delivery = innings.recordDelivery(extra, runs, dismissal, random);
            BallEvent event = new BallEvent(match.nextSequence(), innings.getNumber(), delivery.over(),
                    delivery.batter().getName(), delivery.bowler().getName(), delivery.totalRuns(), delivery.label(),
                    delivery.wicket(), delivery.isBoundary(), commentary.describe(delivery), Instant.now());
            match.addCommentary(event);

            if (innings.isCompleted()) {
                onInningsComplete(match, innings);
            }
            return event;
        }
    }

    private void validate(ExtraType extra, int runs, DismissalType dismissal) {
        if (runs < 0 || runs > 6) {
            throw new IllegalArgumentException("Runs must be between 0 and 6");
        }
        if (dismissal != null && extra != ExtraType.NONE) {
            throw new IllegalArgumentException("A wicket can only be recorded on a delivery without extras");
        }
        if (dismissal != null && dismissal != DismissalType.RUN_OUT && runs > 0) {
            throw new IllegalArgumentException("Only a run out can have runs on the same delivery");
        }
        if (extra == ExtraType.WIDE && runs > 4) {
            throw new IllegalArgumentException("A wide can carry at most 4 extra runs");
        }
        if ((extra == ExtraType.BYE || extra == ExtraType.LEG_BYE) && (runs < 1 || runs > 4)) {
            throw new IllegalArgumentException("Byes and leg byes must be between 1 and 4 runs");
        }
    }

    private void onInningsComplete(Match match, Innings finished) {
        if (finished.getNumber() == 1) {
            Innings chase = new Innings(2, finished.getBowlingTeam(), finished.getBattingTeam(),
                    match.getOversPerInnings(), finished.getRuns() + 1);
            match.addInnings(chase);
            match.addCommentary(BallEvent.system(match.nextSequence(), 1, "Innings break: "
                    + finished.getBattingTeam().getShortName() + " " + finished.getRuns() + "/" + finished.getWickets()
                    + " (" + Format.overs(finished.getLegalBalls()) + " ov). "
                    + chase.getBattingTeam().getName() + " need " + chase.getTarget() + " to win."));
        } else {
            finishMatch(match);
        }
    }

    private void finishMatch(Match match) {
        Innings first = match.getInnings().get(0);
        Innings second = match.getInnings().get(1);
        Team winner = null;
        String result;
        if (second.getRuns() >= second.getTarget()) {
            winner = second.getBattingTeam();
            int wicketsLeft = 10 - second.getWickets();
            int ballsLeft = match.getOversPerInnings() * 6 - second.getLegalBalls();
            result = winner.getName() + " won by " + wicketsLeft + (wicketsLeft == 1 ? " wicket" : " wickets")
                    + (ballsLeft > 0 ? " (" + ballsLeft + " balls left)" : "");
        } else if (second.getRuns() == first.getRuns()) {
            result = "Match tied";
        } else {
            winner = first.getBattingTeam();
            int margin = first.getRuns() - second.getRuns();
            result = winner.getName() + " won by " + margin + (margin == 1 ? " run" : " runs");
        }
        match.complete(result, winner, playerOfTheMatch(match, winner));
        match.addCommentary(BallEvent.system(match.nextSequence(), 2, "Result: " + result + "."));
    }

    /** Simple impact score: runs plus a bonus for fast scoring, and 25 points per wicket. */
    private String playerOfTheMatch(Match match, Team winner) {
        Map<Player, Double> impact = new HashMap<>();
        for (Innings innings : match.getInnings()) {
            for (BattingEntry b : innings.getBatting().values()) {
                double bonus = b.getBalls() > 0 ? (b.getRuns() - b.getBalls()) * 0.5 : 0;
                impact.merge(b.getPlayer(), b.getRuns() + bonus, Double::sum);
            }
            for (BowlingEntry b : innings.getBowling().values()) {
                impact.merge(b.getPlayer(), b.getWickets() * 25.0 - b.getRuns() * 0.3, Double::sum);
            }
        }
        return impact.entrySet().stream()
                .filter(e -> winner == null || e.getKey().getTeamId().equals(winner.getId()))
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().getName())
                .orElse(null);
    }
}
