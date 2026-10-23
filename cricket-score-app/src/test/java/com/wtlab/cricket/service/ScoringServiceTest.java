package com.wtlab.cricket.service;

import com.wtlab.cricket.model.DismissalType;
import com.wtlab.cricket.model.ExtraType;
import com.wtlab.cricket.model.Innings;
import com.wtlab.cricket.model.Match;
import com.wtlab.cricket.model.MatchStatus;
import com.wtlab.cricket.model.PlayerRole;
import com.wtlab.cricket.model.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoringServiceTest {

    private ScoringService scoring;
    private Match match;

    @BeforeEach
    void setUp() {
        scoring = new ScoringService(new CommentaryGenerator());
        match = new Match("t1", team("a", "Alpha"), team("b", "Beta"), "Test Ground", 2, Instant.now());
        scoring.startMatch(match);
    }

    @Test
    void boundaryAddsRunsToTeamAndBatter() {
        Innings inn = match.currentInnings();
        String striker = inn.getStrikerId();

        scoring.recordBall(match, ExtraType.NONE, 4, null);

        assertEquals(4, inn.getRuns());
        assertEquals(1, inn.getLegalBalls());
        assertEquals(4, inn.getBatting().get(striker).getRuns());
        assertEquals(1, inn.getBatting().get(striker).getFours());
        assertEquals(striker, inn.getStrikerId(), "even runs keep the strike");
    }

    @Test
    void wideIsAnExtraAndNotALegalBall() {
        Innings inn = match.currentInnings();

        scoring.recordBall(match, ExtraType.WIDE, 0, null);

        assertEquals(1, inn.getRuns());
        assertEquals(1, inn.getWides());
        assertEquals(0, inn.getLegalBalls());
    }

    @Test
    void singleRotatesStrikeAndOverEndRotatesAgain() {
        Innings inn = match.currentInnings();
        String opener = inn.getStrikerId();
        String firstBowler = inn.getBowlerId();

        scoring.recordBall(match, ExtraType.NONE, 1, null);
        assertNotEquals(opener, inn.getStrikerId());

        for (int i = 0; i < 5; i++) {
            scoring.recordBall(match, ExtraType.NONE, 0, null);
        }
        assertEquals(opener, inn.getStrikerId(), "strike changes at the end of the over");
        assertNotEquals(firstBowler, inn.getBowlerId(), "a new bowler takes the next over");
    }

    @Test
    void wicketBringsInNextBatter() {
        Innings inn = match.currentInnings();
        String striker = inn.getStrikerId();

        scoring.recordBall(match, ExtraType.NONE, 0, DismissalType.BOWLED);

        assertEquals(1, inn.getWickets());
        assertTrue(inn.getBatting().get(striker).isOut());
        assertNotEquals(striker, inn.getStrikerId());
        assertEquals(1, inn.getBowling().values().stream().mapToInt(b -> b.getWickets()).sum());
    }

    @Test
    void matchCompletesAfterBothInnings() {
        for (int i = 0; i < 24 && match.getStatus() == MatchStatus.LIVE; i++) {
            scoring.recordBall(match, ExtraType.NONE, 1, null);
        }
        assertEquals(MatchStatus.COMPLETED, match.getStatus());
        assertNotNull(match.getResult());
    }

    @Test
    void rejectsWicketOnAWide() {
        assertThrows(IllegalArgumentException.class,
                () -> scoring.recordBall(match, ExtraType.WIDE, 0, DismissalType.CAUGHT));
    }

    private static Team team(String id, String name) {
        Team team = new Team(id, name, name.substring(0, 3).toUpperCase(), "#888888");
        for (int i = 1; i <= 11; i++) {
            team.addPlayer(name + " Player" + i, i > 6 ? PlayerRole.BOWLER : PlayerRole.BATTER);
        }
        return team;
    }
}
