package org.p2gx.boqa.core.diseases;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
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
    TargetDisease.Gene finalDisease();
    Set<String> observedHpoTermids();
    
    default String diseaseId() {
        return finalDisease().diseaseId();
    }
    default String diseaseLabel() {
        return finalDisease().diseaseLabel();
    }

    default String geneId() {
        return finalDisease().geneId();
    }

    default String geneSymbol() {
        return finalDisease().geneSymbol();
    }



    /**
     * A single Mendelian disease.
     */
    record Single(TargetDisease.Gene disease) implements CandidateDisease {
        @Override
        public TargetDisease.Gene finalDisease() {
            return disease;
        }

        @Override
        public Set<String> observedHpoTermids() {
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

        @Override
        public Set<String> observedHpoTermids() {
            return finalDisease().observedHpoIds();
        }

    }

    private static TargetDisease.Gene getMelded(List<TargetDisease.Gene> diseasePair) {
        if (diseasePair.size() != 2) {
            throw new PhenolRuntimeException("Unexpected length of pair od target diseases: " + diseasePair.size());
        }
        TargetDisease.Gene t1 = diseasePair.get(0);
        TargetDisease.Gene t2 = diseasePair.get(1);
        String diseaseId = t1.diseaseId() + "-" + t2.diseaseId();
        String diseaseLabel = t1.diseaseLabel() + "-" + t2.diseaseLabel();
        String geneId = t1.geneId() + "-" + t2.geneId();
        String symbol = t1.geneSymbol() + "-" + t2.geneSymbol();
        Set<String> combinedObserved = Stream.concat(
            t1.observedHpoIds().stream(),
            t2.observedHpoIds().stream()
            )
            .collect(Collectors.toSet());
        return new TargetDisease.Gene(diseaseId, diseaseLabel, geneId, symbol, combinedObserved);
    }

    public static List<CandidateDisease.Single> createSingleDiseaseCandidates(List<TargetDisease.Gene> targetDiseases) {
        return targetDiseases.stream()
            .map(td -> new CandidateDisease.Single(td))
            .toList();
    }



    public static List<CandidateDisease> createCandidateDiseases(List<TargetDisease.Gene> targetDiseases) {
        List<CandidateDisease> candidates = new ArrayList<>();
        // first add the singleton diseases
        for (var td: targetDiseases) {
            candidates.add(new CandidateDisease.Single(td));
        }
        // Now add all pairwise combinations
        List<List<TargetDisease.Gene>> diseasePairs = IntStream.range(0, targetDiseases.size())
            .boxed()
            .flatMap(i -> IntStream.range(i + 1, targetDiseases.size())
                .mapToObj(j -> List.of(targetDiseases.get(i), targetDiseases.get(j))))
            .toList();
        // Create candidate disease pairs except if a disease pair has the same gene
        diseasePairs.forEach(pair -> {
            if (!pair.get(0).geneSymbol().equals(pair.get(1).geneSymbol())) {
                TargetDisease.Gene meldedDisease = getMelded(pair); 
                candidates.add(new CandidateDisease.Blended(pair, meldedDisease));
            }
        });
        return candidates;
    }
}