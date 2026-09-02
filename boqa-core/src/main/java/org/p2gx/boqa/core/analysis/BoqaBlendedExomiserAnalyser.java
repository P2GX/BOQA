package org.p2gx.boqa.core.analysis;

import java.util.List;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.p2gx.boqa.core.Counter;
import org.p2gx.boqa.core.DiseaseData;
import org.p2gx.boqa.core.PatientData;
import org.p2gx.boqa.core.algorithm.AlgorithmParameters;
import org.p2gx.boqa.core.algorithm.BlendedCounter;
import org.p2gx.boqa.core.algorithm.BoqaCounts;
import org.p2gx.boqa.core.diseases.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs BOQA-blended analysis for a given query set of HPO terms (patient's data).
 * <p>
 * This class evaluates a single patient's phenotypic profile (HPO terms)
 * against all HPOA-annotated diseases and computes probability scores for diagnostic ranking.
 */
public class BoqaBlendedExomiserAnalyser {
    private static final Logger LOGGER = LoggerFactory.getLogger(BoqaBlendedExomiserAnalyser.class);
    private final Ontology hpo;
    private HpoDiseases hpoDiseases;
    private final DiseaseData diseaseData;
    private final AlgorithmParameters params;

     /**
     * @param hpo      the HPO ontology
     * @param diseases the phenol disease-phenotype annotations; converted internally into the
     *                 plain {@link DiseaseData} used for scoring
     * @param params   BOQA algorithm parameters (alpha, beta)
     */
    public BoqaBlendedExomiserAnalyser(Ontology hpo, HpoDiseases diseases) {
        this.hpo = hpo;
        this.hpoDiseases = diseases;
        this.diseaseData = DiseaseDataPhenolIngest.of(hpo, diseases);
        this.params = AlgorithmParameters.defaultParams();
    }

     /**
     * Computes unnormalized BOQA log scores (log(probabilities))
     * for each HPOA-annotated disease, given a query set of HPO terms (patient's data).
     * This function is also intended to be used in the BoqaPrioritiser of Exomiser.
     *
     * <p>For each HPOA-annotated disease, this method performs the following steps:
     * <ol>
     *   <li>Compute {@link BoqaCounts} using the provided
     *   {@link org.p2gx.boqa.core.algorithm.BoqaSetCounter}</li>
     *   <li>Calculate un-normalized log probability using
     *   {@link #computeUnnormalizedLogProbability(AlgorithmParameters, BoqaCounts)}</li>
     * </ol>
     *
     * @param patientData  Query data (symptoms/features observed in a patient)
     * @param counter      The counter object that computes BoqaCounts for each HPOA-annotated disease
     * @return A {@link BoqaAnalysisResult} containing the patient data along with
     * counts and raw log scores for each HPOA-annotated disease.
     */
    public List<CandidateResult> computeBlendedBoqaResults(
        PatientData patientData, 
        List<TargetDisease.PhenotypeAndGene> targetDiseaseList) {

        // Now the counter is really only computing counts (though it needs HPO to do the induced HPOs)
        Counter counter = new BlendedCounter(hpo);
        // TODO Equivalent in spirit to previous DiseaseData. Given some input, generate a representation of HPOA for
        //  diseases of interest. Here HPOA comes from TargetDisease, which should already inlcude the HPOs. We need a
        //  to create a DiseaseDataIngest that returns List<TargetDisease.Phenotype> to recover the previous pure BOQA
        List<CandidateDiseaseNew> diseaseCandidateList = CandidateDiseaseNew.createCandidateDiseases(targetDiseaseList);

        // At this point our design, which PNR liked, pretty much had one "action" happening, namely:
        // BoqaAnalysisResult result = BoqaPatientAnalyzer.computeBoqaResults(
        //                                ppkt, counter, limit, params)
        // In this way, all of the internals of the anlaysis happen in the core module, and not in this Exomiser
        // specific Analyser class. The class BoqaAnalysisResult holds results for all diseases, maybe this needs to
        // be reworked
        //TODO fix resultsLimit
        int resultsLimit = 100000;
        return BoqaPatientAnalyzerNew.computeBoqaResults(
                patientData, counter, resultsLimit, params, diseaseCandidateList
        );
    }
}