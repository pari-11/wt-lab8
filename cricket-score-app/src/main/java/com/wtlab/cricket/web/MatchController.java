package com.wtlab.cricket.web;

import com.wtlab.cricket.dto.BallRequest;
import com.wtlab.cricket.dto.CreateMatchRequest;
import com.wtlab.cricket.dto.Views.MatchSummaryView;
import com.wtlab.cricket.dto.Views.MatchView;
import com.wtlab.cricket.model.BallEvent;
import com.wtlab.cricket.model.MatchStatus;
import com.wtlab.cricket.service.MatchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    /** All matches (live first), optionally filtered: ?status=LIVE|UPCOMING|COMPLETED */
    @GetMapping
    public List<MatchView> list(@RequestParam(required = false) MatchStatus status) {
        return matchService.listMatches(status);
    }

    /** Full scorecard, live state and latest commentary. */
    @GetMapping("/{id}")
    public MatchView get(@PathVariable String id) {
        return matchService.getMatch(id);
    }

    @GetMapping("/{id}/commentary")
    public List<BallEvent> commentary(@PathVariable String id, @RequestParam(defaultValue = "50") int limit) {
        return matchService.getCommentary(id, limit);
    }

    @GetMapping("/{id}/summary")
    public MatchSummaryView summary(@PathVariable String id) {
        return matchService.getSummary(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatchView create(@Valid @RequestBody CreateMatchRequest request) {
        return matchService.createMatch(request);
    }

    /** Toss and first ball: moves an UPCOMING match to LIVE. */
    @PostMapping("/{id}/start")
    public MatchView start(@PathVariable String id) {
        return matchService.startMatch(id);
    }

    /** Manual score update for one delivery. */
    @PostMapping("/{id}/balls")
    public MatchView recordBall(@PathVariable String id, @Valid @RequestBody BallRequest request) {
        return matchService.recordBall(id, request);
    }
}
