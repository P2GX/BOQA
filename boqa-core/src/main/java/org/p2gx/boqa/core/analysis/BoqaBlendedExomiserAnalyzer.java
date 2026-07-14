package org.p2gx.boqa.core.analysis;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.p2gx.boqa.core.Counter;
import org.p2gx.boqa.core.DiseaseData;
import org.p2gx.boqa.core.PatientData;
import org.p2gx.boqa.core.algorithm.AlgorithmParameters;
import org.p2gx.boqa.core.algorithm.BoqaSetCounter;
import org.p2gx.boqa.core.diseases.BlendedDiseaseData;
import org.p2gx.boqa.core.diseases.DiseaseDataPhenolIngest;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Runs a single BOQA-blended analysis for one patient, intended to be called from Exomiser.
 *
 * <p>The analysis mirrors the {@code ANCHOR_VS_ANCHOR} iteration of the CLI's blended
 * benchmark ({@code BlendedBenchmarkCommand}): the caller supplies the anchor diseases, which
 * are paired with each other (each pair's HPO annotations blended into one entry), and the
 * patient is scored against the anchor diseases and those blends. Only gene-disjoint pairs are
 * blended, so two diseases explained by the same gene are never combined.</p>
 *
 * <p>The heavy, reusable inputs (ontology, disease-phenotype annotations, disease-gene
 * associations and algorithm parameters) are supplied once at construction; {@link #analyze}
 * is then called per patient. Instances are immutable and the analysis is read-only, so a
 * single instance may be reused across patients.</p>
 */
public final class BoqaBlendedExomiserAnalyzer {

    private final Ontology hpo;
    private final DiseaseData diseaseData;
    private final Map<String, Set<String>> genesByDisease;
    private final AlgorithmParameters params;

    /**
     * @param hpo            the HPO ontology
     * @param diseases       the phenol disease-phenotype annotations; converted internally into
     *                       the plain {@link DiseaseData} used for scoring
     * @param genesByDisease disease ID to its associated gene IDs; used to decide which anchor
     *                       diseases may blend (only gene-disjoint pairs are allowed)
     * @param params         BOQA algorithm parameters (alpha, beta)
     */
    public BoqaBlendedExomiserAnalyzer(Ontology hpo,
                                       HpoDiseases diseases,
                                       Map<String, Set<String>> genesByDisease,
                                       AlgorithmParameters params) {
        this.hpo = hpo;
        this.diseaseData = DiseaseDataPhenolIngest.of(hpo, diseases);
        this.genesByDisease = genesByDisease;
        this.params = params;
    }

    /**
     * Runs one BOQA-blended analysis for a single patient, anchored on the given diseases.
     *
     * @param patient        the patient's observed and excluded HPO terms
     * @param anchorDiseases the disease IDs anchoring the blend (e.g. Exomiser's candidate diseases)
     * @param resultsLimit   maximum number of top-scoring (blended) diseases to return
     * @return the ranked BOQA results for the patient
     * @throws IllegalArgumentException if fewer than two anchor diseases are given, since
     *                                  pairing anchors against each other needs at least two
     */
    public BoqaAnalysisResult analyze(PatientData patient, Set<String> anchorDiseases, int resultsLimit) {
        if (anchorDiseases.size() < 2) {
            throw new IllegalArgumentException(
                    "ANCHOR_VS_ANCHOR analysis requires at least two anchor diseases, but got: " + anchorDiseases);
        }

        // Keep only anchor diseases we actually have phenotype annotations for.
        Set<String> annotatedAnchorDiseases = new HashSet<>(anchorDiseases);
        annotatedAnchorDiseases.retainAll(diseaseData.getDiseaseIds());

        // Pair the anchor diseases against each other, blending only gene-disjoint pairs.
        BlendedDiseaseData blendedDiseaseData = new BlendedDiseaseData(
                diseaseData, annotatedAnchorDiseases,
                BlendedDiseaseData.PairingStrategy.ANCHOR_VS_ANCHOR,
                BlendedDiseaseData.geneDisjointBlend(genesByDisease));

        Counter counter = new BoqaSetCounter(blendedDiseaseData, hpo);

        return BoqaPatientAnalyzer.computeBoqaResults(patient, counter, resultsLimit, params);
    }
}
