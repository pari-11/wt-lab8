package com.wtlab.cricket.service;

import com.wtlab.cricket.dto.Views.PlayerStatsView;
import com.wtlab.cricket.dto.Views.TeamView;
import com.wtlab.cricket.model.Player;
import com.wtlab.cricket.model.Team;
import com.wtlab.cricket.repository.TeamRepository;
import com.wtlab.cricket.util.Format;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Career player statistics and leaderboards, aggregated across all matches. */
@Service
public class StatsService {

    private final TeamRepository teams;

    public StatsService(TeamRepository teams) {
        this.teams = teams;
    }

    public List<TeamView> listTeams() {
        return teams.findAll().stream().map(MatchViewMapper::team).toList();
    }

    public List<PlayerStatsView> teamPlayers(String teamId) {
        Team team = teams.findById(teamId).orElseThrow(() -> new NotFoundException("Team not found: " + teamId));
        return team.getPlayers().stream().map(this::toView).toList();
    }

    public List<PlayerStatsView> allPlayers() {
        return teams.findAllPlayers().stream()
                .sorted(Comparator.comparing(Player::getTeamId).thenComparing(Player::getId))
                .map(this::toView)
                .toList();
    }

    public PlayerStatsView player(String playerId) {
        return teams.findPlayer(playerId).map(this::toView)
                .orElseThrow(() -> new NotFoundException("Player not found: " + playerId));
    }

    public List<PlayerStatsView> leaderboard(String category, int limit) {
        Comparator<Player> order = switch (category.toLowerCase(Locale.ROOT)) {
            case "runs" -> Comparator.comparingInt(Player::getRuns);
            case "wickets" -> Comparator.comparingInt(Player::getWickets)
                    .thenComparing(Comparator.comparingInt(Player::getRunsConceded).reversed());
            case "sixes" -> Comparator.comparingInt(Player::getSixes);
            case "strikerate" -> Comparator.comparingDouble(
                    p -> p.getBallsFaced() < 20 ? 0.0 : (double) p.getRuns() / p.getBallsFaced());
            default -> throw new IllegalArgumentException(
                    "Unknown leaderboard category '" + category + "' (use runs, wickets, sixes or strikeRate)");
        };
        return teams.findAllPlayers().stream()
                .sorted(order.reversed())
                .limit(Math.max(1, Math.min(limit, 50)))
                .map(this::toView)
                .toList();
    }

    private PlayerStatsView toView(Player p) {
        Team team = teams.findById(p.getTeamId()).orElseThrow();
        int balls = p.getBallsBowled();
        return new PlayerStatsView(p.getId(), p.getName(), team.getShortName(), team.getColor(), p.getRole().name(),
                p.getMatches(), p.getRuns(), p.getBallsFaced(), p.getHighestScore(), p.getFours(), p.getSixes(),
                p.getDismissals() == 0 ? null : Format.round2((double) p.getRuns() / p.getDismissals()),
                Format.strikeRate(p.getRuns(), p.getBallsFaced()),
                p.getWickets(), Format.overs(balls), p.getRunsConceded(),
                balls == 0 ? null : Format.rate(p.getRunsConceded(), balls));
    }
}
