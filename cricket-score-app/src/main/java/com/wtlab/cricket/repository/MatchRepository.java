package com.wtlab.cricket.repository;

import com.wtlab.cricket.model.Match;
import com.wtlab.cricket.model.MatchStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** In-memory, thread-safe match store. */
@Repository
public class MatchRepository {

    private final Map<String, Match> matches = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();

    public String nextId() {
        return "m" + ids.incrementAndGet();
    }

    public Match save(Match match) {
        matches.put(match.getId(), match);
        return match;
    }

    public Optional<Match> findById(String id) {
        return Optional.ofNullable(matches.get(id));
    }

    public List<Match> findAll() {
        return List.copyOf(matches.values());
    }

    public List<Match> findByStatus(MatchStatus status) {
        return matches.values().stream().filter(m -> m.getStatus() == status).toList();
    }
}
