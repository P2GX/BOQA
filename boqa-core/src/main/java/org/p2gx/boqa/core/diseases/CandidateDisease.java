package org.p2gx.boqa.core.diseases;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.boqa.core.algorithm.BoqaCounts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * The Candidate Disease in the Blended setting is either a single Mendelian disease or a mix of two or more such diseases.
 * 'permits' restricts implementation strictly to these two scenarios. The class can be used like this:
 * <pre>
 * public void processDisease(CandidateDisease candidate) {
 *    switch (candidate) {
 *       case CandidateDisease.Single s -> {
 *          System.out.println("Processing single disease: " + s.disease().diseaseLabel());
 *       }
 *       case CandidateDisease.Blended b -> {
 *         System.out.println("Processing " + b.components().size() + " blended diseases");
 *         List<TargetDisease> list = b.components(); 
 *      }
 *     }
 *  }
 * </pre>
 */
public sealed interface CandidateDisease permits CandidateDisease.Single, CandidateDisease.Blended {
    Logger LOGGER = LoggerFactory.getLogger("org.p2gx.boqa.core.diseases.CandidateDisease");
    // This is the merged disease (or the single Mendelian disease for "Single"), i.e., the disease we will be testing
    TargetDisease finalDisease();
    Set<TermId> observedHpoTermids();
    
    default String diseaseId() {
        return finalDisease().diseaseId();
    }
    default String diseaseLabel() {
        return finalDisease().diseaseLabel();
    }


    /**
     * A single Mendelian disease.
     */
    record Single(TargetDisease disease) implements CandidateDisease {
        @Override
        public TargetDisease finalDisease() {
            return disease;
        }

        @Override
        public Set<TermId> observedHpoTermids() {
            return disease.observedHpoIds();
        }
    }

    /**
     * A list of two or more Mendelian diseases (related to distinct genes) with a final blended disease.
     */
    record Blended(List<TargetDisease.Gene> components, TargetDisease.Gene finalDisease) implements CandidateDisease {
        public Blended {
            if (components == null || components.size() < 2) {
                throw new IllegalArgumentException("Blended diseases must contain at least 2 components");
            }
        }

        String geneId() {
            return finalDisease().geneId();
        }
        String geneSymbol() {
            return finalDisease().geneSymbol();
        }

        @Override
        public Set<TermId> observedHpoTermids() {
            return finalDisease().observedHpoIds();
        }
    }

    private static CandidateDisease.Blended getMelded(List<TargetDisease.Gene> diseaseNplet) {
        if (diseaseNplet.size() != 2) {
           LOGGER.warn("Only doublets of target diseases are supported, found: " + diseaseNplet.size());
        }
        Set<TermId> combinedObservedHpoIds = diseaseNplet.stream()
                .flatMap(d -> d.observedHpoIds().stream())
                .collect(Collectors.toSet());

        String diseaseId = diseaseNplet.stream()
                .map(TargetDisease.Gene::diseaseId)
                .collect(Collectors.joining("-"));

        String diseaseLabel = diseaseNplet.stream()
                .map(TargetDisease.Gene::diseaseLabel)
                .collect(Collectors.joining("-"));

        String geneId = diseaseNplet.stream()
                .map(TargetDisease.Gene::geneId)
                .collect(Collectors.joining("-"));

        String symbol = diseaseNplet.stream()
                .map(TargetDisease.Gene::geneSymbol)
                .collect(Collectors.joining("-"));

        TargetDisease.Gene finalDisease = new TargetDisease.Gene(
                diseaseId, diseaseLabel,
                geneId, symbol,
                combinedObservedHpoIds);
        return new Blended(diseaseNplet, finalDisease);
    }

    static List<CandidateDisease> createSingleDiseaseCandidates(List<? extends TargetDisease> targetDiseases) {
        return targetDiseases.stream()
                .map(CandidateDisease.Single::new)
                .map(candidate -> (CandidateDisease) candidate)
                .toList();
    }

    static List<CandidateDisease> createCandidateDiseases(List<TargetDisease.Gene> targetDiseases) {
        List<CandidateDisease> candidates = createSingleDiseaseCandidates(targetDiseases);

        List<List<TargetDisease.Gene>> diseaseNplet = makeAllowedCombinations(targetDiseases);
        // Create candidate disease pairs except if a disease pair has the same gene
        diseaseNplet.forEach(pair -> {
            if (hasDistinctGenes(pair)) {
                candidates.add(getMelded(pair));
            }
        });
        return candidates;
    }

    static List<List<TargetDisease.Gene>> makeAllowedCombinations(List<TargetDisease.Gene> targetDiseases){
        // Only paired combinations supported for now
        List<List<TargetDisease.Gene>> diseasePairs = IntStream.range(0, targetDiseases.size())
                .boxed()
                .flatMap(i -> IntStream.range(i + 1, targetDiseases.size())
                        .mapToObj(j -> List.of(targetDiseases.get(i), targetDiseases.get(j))))
                .toList();
        return diseasePairs;
    }

    private static boolean hasDistinctGenes(List<TargetDisease.Gene> diseases) {
        return diseases.stream()
                .map(TargetDisease.Gene::geneSymbol)
                .distinct()
                .count() == diseases.size();
    }
}