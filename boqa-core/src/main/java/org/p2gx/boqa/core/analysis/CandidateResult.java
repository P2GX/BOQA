package org.p2gx.boqa.core.analysis;

import java.util.List;
import java.util.Set;

import org.p2gx.boqa.core.algorithm.BoqaCounts;
import org.p2gx.boqa.core.diseases.TargetDisease;

/**
 * The sealed result type. This guarantees that your HTML generator 
 * only receives valid data states.
 */
public sealed interface CandidateResult extends Comparable<CandidateResult>
        permits CandidateResult.SingleResult, CandidateResult.BlendedResult {

    /** Assess improvement over best single disease. Only makes sense for the Blended, but it is easeir
     * to define in the interface, and the result must be false for a single-disease, which cannot have
     * a higher score than itself.
     */
    default boolean improvedComparedToBestSingleDisease() {
        return false;
    }
    double score();
    BoqaCounts counts();
    Set<TargetDisease>  finalDiseases();

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
    record SingleResult(AlgorithmResult singleDiseaseResult) implements CandidateResult {
        @Override
        public  Set<TargetDisease>  finalDiseases(){ return singleDiseaseResult.candidateDisease().diseases();}
        @Override
        public double score() {
            return singleDiseaseResult.boqaScore();
        }
        @Override
        public BoqaCounts counts() {
            return singleDiseaseResult.counts();
        }
    }

    /**
     * Variant 2: Multiple distinct componentOfBlendedResult, plus the final melded result.
     */
    record BlendedResult(
            List<AlgorithmResult> componentOfBlendedResult,
            AlgorithmResult blendedDisease
    ) implements CandidateResult {
//        public Blended {
//            if (componentOfBlendedResult == null || componentOfBlendedResult.size() < 2) {
//                throw new IllegalArgumentException("Blended results must have at least 2 componentOfBlendedResult");
//            }
//        }

        @Override
        public Set<TargetDisease> finalDiseases() {
            return blendedDisease.candidateDisease().diseases();
        }
        @Override
        public BoqaCounts counts() {
            return blendedDisease.counts();
        }
        @Override
        public double score() {
            return blendedDisease.boqaScore();
        }
        @Override
        public boolean improvedComparedToBestSingleDisease() {
            return naiveImprovement();
        }

        private boolean naiveImprovement() {
            double maxSingleScore = componentOfBlendedResult().stream()
                .mapToDouble(AlgorithmResult::boqaScore)
                .max()
                .orElse(0.0);
            return blendedDisease.boqaScore() > maxSingleScore;
        }

    }
}