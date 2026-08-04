package org.p2gx.boqa.core.diseases;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import static java.util.stream.Collectors.toSet;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;
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
    ExomiserTargetDisease finalDisease();
    Set<String> observedHpoTermids();
    
    default String diseaseId() {
        return finalDisease().diseaseId();
    }
    default String diseaseLabel() {
        return finalDisease().diseaseLabel();
    }
    /** A single Mendelian disease. */
    record Single(ExomiserTargetDisease finalDisease, Set<String> observedHpoTermids) implements CandidateDisease {
    }

    /** A list of two or more Mendelian diseases (related to distinct genes) with a final blended disease. */
    record Blended(List<ExomiserTargetDisease> components, ExomiserTargetDisease finalDisease, Set<String> observedHpoTermids) implements CandidateDisease {
        public Blended {
            if (components == null || components.size() < 2) {
                throw new IllegalArgumentException("Blended diseases must contain at least 2 components");
            }
        }
    }

    private static ExomiserTargetDisease getMelded(List<ExomiserTargetDisease> diseasePair) {
        if (diseasePair.size() != 2) {
            throw new PhenolRuntimeException("Unexpected length of pair od target diseases: " + diseasePair.size());
        }
        ExomiserTargetDisease t1 = diseasePair.get(0);
        ExomiserTargetDisease t2 = diseasePair.get(1);
        String diseaseId = t1.diseaseId() + "-" + t2.diseaseId();
        String diseaseLabel = t1.diseaseLabel() + "-" + t2.diseaseLabel();
        String geneId = t1.geneId() + "-" + t2.geneId();
        String symbol = t1.geneSymbol() + "-" + t2.geneSymbol();
        return new ExomiserTargetDisease(diseaseId, diseaseLabel, geneId, symbol);
    }

    static Optional<Set<String>> getObservedIds(String diseaseId, HpoDiseases hpoDiseases) {
       TermId tid = TermId.of(diseaseId);
       return hpoDiseases.diseaseById(tid)
            .map(disease -> StreamSupport.stream(disease.presentAnnotations().spliterator(), false)
                    .map(annot -> annot.id().getValue())
                    .collect(toSet()));
    }

    static List<CandidateDisease> createCandidateDiagnoses(
        List<ExomiserTargetDisease> exomiserTargetDiseases,
        HpoDiseases hpoDiseases
    ) {
        List<CandidateDisease> candidates = new ArrayList<>();
        // first add the singleton diseases
        for (var td: exomiserTargetDiseases) {
            getObservedIds(td.diseaseId(), hpoDiseases).ifPresentOrElse(
                observedIds -> candidates.add(new CandidateDisease.Single(td, observedIds)),
                () -> LOGGER.error("Could not retrieve disease model for '{}'", td.diseaseId())
            );
        }
        // Now add all pairwise combinations
        List<List<ExomiserTargetDisease>> diseasePairs = IntStream.range(0, exomiserTargetDiseases.size())
            .boxed()
            .flatMap(i -> IntStream.range(i + 1, exomiserTargetDiseases.size())
                .mapToObj(j -> List.of(exomiserTargetDiseases.get(i), exomiserTargetDiseases.get(j))))
            .toList();
        // Create candidate disease pairs except if a disease pair has the same gene
        diseasePairs.forEach(pair -> {
            if (!pair.get(0).geneSymbol().equals(pair.get(1).geneSymbol())) {
                ExomiserTargetDisease meldedDisease = getMelded(pair);
                Optional<Set<String>> opt0 = getObservedIds(pair.get(0).diseaseId(), hpoDiseases);
                Optional<Set<String>> opt1 = getObservedIds(pair.get(1).diseaseId(), hpoDiseases);
                if (opt0.isPresent() && opt1.isPresent()) {
                    Set<String> observed0 = opt0.get();
                    Set<String> observed1 = opt1.get();
                    Set<String> combinedObserved = new HashSet<>(opt0.get());
                    combinedObserved.addAll(opt1.get());
                    candidates.add(new CandidateDisease.Blended(pair, meldedDisease, combinedObserved));
                } else {
                    LOGGER.error("Could not retrieve observed phenotypes for melded disease: {} - {}", 
                        pair.get(0).diseaseId(), pair.get(1).diseaseId());
                }
            }
        });
        return candidates;
    }
}