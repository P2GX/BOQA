package com.github.p2gx.boqa.core;

/**
 * Global BOQA configuration parameters.
 * Alpha and beta are fixed across all patients for a single run.
 */
public final class AlgorithmParameters {
    public static final double ALPHA = 1.0 / 19077;
    public static final double BETA = 0.1;

    private AlgorithmParameters() {} // prevent instantiation
}