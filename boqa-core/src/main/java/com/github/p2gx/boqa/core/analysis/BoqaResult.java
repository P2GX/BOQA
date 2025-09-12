package com.github.p2gx.boqa.core.analysis;

import com.github.p2gx.boqa.core.algorithm.BoqaCounts;

/**
 * Record wrapping around {@link BoqaCounts} and combining it with a probability score.
 *
 * <p>This record pairs the {@link BoqaCounts} (which depend only on the ontology and patient data)
 * with the computed BOQA score (which additionally depends on algorithm parameters α and β, and
 * the normalization across all diseases being analyzed).
 *
 * <p>The separation is important because:
 * <ul>
 *   <li>{@code counts} are deterministic given patient HPOs and disease annotations</li>
 *   <li>{@code boqaScore} depends on algorithm parameters and list of diseases analyzed</li>
 * </ul>
 *
 * <p>Results are naturally ordered by score (highest first) for diagnostic ranking.
 *
 * @param counts    The BOQA counts for this disease.
 * @param boqaScore The normalized BOQA probability score (0.0 to 1.0), where higher values
 *                  indicate greater diagnostic likelihood.
 *
 * @todo add check/handling for boqaScore = NaN
 * @todo add test to check ordering
 */
public record BoqaResult(BoqaCounts counts, double boqaScore) implements Comparable<BoqaResult> {
    /**
     * Compares BoqaResults by score in descending order (highest score first).
     *
     * <p>This natural ordering places the most likely diagnoses at the beginning
     * of sorted collections.
     *
     * @param other the BoqaResult to compare to
     * @return negative if this score is higher, positive if lower, zero if equal
     */
    @Override
    public int compareTo(BoqaResult other) {
        return Double.compare(other.boqaScore(), this.boqaScore);
    }
}

