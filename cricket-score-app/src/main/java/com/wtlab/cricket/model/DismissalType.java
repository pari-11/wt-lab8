package com.wtlab.cricket.model;

public enum DismissalType {
    BOWLED,
    CAUGHT,
    LBW,
    RUN_OUT,
    STUMPED;

    /** Run outs are not credited to the bowler. */
    public boolean creditsBowler() {
        return this != RUN_OUT;
    }
}
