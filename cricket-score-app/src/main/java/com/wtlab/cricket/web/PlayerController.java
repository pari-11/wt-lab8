package com.wtlab.cricket.web;

import com.wtlab.cricket.dto.Views.PlayerStatsView;
import com.wtlab.cricket.dto.Views.TeamView;
import com.wtlab.cricket.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PlayerController {

    private final StatsService statsService;

    public PlayerController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/api/teams")
    public List<TeamView> teams() {
        return statsService.listTeams();
    }

    @GetMapping("/api/teams/{teamId}/players")
    public List<PlayerStatsView> teamPlayers(@PathVariable String teamId) {
        return statsService.teamPlayers(teamId);
    }

    @GetMapping("/api/players")
    public List<PlayerStatsView> players() {
        return statsService.allPlayers();
    }

    @GetMapping("/api/players/{playerId}")
    public PlayerStatsView player(@PathVariable String playerId) {
        return statsService.player(playerId);
    }

    /** ?category=runs|wickets|sixes|strikeRate&limit=5 */
    @GetMapping("/api/players/leaderboard")
    public List<PlayerStatsView> leaderboard(@RequestParam(defaultValue = "runs") String category,
                                             @RequestParam(defaultValue = "5") int limit) {
        return statsService.leaderboard(category, limit);
    }
}
