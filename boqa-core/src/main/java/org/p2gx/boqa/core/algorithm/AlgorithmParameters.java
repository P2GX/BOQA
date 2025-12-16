package org.p2gx.boqa.core.algorithm;

/**
 * Global BOQA configuration parameters.
 * Alpha and beta are fixed (across all diseases and patients) for a single run.
 *
 * {@code ALPHA} represents the probability of a false positive, {@code BETA} that of a false negative.
 */
public final class AlgorithmParameters {
    private static final double DEFAULT_ALPHA = 1.0 / 19077;
    private static final double DEFAULT_BETA = 0.9;
    private static final double DEFAULT_TEMPERATURE = 1.0;

    private final double alpha;
    private final double beta;
    private final double temperature;
    private final double logAlpha;
    private final double logBeta;
    private final double logOneMinusAlpha;
    private final double logOneMinusBeta;

    private AlgorithmParameters(double alpha, double beta, double temperature) {
        this.alpha = alpha;
        this.beta = beta;
        this.temperature = temperature;
        this.logAlpha = Math.log(alpha);
        this.logBeta = Math.log(beta);
        this.logOneMinusAlpha = Math.log(1-alpha);
        this.logOneMinusBeta = Math.log(1-beta);
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
    public static AlgorithmParameters create(Double alpha, Double beta, Double temperature) {
        double a = (alpha != null) ? alpha : DEFAULT_ALPHA;
        double b = (beta != null) ? beta : DEFAULT_BETA;
        double t = (temperature != null) ? temperature : DEFAULT_TEMPERATURE;
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
        // Validate temperature
        if (t < 1.0) {
            throw new IllegalArgumentException(
                    String.format("Temperature must be in the range [1, infinity). Got: %f", t)
            );
        }
        return new AlgorithmParameters(a, b, t);
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
    public double getLogAlpha() {
        return logAlpha;
    }
    public double getLogBeta() {
        return logBeta;
    }
    public double getLogOneMinusAlpha() {
        return logOneMinusAlpha;
    }
    public double getLogOneMinusBeta() {
        return logOneMinusBeta;
    }
}