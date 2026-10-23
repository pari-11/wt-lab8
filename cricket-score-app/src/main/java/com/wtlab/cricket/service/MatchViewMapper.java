package com.wtlab.cricket.service;

import com.wtlab.cricket.dto.Views.BatterView;
import com.wtlab.cricket.dto.Views.BowlerView;
import com.wtlab.cricket.dto.Views.ExtrasView;
import com.wtlab.cricket.dto.Views.InningsView;
import com.wtlab.cricket.dto.Views.LiveState;
import com.wtlab.cricket.dto.Views.MatchSummaryView;
import com.wtlab.cricket.dto.Views.MatchView;
import com.wtlab.cricket.dto.Views.TeamView;
import com.wtlab.cricket.dto.Views.TopPerformer;
import com.wtlab.cricket.model.BallEvent;
import com.wtlab.cricket.model.BattingEntry;
import com.wtlab.cricket.model.BowlingEntry;
import com.wtlab.cricket.model.Innings;
import com.wtlab.cricket.model.Match;
import com.wtlab.cricket.model.MatchStatus;
import com.wtlab.cricket.model.Team;
import com.wtlab.cricket.util.Format;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Builds immutable JSON views from a match while holding its lock. */
@Component
public class MatchViewMapper {

    public MatchView toView(Match m, int commentaryLimit) {
        synchronized (m) {
            Innings current = m.currentInnings();
            LiveState live = m.getStatus() == MatchStatus.LIVE && current != null ? liveState(current) : null;
            List<BallEvent> commentary = m.getCommentary().stream().limit(commentaryLimit).toList();
            return new MatchView(m.getId(), m.getTitle(), m.getMatchType(), m.getVenue(), m.getStatus(),
                    m.getOversPerInnings(), m.getStartTime(), team(m.getTeamA()), team(m.getTeamB()), m.getTossInfo(),
                    statusText(m), m.getResult(), m.getPlayerOfTheMatch(), m.getInnings().size() - 1,
                    m.getInnings().stream().map(this::innings).toList(), live, commentary);
        }
    }

    public List<BallEvent> commentary(Match m, int limit) {
        synchronized (m) {
            return m.getCommentary().stream().limit(limit).toList();
        }
    }

    public MatchSummaryView toSummary(Match m) {
        synchronized (m) {
            List<String> scores = new ArrayList<>();
            List<TopPerformer> batters = new ArrayList<>();
            List<TopPerformer> bowlers = new ArrayList<>();
            for (Innings inn : m.getInnings()) {
                scores.add(inn.getBattingTeam().getShortName() + " " + inn.getRuns() + "/" + inn.getWickets()
                        + " (" + Format.overs(inn.getLegalBalls()) + " ov)");
                String battingSide = inn.getBattingTeam().getShortName();
                String bowlingSide = inn.getBowlingTeam().getShortName();
                for (BattingEntry b : inn.getBatting().values()) {
                    batters.add(new TopPerformer(b.getPlayer().getName(), battingSide,
                            b.getRuns() + (b.isOut() ? "" : "*") + " (" + b.getBalls() + ")", b.getRuns()));
                }
                for (BowlingEntry b : inn.getBowling().values()) {
                    if (b.getBalls() > 0) {
                        // Rank by wickets, then economy: fewer runs breaks ties.
                        bowlers.add(new TopPerformer(b.getPlayer().getName(), bowlingSide,
                                b.getWickets() + "/" + b.getRuns() + " (" + Format.overs(b.getBalls()) + ")",
                                b.getWickets() * 1000 - b.getRuns()));
                    }
                }
            }
            Comparator<TopPerformer> byValue = Comparator.comparingInt(TopPerformer::value).reversed();
            return new MatchSummaryView(m.getId(), m.getTitle(), m.getStatus(), m.getResult(),
                    m.getPlayerOfTheMatch(), scores,
                    batters.stream().sorted(byValue).limit(3).toList(),
                    bowlers.stream().sorted(byValue).limit(3).toList());
        }
    }

    private String statusText(Match m) {
        return switch (m.getStatus()) {
            case UPCOMING -> "Match yet to begin";
            case COMPLETED -> m.getResult();
            case LIVE -> {
                Innings inn = m.currentInnings();
                if (inn.getTarget() == null) {
                    yield m.getTossInfo();
                }
                int need = inn.getTarget() - inn.getRuns();
                int balls = inn.getMaxOvers() * 6 - inn.getLegalBalls();
                yield inn.getBattingTeam().getShortName() + " need " + need + (need == 1 ? " run" : " runs")
                        + " in " + balls + (balls == 1 ? " ball" : " balls");
            }
        };
    }

    private LiveState liveState(Innings inn) {
        int ballsRemaining = inn.getMaxOvers() * 6 - inn.getLegalBalls();
        Integer target = inn.getTarget();
        Integer runsNeeded = target == null ? null : Math.max(0, target - inn.getRuns());
        Double rrr = target == null || ballsRemaining == 0 ? null : Format.round2(runsNeeded * 6.0 / ballsRemaining);
        return new LiveState(
                batter(inn, inn.getStrikerId()),
                batter(inn, inn.getNonStrikerId()),
                bowler(inn.getBowling().get(inn.getBowlerId())),
                Format.rate(inn.getRuns(), inn.getLegalBalls()),
                rrr, target, runsNeeded, ballsRemaining, List.copyOf(inn.getThisOver()));
    }

    private InningsView innings(Innings inn) {
        return new InningsView(inn.getNumber(), team(inn.getBattingTeam()), team(inn.getBowlingTeam()),
                inn.getRuns(), inn.getWickets(), Format.overs(inn.getLegalBalls()),
                Format.rate(inn.getRuns(), inn.getLegalBalls()),
                new ExtrasView(inn.getExtras(), inn.getWides(), inn.getNoBalls(), inn.getByes(), inn.getLegByes()),
                inn.getBatting().keySet().stream().map(id -> batter(inn, id)).toList(),
                inn.getBowling().values().stream().map(this::bowler).toList(),
                List.copyOf(inn.getFallOfWickets()), List.copyOf(inn.getRunsPerOver()),
                inn.getTarget(), inn.isCompleted());
    }

    private BatterView batter(Innings inn, String playerId) {
        BattingEntry b = playerId == null ? null : inn.getBatting().get(playerId);
        if (b == null) {
            return null;
        }
        boolean onStrike = !inn.isCompleted() && playerId.equals(inn.getStrikerId());
        return new BatterView(playerId, b.getPlayer().getName(), b.getRuns(), b.getBalls(), b.getFours(),
                b.getSixes(), Format.strikeRate(b.getRuns(), b.getBalls()), b.getDismissal(), b.isOut(), onStrike);
    }

    private BowlerView bowler(BowlingEntry b) {
        if (b == null) {
            return null;
        }
        return new BowlerView(b.getPlayer().getId(), b.getPlayer().getName(), Format.overs(b.getBalls()),
                b.getMaidens(), b.getRuns(), b.getWickets(), Format.rate(b.getRuns(), b.getBalls()));
    }

    static TeamView team(Team t) {
        return new TeamView(t.getId(), t.getName(), t.getShortName(), t.getColor());
    }
}
