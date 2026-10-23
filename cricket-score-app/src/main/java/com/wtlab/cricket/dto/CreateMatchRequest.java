package com.wtlab.cricket.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/**
 * @param overs     overs per innings, defaults to 20
 * @param startTime scheduled start, defaults to 30 minutes from now
 */
public record CreateMatchRequest(
        @NotBlank String teamAId,
        @NotBlank String teamBId,
        @NotBlank String venue,
        @Min(1) @Max(50) Integer overs,
        Instant startTime) {
}
