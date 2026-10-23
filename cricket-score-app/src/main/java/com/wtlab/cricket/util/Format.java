package com.wtlab.cricket.util;

public final class Format {

    private Format() {
    }

    public static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /** 75 legal balls -> "12.3" */
    public static String overs(int legalBalls) {
        return (legalBalls / 6) + "." + (legalBalls % 6);
    }

    /** Runs per over. */
    public static double rate(int runs, int legalBalls) {
        return legalBalls == 0 ? 0.0 : round2(runs * 6.0 / legalBalls);
    }

    public static double strikeRate(int runs, int balls) {
        return balls == 0 ? 0.0 : round2(runs * 100.0 / balls);
    }

    public static String surname(String fullName) {
        int space = fullName.lastIndexOf(' ');
        return space < 0 ? fullName : fullName.substring(space + 1);
    }
}
