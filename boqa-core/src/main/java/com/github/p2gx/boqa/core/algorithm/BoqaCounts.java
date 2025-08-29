package com.github.p2gx.boqa.core.algorithm;

/**
 * Java record containing the exponents of alpha, beta, 1-alpha, 1-beta
 * determined for a disease.
 * <p>
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 * @author <a href="mailto:leonardo.chimirri@bih-charite.de">Leonardo Chimirri</a>
 */
public record BoqaCounts(String diseaseId, String diseaseLabel, int tpBoqaCount, int fpBoqaCount, int tnBoqaCount, int fnBoqaCount) {
    public BoqaCounts {
        if (tpBoqaCount < 0 || fpBoqaCount < 0 || tnBoqaCount < 0 || fnBoqaCount < 0) {
            throw new java.lang.IllegalArgumentException("Counts must be greater than zero!");
        }
    }
}