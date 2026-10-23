package com.wtlab.cricket.service;

import com.wtlab.cricket.dto.BallRequest;
import com.wtlab.cricket.dto.CreateMatchRequest;
import com.wtlab.cricket.dto.Views.MatchSummaryView;
import com.wtlab.cricket.dto.Views.MatchView;
import com.wtlab.cricket.model.BallEvent;
import com.wtlab.cricket.model.Match;
import com.wtlab.cricket.model.MatchStatus;
import com.wtlab.cricket.model.Team;
import com.wtlab.cricket.repository.MatchRepository;
import com.wtlab.cricket.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class MatchService {

    /** Commentary entries embedded in every match view; the full log is on /commentary. */
    private static final int VIEW_COMMENTARY = 30;

    private final MatchRepository matches;
    private final TeamRepository teams;
    private final ScoringService scoring;
    private final MatchViewMapper mapper;
    private final LiveUpdatePublisher publisher;

    public MatchService(MatchRepository matches, TeamRepository teams, ScoringService scoring,
                        MatchViewMapper mapper, LiveUpdatePublisher publisher) {
        this.matches = matches;
        this.teams = teams;
        this.scoring = scoring;
        this.mapper = mapper;
        this.publisher = publisher;
    }

    public List<MatchView> listMatches(MatchStatus status) {
        List<Match> source = status == null ? matches.findAll() : matches.findByStatus(status);
        return source.stream()
                .sorted(Comparator.comparing(Match::getStatus).thenComparing(Match::getStartTime))
                .map(m -> mapper.toView(m, VIEW_COMMENTARY))
                .toList();
    }

    public MatchView getMatch(String id) {
        return mapper.toView(findMatch(id), VIEW_COMMENTARY);
    }

    public List<BallEvent> getCommentary(String id, int limit) {
        return mapper.commentary(findMatch(id), Math.max(1, Math.min(limit, 400)));
    }

    public MatchSummaryView getSummary(String id) {
        return mapper.toSummary(findMatch(id));
    }

    public MatchView createMatch(CreateMatchRequest request) {
        if (request.teamAId().equals(request.teamBId())) {
            throw new IllegalArgumentException("A team cannot play against itself");
        }
        Team a = findTeam(request.teamAId());
        Team b = findTeam(request.teamBId());
        int overs = request.overs() == null ? 20 : request.overs();
        Instant start = request.startTime() == null ? Instant.now().plus(Duration.ofMinutes(30)) : request.startTime();
        Match match = matches.save(new Match(matches.nextId(), a, b, request.venue(), overs, start));
        return publish(match);
    }

    public MatchView startMatch(String id) {
        Match match = findMatch(id);
        scoring.startMatch(match);
        return publish(match);
    }

    public MatchView recordBall(String id, BallRequest request) {
        Match match = findMatch(id);
        scoring.recordBall(match, request.extraOrNone(), request.runs(), request.dismissal());
        return publish(match);
    }

    /** Domain access for the simulator and data seeder. */
    public List<Match> findByStatus(MatchStatus status) {
        return matches.findByStatus(status);
    }

    private MatchView publish(Match match) {
        MatchView view = mapper.toView(match, VIEW_COMMENTARY);
        publisher.publish("match", view);
        return view;
    }

    private Match findMatch(String id) {
        return matches.findById(id).orElseThrow(() -> new NotFoundException("Match not found: " + id));
    }

    private Team findTeam(String id) {
        return teams.findById(id).orElseThrow(() -> new NotFoundException("Team not found: " + id));
    }
}
