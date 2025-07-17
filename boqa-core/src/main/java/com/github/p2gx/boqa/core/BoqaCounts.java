package com.github.p2gx.boqa.core;

/**
 * Java record containing the exponents of alpha, beta, 1-alpha, 1-beta
 * determined for a disease.
 * <p>
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 * @author <a href="mailto:leonardo.chimirri@bih-charite.de">Leonardo Chimirri</a>
 */
public record BoqaCounts(String diseaseId, int tpExponent, int fpExponent, int tnExponent, int fnExponent) {
    public BoqaCounts {
        if (tpExponent < 0 || fpExponent < 0 || tnExponent < 0 || fnExponent < 0) {
            throw new java.lang.IllegalArgumentException("Counts must be greater than zero!");
        }
    }
}