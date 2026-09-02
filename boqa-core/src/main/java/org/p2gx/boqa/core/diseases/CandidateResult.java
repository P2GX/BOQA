package org.p2gx.boqa.core.diseases;

import java.util.List;

import org.p2gx.boqa.core.algorithm.BoqaCountsNew;

/**
 * The sealed result type. This guarantees that your HTML generator 
 * only receives valid data states.
 */
public sealed interface CandidateResult extends Comparable<CandidateResult>
        permits CandidateResult.Single, CandidateResult.Blended {

    /** Assess improvement over best single disease. Only makes sense for the Blended, but it is easeir
     * to define in the interface, and the result must be false for a single-disease, which cannot have
     * a higher score than itself.
     */
    default boolean improvedComparedToBestSingleDisease() {
        return false;
    }
    double score();
    BoqaCountsNew counts();

//    default double score() {
//        return finalDiseaseModel().score();
//    }
//
//    default BoqaCountsNew counts() {
//        return finalDiseaseModel().counts();
//    }

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
    default int compareTo(CandidateResult other) {
        boolean isThisNaN = Double.isNaN(this.score());
        boolean isOtherNaN = Double.isNaN(other.score());

        if (isThisNaN && isOtherNaN) {
            return 0;
        }
        if (isThisNaN) {
            return 1;
        }
        if (isOtherNaN) {
            return -1;
        }
        return Double.compare(other.score(), this.score());
    }


    /**
     * Variant 1: Exactly one disease and one set of counts.
     */
    record Single(DiseaseComponent component) implements CandidateResult {
        @Override
        public double score() {
            return component.score();
        }

        @Override
        public BoqaCountsNew counts() {
            return component.counts();
        }
    }

    /**
     * Variant 2: Multiple distinct components, plus the final melded result.
     */
    record Blended(
            List<DiseaseComponent> components,
            BoqaCountsNew counts,
            double score
    ) implements CandidateResult {
//        public Blended {
//            if (components == null || components.size() < 2) {
//                throw new IllegalArgumentException("Blended results must have at least 2 components");
//            }
//        }
        @Override
        public boolean improvedComparedToBestSingleDisease() {
            return naiveImprovement();
        }

        private boolean naiveImprovement() {
            double maxSingleScore = components().stream()
                .mapToDouble(DiseaseComponent::score)
                .max()
                .orElse(0.0);
            return score > maxSingleScore;
        }

    }
}