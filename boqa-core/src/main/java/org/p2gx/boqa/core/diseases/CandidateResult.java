package org.p2gx.boqa.core.diseases;

import java.util.List;

/**
 * The sealed result type. This guarantees that your HTML generator 
 * only receives valid data states.
 */
public sealed interface CandidateResult permits CandidateResult.Single, CandidateResult.Blended {
    
    DiseaseComponent finalDiseaseModel();
    /** Assess improvement over best single disease. Only makes sence for the Blended, but it is easer
     * to define in the interface, and the result must be false for a single-disease, which cannot have
     * a higher score than itself.
     */
    default boolean improvedComparedToBestSingleDisease() {
        return false;
    }
    


    /**
     * Variant 1: Exactly one disease and one set of counts.
     */
    record Single(
        DiseaseComponent component
    ) implements CandidateResult {
        @Override
        public DiseaseComponent finalDiseaseModel() {
            return component;
        }

    }

    /**
     * Variant 2: Multiple distinct components, plus the final melded result.
     */
    record Blended(
        List<DiseaseComponent> components,
        DiseaseComponent finalDiseaseModel
    ) implements CandidateResult {
        public Blended {
            if (components == null || components.size() < 2) {
                throw new IllegalArgumentException("Blended results must have at least 2 components");
            }
        }

        @Override
        public
        boolean improvedComparedToBestSingleDisease() {
            return naiveImprovement();
        }

        private boolean naiveImprovement() {
            double meldedScore = finalDiseaseModel().score();
            double maxSingleScore = components().stream()
                .mapToDouble(DiseaseComponent::score)
                .max()
                .orElse(0.0);
            return meldedScore > maxSingleScore;
        }

    }
}