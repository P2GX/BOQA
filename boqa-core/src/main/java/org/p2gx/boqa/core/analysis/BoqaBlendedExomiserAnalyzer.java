package org.p2gx.boqa.core.analysis;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.p2gx.boqa.core.Counter;
import org.p2gx.boqa.core.DiseaseData;
import org.p2gx.boqa.core.PatientData;
import org.p2gx.boqa.core.algorithm.AlgorithmParameters;
import org.p2gx.boqa.core.algorithm.BoqaCounts;
import org.p2gx.boqa.core.algorithm.BoqaSetCounter;
import org.p2gx.boqa.core.diseases.BlendedDiseaseData;
import org.p2gx.boqa.core.diseases.DiseaseDataPhenolIngest;
import org.p2gx.boqa.core.patient.DiseaseDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Runs a single BOQA-blended analysis for one patient, intended to be called from Exomiser.
 *
 * <p>The caller supplies its candidate diseases as {@link TargetDisease}s, each carrying the gene
 * that made it a candidate. The candidates are paired with each other (each pair's HPO annotations
 * blended into one entry), and the patient is scored against the candidates and those blends. Two
 * candidates are only blended when their genes differ, so a single gene never explains both halves
 * of a blend.</p>
 *
 * <p>The heavy, reusable inputs (ontology, disease-phenotype annotations and algorithm parameters)
 * are supplied once at construction; {@link #analyze} is then called per patient. Instances are
 * immutable and the analysis is read-only, so a single instance may be reused across patients.</p>
 */
public final class BoqaBlendedExomiserAnalyzer {

    private final Ontology hpo;
    private final DiseaseData diseaseData;
    private final AlgorithmParameters params;

    /**
     * @param hpo      the HPO ontology
     * @param diseases the phenol disease-phenotype annotations; converted internally into the
     *                 plain {@link DiseaseData} used for scoring
     * @param params   BOQA algorithm parameters (alpha, beta)
     */
    public BoqaBlendedExomiserAnalyzer(Ontology hpo, HpoDiseases diseases, AlgorithmParameters params) {
        this.hpo = hpo;
        this.diseaseData = DiseaseDataPhenolIngest.of(hpo, diseases);
        this.params = params;
    }

    /**
     * Runs one BOQA-blended analysis for a single patient, anchored on the given candidate diseases.
     *
     * @param patient        the patient's observed and excluded HPO terms
     * @param anchorDiseases the candidate diseases to anchor the blend on, with their genes
     * @param resultsLimit   maximum number of top-scoring entries to return
     * @return the entries (candidates and their blends) ranked by score, highest first
     * @throws IllegalArgumentException if fewer than two anchor diseases are given, since pairing
     *                                  anchors against each other needs at least two, or if a
     *                                  disease is anchored more than once
     */
    public List<BlendedResult> analyze(PatientData patient, Set<TargetDisease> anchorDiseases, int resultsLimit) {
        if (anchorDiseases.size() < 2) {
            throw new IllegalArgumentException(
                    "Blended analysis requires at least two anchor diseases, but got: " + anchorDiseases);
        }

        // Keep only anchor diseases we actually have phenotype annotations for.
        Map<String, TargetDisease> anchorsByDiseaseId = new HashMap<>();
        for (TargetDisease anchor : anchorDiseases) {
            // target diseasaes are singletons!
            // TODO placeholder, in future we only want one, DiseaseInfo or DiseaseDTO or TargetDisease...
            Set<DiseaseDTO> fullId = Set.of(new DiseaseDTO(anchor.diseaseId(), anchor.diseaseLabel()));
            if (!diseaseData.getDiagnosisIds().contains(fullId)) {
                continue;
            }
            TargetDisease alreadyAnchored = anchorsByDiseaseId.put(anchor.diseaseId(), anchor);
            if (alreadyAnchored != null) {
                throw new IllegalArgumentException("Disease " + anchor.diseaseId() + " is anchored twice, on genes "
                        + alreadyAnchored.geneSymbol() + " and " + anchor.geneSymbol()
                        + "; anchor each disease on a single gene!");
            }
        }

        // Pair the anchor diseases against each other, blending only those with differing genes.
        BlendedDiseaseData blendedDiseaseData = new BlendedDiseaseData(
                diseaseData, anchorsByDiseaseId.keySet(),
                BlendedDiseaseData.PairingStrategy.ANCHOR_VS_ANCHOR,
                differingGenes(anchorsByDiseaseId));

        Counter counter = new BoqaSetCounter(blendedDiseaseData, hpo);

        // Score every entry: a blend reports its components' counts, so those must not be cut off
        // before the results are assembled. The limit is applied to the assembled results instead.
        BoqaAnalysisResult scoredEntries = BoqaPatientAnalyzer.computeBoqaResults(
                patient, counter, blendedDiseaseData.size(), params);

        // TODO unnecessary in new framework, could be for item in setof DiseaseDTO...
        Map<String, BoqaCounts> countsByDiseaseId = new HashMap<>();
        for (BoqaResult scoredEntry : scoredEntries.boqaResults()) {
            countsByDiseaseId.put(scoredEntry.counts().diseases(), scoredEntry.counts());
        }

        // The scored entries are already ranked by score, so assembling them in order keeps that ranking.
        return scoredEntries.boqaResults().stream()
                .map(scoredEntry -> assembleResult(scoredEntry, blendedDiseaseData, anchorsByDiseaseId, countsByDiseaseId))
                .limit(resultsLimit)
                .toList();
    }

    /** Allows two anchor diseases to blend only when different genes made them candidates. */
    private static BiPredicate<String, String> differingGenes(Map<String, TargetDisease> anchorsByDiseaseId) {
        return (diseaseId1, diseaseId2) ->
                anchorsByDiseaseId.get(diseaseId1).geneId() != anchorsByDiseaseId.get(diseaseId2).geneId();
    }

    /** Joins a scored entry to the diseases it is made of, and to each of their counts. */
    private static BlendedResult assembleResult(BoqaResult scoredEntry, BlendedDiseaseData blendedDiseaseData,
                                                Map<String, TargetDisease> anchorsByDiseaseId,
                                                Map<String, BoqaCounts> countsByDiseaseId) {
        // TODO unnecessary splitter
        List<String> componentIds = blendedDiseaseData.componentsOf(scoredEntry.counts().diseaseId());
        List<TargetDisease> components = componentIds.stream().map(anchorsByDiseaseId::get).toList();
        // if len DiseaseInfo > 1, for each DiseaseInfo in a BoqaResult also get the BoqaCounts of the subcomponents
        List<BoqaCounts> componentCounts = componentIds.stream().map(countsByDiseaseId::get).toList();
        return new BlendedResult(components, componentCounts, scoredEntry.counts(), scoredEntry.boqaScore());
    }
}
