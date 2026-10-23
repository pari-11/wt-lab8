package com.wtlab.cricket.repository;

import com.wtlab.cricket.model.Player;
import com.wtlab.cricket.model.Team;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory, thread-safe store for teams and their players. */
@Repository
public class TeamRepository {

    private final Map<String, Team> teams = new ConcurrentHashMap<>();
    private final Map<String, Player> players = new ConcurrentHashMap<>();

    public Team save(Team team) {
        teams.put(team.getId(), team);
        team.getPlayers().forEach(p -> players.put(p.getId(), p));
        return team;
    }

    public Optional<Team> findById(String id) {
        return Optional.ofNullable(teams.get(id));
    }

    public List<Team> findAll() {
        return teams.values().stream().sorted(Comparator.comparing(Team::getName)).toList();
    }

    public Optional<Player> findPlayer(String playerId) {
        return Optional.ofNullable(players.get(playerId));
    }

    public List<Player> findAllPlayers() {
        return List.copyOf(players.values());
    }
}
