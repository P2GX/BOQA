package com.github.p2gx.boqa.core.algorithm;

/**
 * Global BOQA configuration parameters.
 * Alpha and beta are fixed (across all diseases and patients) for a single run.
 *
 * {@code ALPHA} represents the probability of a false positive, {@code BETA} that of a false negative.
 */
public final class AlgorithmParameters {
    public static final double ALPHA = 1.0 / 19077/32;
    public static final double BETA = 0.9;

    private AlgorithmParameters() {} // prevent instantiation
}