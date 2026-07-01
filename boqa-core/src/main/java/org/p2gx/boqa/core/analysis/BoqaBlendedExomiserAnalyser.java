package org.p2gx.boqa.core.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.p2gx.boqa.core.Counter;
import org.p2gx.boqa.core.PatientData;
import org.p2gx.boqa.core.algorithm.AlgorithmParameters;
import org.p2gx.boqa.core.algorithm.BoqaCounts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/* Interface with Exomiser that contains all data we need to make an Exomiser result.
We do not want to import the actual Exomiser library, but we want to make it easy to create objects that will
implement the PriorityResult interface from Exomiser. These objects have
1. int geneId();
2. String geneSymbol();
3. double score();
4. PriorityType priorityType();
5. default String getHTMLCode() {return "";}
6. Therefore, we need to get this information from the Exomiser
*/
final record TargetDisease(
    String diseaseId,
    String diseaseLabel,
    int geneId,
    String geneSymbol //, and variant data for HTML
) {

}


// This can be a single disease or have 2 or more blended diseases
final record BlendedDisease(
    List<TargetDisease> diseaseList,
    // either the melded or the original
    TargetDisease finalDisease
) {
    boolean isBlended() { return diseaseList.size()>1; }
    List<TargetDisease> getDiseaseList() { return diseaseList; }
    TargetDisease firstDisease() { return diseaseList.getFirst(); }
}

/** We need to return enough data to enable Exomiser to create the HTML output. */
final record BlendedResult(
    List<TargetDisease> diseaseList,
    TargetDisease finalDisease, 
    /* Boqa Counts for each component (or just one for a single disease) */
    List<BoqaCounts> boqaCountsList,
    /* Boqa counts for melded (or this is identical to the above if there is just one, i.e., single disease) */
    BoqaCounts finalCounts,
    double score
){

    boolean isBlended(){ return diseaseList.size()>1; }
    // if we have two blended diseases, boqCountsList has disease1, disease2, and disease1+2 in that order
    // alternatively, we dou

}




/**
 * Performs BOQA-blended analysis for a given query set of HPO terms (patient's data).
 * <p>
 * This class evaluates a single patient's phenotypic profile (HPO terms)
 * against all HPOA-annotated diseases and computes probability scores for diagnostic ranking.
 */
public class BoqaBlendedExomiserAnalyser {
      private static final Logger LOGGER = LoggerFactory.getLogger(BoqaBlendedExomiserAnalyser.class);



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
    public static List<BlendedResult> computeBlendedBoqaResults(
            PatientData patientData, 
            Ontology hpo,
            HpoDiseases diseases, 
            AlgorithmParameters params, 
            Set<TargetDisease> targetDiseaseIdSet) {
                List<String> targetDiseaseIds = targetDiseaseIdSet.stream()
                        .map(TargetDisease::diseaseId)
                        .toList();
                List<BlendedDisease> singleAndBlendedDiseaseList = List.of(); // create this list!
                List<BlendedResult> bbqResults = new ArrayList<>();
                for (BlendedDisease bd : singleAndBlendedDiseaseList) {
                    List<BoqaCounts> bcountsList = new ArrayList<>();
                    for (TargetDisease tdisease : bd.diseaseList()) {
                         BoqaCounts bc = counter.computeBoqaCounts(tdisease.diseaseId(), patientData);
                         bcountsList.add(bc);
                    }
                    BoqaCounts finalCounts = counter.computeBoqaCounts(bd.finalDisease(), patientData);
                    double score = 42.7; // calculate for melded or final
                    BlendedResult results = new BlendedResult(bd.diseaseList(), bd.finalDisease(), bcountsList, finalCounts, score);
                    bbqResults.add(results);
                }
                /// For each bl
                return bbqResults;
               
                // 1. Create Counter object using the HPO and the HpoDiseases that we get from Exomiser
                // 2. If we want to use this, then to blend OMIM:123456 and OMIM:654321 we can create a TermId OMIM:123456-654321
                // We need to create a Map<TermId, BlendedDisease> to store information about the blended diseases
                // 3. private final Map<TermId, Set<TermId>> diseaseLayers => Create this similar to existing code (for the target single diseases and the blended diseases)
                // 4. perform the algorithm as it is now, probably also add the Bayes factor threshold as option
                // 5. instead of returning a BoqaResult, return a list of BoqaResults and BoqaBlended results to Exomiser and use these to create what we
                // need to satisfy PriorityResult in Exomiser (the BoqaBlendedPrioritiser will create a BbqPriorityResult: BBQ=BOQA Blended Query)
                /*
                List<BoqaResult> allResults = counter.getDiseaseIds()
                .parallelStream() // fast: computes counts + scores in parallel
                .map(dId -> {
                    BoqaCounts bc = counter.computeBoqaCounts(dId, patientData);
                    double rawScore = computeUnnormalizedLogProbability(params, bc);
                    return new BoqaResult(bc, rawScore);
                })
                .toList();

        return new BoqaAnalysisResult(patientData, allResults);
         */
            }


}
