package com.wtlab.cricket.service;

import com.wtlab.cricket.model.Delivery;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/** Turns a delivery into a line of ball-by-ball commentary. */
@Component
public class CommentaryGenerator {

    private static final String[] WICKET = {"Big breakthrough!", "The crowd goes silent.", "That's a huge wicket.", "Got him!"};
    private static final String[] SIX = {"launched over long-on", "that's gone miles into the stands",
            "slog-swept high over deep midwicket", "lofted cleanly over extra cover"};
    private static final String[] FOUR = {"driven beautifully through the covers", "pulled hard, races away to the rope",
            "cut past point, no stopping that", "flicked off the pads to fine leg"};
    private static final String[] DOT = {"no run, defended solidly", "beaten outside off!", "dot ball, good length",
            "straight to the fielder"};
    private static final String[] SINGLE = {"worked to square leg", "pushed to long-off", "dabbed down to third man",
            "nudged into the leg side"};
    private static final String[] TWO = {"placed into the gap, they come back for two",
            "good running between the wickets"};
    private static final String[] WIDE = {"drifting down leg", "too far outside off"};

    public String describe(Delivery d) {
        String prefix = d.over() + " " + d.bowler().getShortName() + " to " + d.batter().getShortName() + ", ";
        if (d.wicket()) {
            return prefix + "OUT! " + d.dismissal() + ". " + pick(WICKET);
        }
        return prefix + switch (d.extra()) {
            case WIDE -> "wide, " + pick(WIDE) + (d.totalRuns() > 1 ? " (" + d.totalRuns() + " runs)" : "");
            case NO_BALL -> "NO BALL! Overstepped" + (d.batRuns() > 0 ? ", and " + d.batRuns() + " off the bat" : "");
            case BYE -> d.totalRuns() + (d.totalRuns() == 1 ? " bye" : " byes") + ", beats the keeper";
            case LEG_BYE -> d.totalRuns() + (d.totalRuns() == 1 ? " leg bye" : " leg byes") + ", off the pads";
            case NONE -> switch (d.batRuns()) {
                case 0 -> pick(DOT);
                case 1 -> "1 run, " + pick(SINGLE);
                case 2 -> "2 runs, " + pick(TWO);
                case 3 -> "3 runs, chased down just inside the rope";
                case 4 -> "FOUR! " + pick(FOUR);
                case 6 -> "SIX! " + pick(SIX);
                default -> d.batRuns() + " runs";
            };
        };
    }

    private static String pick(String[] options) {
        return options[ThreadLocalRandom.current().nextInt(options.length)];
    }
}
