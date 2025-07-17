package com.github.p2gx.boqa.core;

/**
 * Java record containing the false-positive, false-negative, true-negative and true-positive counts
 * determined for a disease.
 * <p>
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 */
public record BoqaCounts(String diseaseId, int tpExponent, int fpExponent, int tnExponent, int fnExponent) {
    public BoqaCounts {
        if (tpExponent < 0 || fpExponent < 0 || tnExponent < 0 || fnExponent < 0) {
            throw new java.lang.IllegalArgumentException("Counts must be greater than zero!");
        }
    }
}