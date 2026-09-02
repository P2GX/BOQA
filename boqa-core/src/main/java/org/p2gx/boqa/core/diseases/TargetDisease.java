package org.p2gx.boqa.core.diseases;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Objects;
import java.util.Set;

/**
 * Represents a disease target which can either be a pure disease-level target 
 * or a more specific gene-disease target.
 */
public sealed interface TargetDisease permits TargetDisease.PhenotypeOnly, TargetDisease.PhenotypeAndGene {
    
    String diseaseId();
    String diseaseLabel();
    Set<TermId> observedHpoIds();

    /**
     * A disease target without associated gene information.
     */
    record PhenotypeOnly(
        String diseaseId,
        String diseaseLabel,
        Set<TermId> observedHpoIds
    ) implements TargetDisease {
        public PhenotypeOnly {
            Objects.requireNonNull(diseaseId, "diseaseId cannot be null");
            Objects.requireNonNull(diseaseLabel, "diseaseLabel cannot be null");
        }
    }

    /**
     * A disease target tied to a specific gene in addition to the disease id.
     */
    record PhenotypeAndGene(
        String diseaseId,
        String diseaseLabel,
        String geneId,
        String geneSymbol,
        Set<TermId> observedHpoIds
    ) implements TargetDisease {
        public PhenotypeAndGene {
            Objects.requireNonNull(diseaseId, "diseaseId cannot be null");
            Objects.requireNonNull(diseaseLabel, "diseaseLabel cannot be null");
            Objects.requireNonNull(geneId, "geneId cannot be null");
            Objects.requireNonNull(geneSymbol, "geneSymbol cannot be null");
        }
    }
}