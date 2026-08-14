package org.p2gx.boqa.core.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.p2gx.boqa.core.Counter;
import org.p2gx.boqa.core.DiseaseData;
import org.p2gx.boqa.core.PatientData;
import org.p2gx.boqa.core.algorithm.AlgorithmParameters;
import org.p2gx.boqa.core.algorithm.BlendedCounter;
import org.p2gx.boqa.core.algorithm.BoqaCounts;
import org.p2gx.boqa.core.diseases.CandidateDisease;
import org.p2gx.boqa.core.diseases.CandidateResult;
import org.p2gx.boqa.core.diseases.DiseaseComponent;
import org.p2gx.boqa.core.diseases.DiseaseDataPhenolIngest;
import org.p2gx.boqa.core.diseases.TargetDisease;
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
        List<TargetDisease.Gene> targetDiseaseList) {
        //List<CandidateDisease> candidateDiseaseList = CandidateDisease.createCandidateDiseases(targetDiseaseList);
        List<CandidateResult> bbqResults = new ArrayList<>();
        Counter counter = new BlendedCounter(hpo, hpoDiseases, candidateDiseaseList);
        List<CandidateDisease.Single> singleDiseaseCandidateList = CandidateDisease.createSingleDiseaseCandidates(targetDiseaseList);
        Map< CandidateDisease, CandidateResult> singleResultMap = new HashMap<>();
        for (CandidateDisease cd : singleDiseaseCandidateList) {
            if (cd instanceof CandidateDisease.Blended) continue; // only analyse singles
            BoqaCounts bcounts = counter.computeBoqaCounts(cd.diseaseId(), patientData);
            double boqaScore = BoqaPatientAnalyzer.computeUnnormalizedLogProbability(params, bcounts);
            DiseaseComponent dcomponent = new DiseaseComponent(cd.finalDisease(), bcounts, boqaScore);
            CandidateResult result = new CandidateResult.Single(dcomponent);
            singleResultMap.put(cd, result);
        }

        for (CandidateDisease candidate : candidateDiseaseList) {
            if (candidate instanceof CandidateDisease.Blended blended) {
                TargetDisease finalDisease = blended.finalDisease();
                List<TargetDisease.Gene> components = blended.components();
                BoqaCounts bcounts = counter.computeBoqaCounts(finalDisease.diseaseId(), patientData); // result for the blended
                double blendedBoqaScore = BoqaPatientAnalyzer.computeUnnormalizedLogProbability(params, bcounts);
                DiseaseComponent blendedDisease = new DiseaseComponent(finalDisease, bcounts, blendedBoqaScore);
                // we also want to get the scores for the component diseases. 
                // there is probably a more efficient way, this is recalculating
                List<DiseaseComponent> dComponents = new ArrayList<>();
                for (var dc: components) {
                   // BoqaCounts singleDiseaseBoqaCounts = counter.computeBoqaCounts(dc.diseaseId(), patientData); 
                    //double singleDiseaseBoqaScore = BoqaPatientAnalyzer.computeUnnormalizedLogProbability(params, singleDiseaseBoqaCounts);
                    CandidateResult cresult = singleResultMap.get(dc);
                    if (cresult == null) {
                        System.err.println("[ERROR] Could not retrieved result for " + dc);
                    }
                    dComponents.add(new DiseaseComponent(dc, cresult.counts(), cresult.score()));
                }
                CandidateResult result = new CandidateResult.Blended(dComponents, blendedDisease);
                if (result.improvedComparedToBestSingleDisease()) {
                    bbqResults.add(result); // only record melded candidates that are better than the best single disease
                }
            }
                
            }
        }
        return bbqResults;
    }
}