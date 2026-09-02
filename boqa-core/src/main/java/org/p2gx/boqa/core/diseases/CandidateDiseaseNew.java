package org.p2gx.boqa.core.diseases;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


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
public sealed interface CandidateDiseaseNew permits CandidateDiseaseNew.SingleDiseaseNew, CandidateDiseaseNew.BlendedDiseaseNew {
    Logger LOGGER = LoggerFactory.getLogger("org.p2gx.boqa.core.diseases.CandidateDisease");
    // This is the merged disease (or the single Mendelian disease for "Single"), i.e., the disease we will be testing
    Set<TermId> observedHpoTermids();
    
   /**
     * A single Mendelian disease.
     */
    record SingleDiseaseNew(TargetDisease disease) implements CandidateDiseaseNew {
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
    record BlendedDiseaseNew(Set<TargetDisease.PhenotypeAndGene> components) implements CandidateDiseaseNew {
        public BlendedDiseaseNew {
            if (components == null || components.size() < 2) {
                throw new IllegalArgumentException("Blended diseases must contain at least 2 components");
            }
        }
        Set<String> geneId() {
            return components.stream().map(TargetDisease.PhenotypeAndGene::geneId).collect(Collectors.toSet());
        }
        Set<String> geneSymbol() {
            return components.stream().map(TargetDisease.PhenotypeAndGene::geneSymbol).collect(Collectors.toSet());
        }

        public Set<TargetDisease> finalDisease() {
            return components.stream()
                    .map(g -> (TargetDisease) g)
                    .collect(Collectors.toSet());
        }

        @Override
        public Set<TermId> observedHpoTermids() {
            return components.stream().flatMap(
                    d->d.observedHpoIds().stream()).collect(Collectors.toSet());
        }
    }

    static List<CandidateDiseaseNew> createSingleDiseaseCandidates(List<? extends TargetDisease> targetDiseases) {
        return targetDiseases.stream()
                .map(SingleDiseaseNew::new)
                .map(candidate -> (CandidateDiseaseNew) candidate)
                .toList();
    }

    static List<CandidateDiseaseNew> createCandidateDiseases(List<TargetDisease.PhenotypeAndGene> targetDiseases) {
        List<CandidateDiseaseNew> candidates = createSingleDiseaseCandidates(targetDiseases);

        List<Set<TargetDisease.PhenotypeAndGene>> diseaseNplet = makeAllowedCombinations(targetDiseases);
        // Create candidate disease pairs except if a disease pair has the same gene
        diseaseNplet.forEach(pair -> {
            if (hasDistinctGenes(pair)) {
                candidates.add(new BlendedDiseaseNew(pair));
            }
        });
        return candidates;
    }

    static List<Set<TargetDisease.PhenotypeAndGene>> makeAllowedCombinations(List<TargetDisease.PhenotypeAndGene> targetDiseases){
        // Only paired combinations supported for now
        List<Set<TargetDisease.PhenotypeAndGene>> diseasePairs = IntStream.range(0, targetDiseases.size())
                .boxed()
                .flatMap(i -> IntStream.range(i + 1, targetDiseases.size())
                        .mapToObj(j -> Set.of(targetDiseases.get(i), targetDiseases.get(j))))
                .toList();
        return diseasePairs;
    }

    private static boolean hasDistinctGenes(Set<TargetDisease.PhenotypeAndGene> diseases) {
        return diseases.stream()
                .map(TargetDisease.PhenotypeAndGene::geneSymbol)
                .distinct()
                .count() == diseases.size();
    }
}