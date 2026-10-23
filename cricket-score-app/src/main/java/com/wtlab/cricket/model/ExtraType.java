package com.wtlab.cricket.model;

public enum ExtraType {
    NONE,
    WIDE,
    NO_BALL,
    BYE,
    LEG_BYE;

    /** Wides and no-balls have to be bowled again. */
    public boolean isLegal() {
        return this != WIDE && this != NO_BALL;
    }
}
