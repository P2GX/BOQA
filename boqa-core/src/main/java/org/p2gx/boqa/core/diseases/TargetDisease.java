package org.p2gx.boqa.core.diseases;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.boqa.core.analysis.BoqaPatientAnalyzer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents a disease target which can either be a pure disease-level target 
 * or a more specific gene-disease target.
 */
public sealed interface TargetDisease permits TargetDisease.PhenotypeOnly, TargetDisease.PhenotypeAndGene {
    
    String diseaseId();
    String diseaseLabel();
    @JsonIgnore
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

    static List<PhenotypeAndGene> distinctTargetDiseases(
            List<TargetDisease.PhenotypeAndGene> targetDiseases) {

        return targetDiseases.stream()
                .collect(Collectors.toMap(
                        disease -> new DiseaseGeneKey(
                                disease.diseaseId(),
                                disease.geneId()),
                        Function.identity(),
                        (existing, duplicate) -> existing,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();
    }

    public record DiseaseGeneKey(
            String diseaseId,
            String geneId) {
    }
}