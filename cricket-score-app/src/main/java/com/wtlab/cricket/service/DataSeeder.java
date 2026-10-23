package com.wtlab.cricket.service;

import com.wtlab.cricket.model.Match;
import com.wtlab.cricket.model.MatchStatus;
import com.wtlab.cricket.model.PlayerRole;
import com.wtlab.cricket.model.Team;
import com.wtlab.cricket.repository.MatchRepository;
import com.wtlab.cricket.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Predicate;

import static com.wtlab.cricket.model.PlayerRole.ALL_ROUNDER;
import static com.wtlab.cricket.model.PlayerRole.BATTER;
import static com.wtlab.cricket.model.PlayerRole.BOWLER;
import static com.wtlab.cricket.model.PlayerRole.WICKET_KEEPER;

/** Loads four teams and a mix of completed, live and upcoming matches on startup. */
@Component
@ConditionalOnProperty(name = "cricket.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final TeamRepository teams;
    private final MatchRepository matches;
    private final MatchService matchService;
    private final MatchSimulator simulator;

    public DataSeeder(TeamRepository teams, MatchRepository matches, MatchService matchService,
                      MatchSimulator simulator) {
        this.teams = teams;
        this.matches = matches;
        this.matchService = matchService;
        this.simulator = simulator;
    }

    @Override
    public void run(ApplicationArguments args) {
        Team ind = team("ind", "India", "IND", "#1f6feb",
                "Rohit Sharma", BATTER, "Shubman Gill", BATTER, "Virat Kohli", BATTER, "Suryakumar Yadav", BATTER,
                "Rishabh Pant", WICKET_KEEPER, "Hardik Pandya", ALL_ROUNDER, "Ravindra Jadeja", ALL_ROUNDER,
                "Axar Patel", ALL_ROUNDER, "Kuldeep Yadav", BOWLER, "Jasprit Bumrah", BOWLER, "Mohammed Siraj", BOWLER);
        Team aus = team("aus", "Australia", "AUS", "#f5b700",
                "Travis Head", BATTER, "David Warner", BATTER, "Mitchell Marsh", ALL_ROUNDER, "Glenn Maxwell", ALL_ROUNDER,
                "Marcus Stoinis", ALL_ROUNDER, "Tim David", BATTER, "Matthew Wade", WICKET_KEEPER,
                "Pat Cummins", BOWLER, "Mitchell Starc", BOWLER, "Adam Zampa", BOWLER, "Josh Hazlewood", BOWLER);
        Team eng = team("eng", "England", "ENG", "#e5484d",
                "Jos Buttler", WICKET_KEEPER, "Phil Salt", BATTER, "Will Jacks", ALL_ROUNDER, "Jonny Bairstow", BATTER,
                "Harry Brook", BATTER, "Liam Livingstone", ALL_ROUNDER, "Moeen Ali", ALL_ROUNDER,
                "Chris Jordan", BOWLER, "Jofra Archer", BOWLER, "Adil Rashid", BOWLER, "Mark Wood", BOWLER);
        Team nz = team("nz", "New Zealand", "NZ", "#8b95a5",
                "Devon Conway", WICKET_KEEPER, "Finn Allen", BATTER, "Kane Williamson", BATTER, "Daryl Mitchell", BATTER,
                "Glenn Phillips", ALL_ROUNDER, "Mark Chapman", BATTER, "Mitchell Santner", ALL_ROUNDER,
                "Tim Southee", BOWLER, "Trent Boult", BOWLER, "Lockie Ferguson", BOWLER, "Ish Sodhi", BOWLER);

        Instant now = Instant.now();

        // A finished match from yesterday.
        Match completed = match(ind, eng, "Eden Gardens, Kolkata", now.minus(Duration.ofDays(1)));
        playUntil(completed, m -> m.getStatus() == MatchStatus.COMPLETED);
        completed.setStartTime(now.minus(Duration.ofDays(1)));

        // Live: first innings, a little past the powerplay.
        Match first = match(ind, aus, "Wankhede Stadium, Mumbai", now.minus(Duration.ofMinutes(40)));
        playUntil(first, m -> m.currentInnings().getLegalBalls() >= 52);

        // Live: second innings chase under way.
        Match chase = match(eng, nz, "Lord's, London", now.minus(Duration.ofMinutes(110)));
        playUntil(chase, m -> m.getInnings().size() == 2 && m.currentInnings().getLegalBalls() >= 46);

        // Upcoming: starts automatically in two hours (or start it via POST /api/matches/{id}/start).
        matches.save(new Match(matches.nextId(), aus, nz, "Melbourne Cricket Ground", 20, now.plus(Duration.ofHours(2))));

        log.info("Seeded {} teams and {} matches", teams.findAll().size(), matches.findAll().size());
    }

    private Team team(String id, String name, String shortName, String color, Object... squad) {
        Team team = new Team(id, name, shortName, color);
        for (int i = 0; i < squad.length; i += 2) {
            team.addPlayer((String) squad[i], (PlayerRole) squad[i + 1]);
        }
        return teams.save(team);
    }

    private Match match(Team a, Team b, String venue, Instant start) {
        Match match = matches.save(new Match(matches.nextId(), a, b, venue, 20, start));
        matchService.startMatch(match.getId());
        match.setStartTime(start);
        return match;
    }

    private void playUntil(Match match, Predicate<Match> done) {
        for (int guard = 0; guard < 1000 && match.getStatus() == MatchStatus.LIVE && !done.test(match); guard++) {
            matchService.recordBall(match.getId(), simulator.randomBall());
        }
    }
}
