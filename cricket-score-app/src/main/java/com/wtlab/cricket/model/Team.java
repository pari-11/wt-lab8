package com.wtlab.cricket.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Team {

    private final String id;
    private final String name;
    private final String shortName;
    private final String color;
    /** Kept in batting order. */
    private final List<Player> players = new ArrayList<>();

    public Team(String id, String name, String shortName, String color) {
        this.id = id;
        this.name = name;
        this.shortName = shortName;
        this.color = color;
    }

    public Player addPlayer(String playerName, PlayerRole role) {
        Player player = new Player(id + "-" + (players.size() + 1), playerName, id, role);
        players.add(player);
        return player;
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    /** Bowling options with the specialist bowlers (lower order) first. */
    public List<Player> bowlers() {
        List<Player> bowlers = new ArrayList<>(players.stream().filter(p -> p.getRole().canBowl()).toList());
        Collections.reverse(bowlers);
        return bowlers;
    }

    public Player wicketKeeper() {
        return players.stream()
                .filter(p -> p.getRole() == PlayerRole.WICKET_KEEPER)
                .findFirst()
                .orElse(players.get(0));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public String getColor() {
        return color;
    }
}
