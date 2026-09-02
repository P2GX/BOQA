package org.p2gx.boqa.core.diseases;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Objects;
import java.util.Set;

/**
 * Represents a disease target which can either be a pure disease-level target 
 * or a more specific gene-disease target.
 */
public sealed interface TargetDisease permits TargetDisease.Phenotype, TargetDisease.Gene {
    
    String diseaseId();
    String diseaseLabel();
    Set<TermId> observedHpoIds();

    /**
     * A disease target without associated gene information.
     */
    record Phenotype(
        String diseaseId,
        String diseaseLabel,
        Set<TermId> observedHpoIds
    ) implements TargetDisease {
        public Phenotype {
            Objects.requireNonNull(diseaseId, "diseaseId cannot be null");
            Objects.requireNonNull(diseaseLabel, "diseaseLabel cannot be null");
        }
    }

    /**
     * A disease target tied to a specific gene in addition to the disease id.
     */
    record Gene (
        String diseaseId,
        String diseaseLabel,
        String geneId,
        String geneSymbol,
        Set<TermId> observedHpoIds
    ) implements TargetDisease {
        public Gene {
            Objects.requireNonNull(diseaseId, "diseaseId cannot be null");
            Objects.requireNonNull(diseaseLabel, "diseaseLabel cannot be null");
            Objects.requireNonNull(geneId, "geneId cannot be null");
            Objects.requireNonNull(geneSymbol, "geneSymbol cannot be null");
        }
    }
}