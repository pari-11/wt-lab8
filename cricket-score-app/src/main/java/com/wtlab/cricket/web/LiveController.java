package com.wtlab.cricket.web;

import com.wtlab.cricket.dto.Views.SimulatorStatus;
import com.wtlab.cricket.service.LiveUpdatePublisher;
import com.wtlab.cricket.service.MatchSimulator;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class LiveController {

    private final LiveUpdatePublisher publisher;
    private final MatchSimulator simulator;

    public LiveController(LiveUpdatePublisher publisher, MatchSimulator simulator) {
        this.publisher = publisher;
        this.simulator = simulator;
    }

    /** Server-Sent Events stream: a "match" event with the full MatchView after every score change. */
    @GetMapping(path = "/api/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return publisher.subscribe();
    }

    @GetMapping("/api/simulator")
    public SimulatorStatus simulatorStatus() {
        return simulator.status();
    }

    @PostMapping("/api/simulator/pause")
    public SimulatorStatus pause() {
        return simulator.pause();
    }

    @PostMapping("/api/simulator/resume")
    public SimulatorStatus resume() {
        return simulator.resume();
    }
}
