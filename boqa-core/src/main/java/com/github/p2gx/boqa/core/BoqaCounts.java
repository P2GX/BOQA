package com.github.p2gx.boqa.core;

/**
 * Java record containing the false-positive, false-negative, true-negative and true-positive counts
 * determined for a disease.
 * <p>
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 */
//
public record BoqaCounts(String diseaseId, int tp, int fp, int tn, int fn) {
    public BoqaCounts {
        if (tp < 0 || fp < 0 || tn < 0 || fn < 0) {
            throw new java.lang.IllegalArgumentException("Counts must be greater than zero!");
        }
    }
}