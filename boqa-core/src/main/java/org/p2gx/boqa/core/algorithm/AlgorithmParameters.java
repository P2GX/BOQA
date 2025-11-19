package org.p2gx.boqa.core.algorithm;

/**
 * Global BOQA configuration parameters.
 * Alpha and beta are fixed (across all diseases and patients) for a single run.
 *
 * {@code ALPHA} represents the probability of a false positive, {@code BETA} that of a false negative.
 */
public final class AlgorithmParameters {
    private static final double DEFAULT_ALPHA = 1.0 / 19077/32;
    private static final double DEFAULT_BETA = 0.9;

    // Keep these for backward compatibility with existing code
    public static final double ALPHA = DEFAULT_ALPHA;
    public static final double BETA = DEFAULT_BETA;

    private final double alpha;
    private final double beta;

    private AlgorithmParameters(double alpha, double beta) {
        this.alpha = alpha;
        this.beta = beta;
    }

    /**
     * Create parameters with custom alpha and beta values.
     * If a value is null, the default will be used.
     *
     * @param alpha the alpha parameter (false positive probability), or null for default
     * @param beta the beta parameter (false negative probability), or null for default
     * @return AlgorithmParameters instance
     * @throws IllegalArgumentException if alpha or beta is not in the range (0, 1)
     */
    public static AlgorithmParameters create(Float alpha, Float beta) {
        double a = (alpha != null) ? alpha : DEFAULT_ALPHA;
        double b = (beta != null) ? beta : DEFAULT_BETA;

        // Validate alpha
        if (a <= 0.0 || a >= 1.0) {
            throw new IllegalArgumentException(
                    String.format("Alpha must be in the range (0, 1), exclusive. Got: %f", a)
            );
        }

        // Validate beta
        if (b <= 0.0 || b >= 1.0) {
            throw new IllegalArgumentException(
                    String.format("Beta must be in the range (0, 1), exclusive. Got: %f", b)
            );
        }

        return new AlgorithmParameters(a, b);
    }

    /**
     * Create parameters using the default values.
     */
    public static AlgorithmParameters createDefault() {
        return new AlgorithmParameters(DEFAULT_ALPHA, DEFAULT_BETA);
    }

    public double getAlpha() {
        return alpha;
    }

    public double getBeta() {
        return beta;
    }
}