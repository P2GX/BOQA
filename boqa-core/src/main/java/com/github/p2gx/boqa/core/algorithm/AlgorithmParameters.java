package com.github.p2gx.boqa.core.algorithm;

/**
 * Global BOQA configuration parameters.
 * Alpha and beta are fixed across all patients for a single run.
 */
public final class AlgorithmParameters {
    //public static final double ALPHA = 0.0005;
    //public static final double ALPHA = 1.0 / 19077;
    //public static final double ALPHA = 1.0 / 19077/16;
    public static final double ALPHA = 1.0 / 19077/64;
    //public static final double ALPHA = 0.05;
    //public static final double BETA = 0.6;
    public static final double BETA = 0.8;

    private AlgorithmParameters() {} // prevent instantiation
}