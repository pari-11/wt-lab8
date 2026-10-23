package com.wtlab.cricket.model;

public enum PlayerRole {
    BATTER,
    WICKET_KEEPER,
    ALL_ROUNDER,
    BOWLER;

    public boolean canBowl() {
        return this == BOWLER || this == ALL_ROUNDER;
    }
}
