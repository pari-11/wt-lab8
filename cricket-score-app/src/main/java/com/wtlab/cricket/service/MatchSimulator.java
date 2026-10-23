package com.wtlab.cricket.service;

import com.wtlab.cricket.dto.BallRequest;
import com.wtlab.cricket.dto.Views.SimulatorStatus;
import com.wtlab.cricket.model.DismissalType;
import com.wtlab.cricket.model.ExtraType;
import com.wtlab.cricket.model.Match;
import com.wtlab.cricket.model.MatchStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Simulates a live data feed: on every tick it bowls one realistic random delivery in each live match,
 * which flows through the same scoring pipeline (and SSE broadcast) as a manual score update.
 */
@Component
public class MatchSimulator {

    private static final Logger log = LoggerFactory.getLogger(MatchSimulator.class);

    private final MatchService matchService;
    private final AtomicBoolean running;
    private final long intervalMs;
    private final boolean autoStartUpcoming;
    private final Random random = new Random();

    public MatchSimulator(MatchService matchService,
                          @Value("${cricket.simulator.enabled:true}") boolean enabled,
                          @Value("${cricket.simulator.interval-ms:3000}") long intervalMs,
                          @Value("${cricket.simulator.auto-start-upcoming:true}") boolean autoStartUpcoming) {
        this.matchService = matchService;
        this.running = new AtomicBoolean(enabled);
        this.intervalMs = intervalMs;
        this.autoStartUpcoming = autoStartUpcoming;
    }

    @Scheduled(fixedDelayString = "${cricket.simulator.interval-ms:3000}",
            initialDelayString = "${cricket.simulator.interval-ms:3000}")
    public void tick() {
        if (!running.get()) {
            return;
        }
        for (Match match : matchService.findByStatus(MatchStatus.LIVE)) {
            try {
                matchService.recordBall(match.getId(), randomBall());
            } catch (IllegalStateException e) {
                // The match finished (e.g. via a manual update) between listing and scoring.
                log.debug("Skipped {}: {}", match.getId(), e.getMessage());
            }
        }
        if (autoStartUpcoming) {
            for (Match match : matchService.findByStatus(MatchStatus.UPCOMING)) {
                if (match.getStartTime().isBefore(Instant.now())) {
                    log.info("Auto-starting match {} ({})", match.getId(), match.getTitle());
                    matchService.startMatch(match.getId());
                }
            }
        }
    }

    /** Weighted random outcome loosely based on T20 scoring patterns. */
    public BallRequest randomBall() {
        int roll = random.nextInt(1000);
        if (roll < 40) {
            DismissalType how = randomDismissal();
            int runs = how == DismissalType.RUN_OUT && random.nextBoolean() ? 1 : 0;
            return new BallRequest(runs, ExtraType.NONE, how);
        }
        if (roll < 70) {
            return new BallRequest(0, ExtraType.WIDE, null);
        }
        if (roll < 80) {
            return new BallRequest(randomRuns(), ExtraType.NO_BALL, null);
        }
        if (roll < 90) {
            return new BallRequest(1, ExtraType.LEG_BYE, null);
        }
        return new BallRequest(randomRuns(), ExtraType.NONE, null);
    }

    private int randomRuns() {
        int roll = random.nextInt(1000);
        if (roll < 330) return 0;
        if (roll < 670) return 1;
        if (roll < 760) return 2;
        if (roll < 770) return 3;
        if (roll < 920) return 4;
        return 6;
    }

    private DismissalType randomDismissal() {
        int roll = random.nextInt(100);
        if (roll < 55) return DismissalType.CAUGHT;
        if (roll < 75) return DismissalType.BOWLED;
        if (roll < 90) return DismissalType.LBW;
        if (roll < 96) return DismissalType.RUN_OUT;
        return DismissalType.STUMPED;
    }

    public SimulatorStatus status() {
        return new SimulatorStatus(running.get(), intervalMs);
    }

    public SimulatorStatus pause() {
        running.set(false);
        return status();
    }

    public SimulatorStatus resume() {
        running.set(true);
        return status();
    }
}
