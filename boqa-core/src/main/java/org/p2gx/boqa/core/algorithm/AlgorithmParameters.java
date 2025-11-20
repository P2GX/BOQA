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
    private static final double DEFAULT_TEMPERATURE = 50;

    // Keep these for backward compatibility with existing code
    public static final double ALPHA = DEFAULT_ALPHA;
    public static final double BETA = DEFAULT_BETA;

    private final double alpha;
    private final double beta;
    private final double temperature;

    private AlgorithmParameters(double alpha, double beta, double temperature) {
        this.alpha = alpha;
        this.beta = beta;
        this.temperature = temperature;
    }

    /**
     * Create parameters using the default values.
     */
    public static AlgorithmParameters create() {
        return create(DEFAULT_ALPHA, DEFAULT_BETA, DEFAULT_TEMPERATURE);
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
    public static AlgorithmParameters create(double alpha, double beta, double temperature) {
        // Validate alpha
        if (alpha <= 0.0 || alpha >= 1.0) {
            throw new IllegalArgumentException(
                    String.format("Alpha must be in the range (0, 1), exclusive. Got: %f", alpha)
            );
        }
        // Validate beta
        if (beta <= 0.0 || beta >= 1.0) {
            throw new IllegalArgumentException(
                    String.format("Beta must be in the range (0, 1), exclusive. Got: %f", beta)
            );
        }
        // Validate temperature
        if (temperature < 1.0) {
            throw new IllegalArgumentException(
                    String.format("Temperature must be in the range [1, infinity). Got: %f", temperature)
            );
        }
        return new AlgorithmParameters(alpha, beta, temperature);
    }

    public double getAlpha() {
        return alpha;
    }

    public double getBeta() {
        return beta;
    }

    public double getTemperature() {
        return temperature;
    }
}