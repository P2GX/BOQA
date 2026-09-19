package org.p2gx.boqa.core.analysis;

import java.util.List;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.p2gx.boqa.core.Counter;
import org.p2gx.boqa.core.DiseaseData;
import org.p2gx.boqa.core.PatientData;
import org.p2gx.boqa.core.algorithm.AlgorithmParameters;
import org.p2gx.boqa.core.algorithm.BoqaCounts;
import org.p2gx.boqa.core.algorithm.SetCounter;
import org.p2gx.boqa.core.diseases.*;
import org.p2gx.boqa.core.internal.OntologyTraverser;
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
     *   {@link org.p2gx.boqa.core.algorithm.SetCounter}</li>
     *   <li>Calculate log probability using
     *   TODO
     * </ol>
     *
     * @param patientData  Query data (symptoms/features observed in a patient)
     * @return A {@link PatientAnalysisResult} containing the patient data along with
     * counts and raw log scores for each HPOA-annotated disease.
     */
    public List<CandidateResult> computeBlendedBoqaResults(
        PatientData patientData, 
        List<TargetDisease.PhenotypeAndGene> targetDiseaseList) {

        // Now the counter is really only computing counts (though it needs HPO to do the induced HPOs)
        OntologyTraverser ontologyTraverser = new OntologyTraverser(hpo);
        Counter counter = new SetCounter(ontologyTraverser, patientData.getObservedTerms());
        List<CandidateDisease> diseaseCandidateList = CandidateDisease.createCandidateDiseases(targetDiseaseList);

        //TODO fix resultsLimit
        int resultsLimit = 100000;
        return BoqaPatientAnalyzer.computeBoqaResults(
                patientData, counter, resultsLimit, params, diseaseCandidateList
        );
    }
}