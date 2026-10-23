package com.wtlab.cricket.dto;

import com.wtlab.cricket.model.DismissalType;
import com.wtlab.cricket.model.ExtraType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * A manual score update for one delivery.
 *
 * @param runs      runs off the bat, or runs taken on a wide / as byes (0-6)
 * @param extra     NONE (default), WIDE, NO_BALL, BYE or LEG_BYE
 * @param dismissal how the striker got out, or null if not out
 */
public record BallRequest(
        @Min(0) @Max(6) int runs,
        ExtraType extra,
        DismissalType dismissal) {

    public ExtraType extraOrNone() {
        return extra == null ? ExtraType.NONE : extra;
    }
}
