package com.github.p2gx.boqa.core.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.p2gx.boqa.core.PatientData;
import com.github.p2gx.boqa.core.algorithm.AlgorithmParameters;
import com.github.p2gx.boqa.core.algorithm.BoqaCounts;
import com.github.p2gx.boqa.core.algorithm.BoqaSetCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Bookkeeping class that holds the results of a Boqa Analysis for a single patient.
 *
 * <p>This class stores:
 * <ul>
 *   <li>The input {@link PatientData} used for analysis.</li>
 *   <li>Computed {@link BoqaCounts} for each disease.</li>
 *   <li>Associated BOQA scores, normalized across all diseases.</li>
 * </ul>
 *
 * <p>Provides methods for:
 * <ul>
 *   <li>Retrieving counts and scores for all diseases.</li>
 *   <li>Computing un-normalized and normalized BOQA probabilities from counts.</li>
 * </ul>
 *
 * <p>Scores are stored alongside counts in a {@link BoqaResult} record
 * to allow flexible experimentation without altering the primary counts structure.
 *
 * <p><strong>Thread-safety:</strong> Results are stored in a HashMap (mutable) without synchronization,
 * so multiple threads accessing it may cause problem. This class is not thread-safe. Results should be computed
 * and accessed in a single-threaded context or with external synchronization.
 */
public class AnalysisResults {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisResults.class);
    private PatientData patientData;
    private final int resultsLimit;
    /**
     * Extra record with wrapping around {@link BoqaCounts} and adding the score.
     *
     * <p> Useful to ease development phase without changing future code.
     *
     * @param counts    The BOQA counts for a disease.
     * @param boqaScore The normalized BOQA score for that disease.
     *
     * <p>
     * TODO add check/handling for boqaScore = NaN
     */
    public record BoqaResult(BoqaCounts counts, Double boqaScore) implements Comparable<BoqaResult>{
        @Override
        public int compareTo(BoqaResult other) {
            return other.boqaScore.compareTo(this.boqaScore);
        }
    }

    // TODO consider getting rid of state here and making the function static
    private final List<BoqaResult> resultsList = new ArrayList<>();
    public List<BoqaResult> getBoqaResults() {
        return resultsList;
    }

    /**
     * Constructs a new results container for a given patient.
     *
     * @param patientData The patient data used to compute BOQA scores.
     */
    public AnalysisResults(PatientData patientData, int resultsLimit) {
        this.patientData = patientData;
        this.resultsLimit = resultsLimit;
    }


    public PatientData getPatientData() {
        return patientData;
    }

    /**
     * Returns only a copy of the {@link BoqaCounts} for each disease, discarding scores.
     * <p>Useful for comparing counts against the pyboqa implementation.
     *
     * @return A map from disease ID to {@link BoqaCounts}.
     */
    @JsonIgnore
    public Map<String, BoqaCounts> getBoqaCounts() {
        Map<String, BoqaCounts> boqaCountsMap = new HashMap<>();
        for (BoqaResult result : resultsList) {
            String id = result.counts.diseaseId();
            boqaCountsMap.put(id, result.counts());
        }
        return boqaCountsMap;
    }

    /**
     * Computes normalized BOQA scores (probabilities) from a list of {@link BoqaCounts}.
     *
     * <p>This method:
     * <ol>
     *   <li>Computes un-normalized probabilities for each disease.</li>
     *   <li>Normalizes them by dividing by the sum across all diseases.</li>
     *   <li>Stores the resulting {@link BoqaResult} in {@code resultsList}, sorted by boqaScore.</li>
     * </ol>
     * <p>
     * TODO consider using again pyboqa scores results, but this is trivial at this point
     *
     * @param boqaCountsList List of BoqaCounts for all diseases in the analysis.
     */
    public void computeBoqaListResults(List<BoqaCounts> boqaCountsList) {
        //ArrayList<BoqaResult> resultsList = new ArrayList<>();
        Map<String, Double> rawScores = boqaCountsList.stream()
                .collect(Collectors.toMap(
                        BoqaCounts::diseaseId,
                        bc -> computeUnnormalizedProbability(AlgorithmParameters.ALPHA, AlgorithmParameters.BETA, bc)
                ));
        double sum = rawScores.values().stream().mapToDouble(Double::doubleValue).sum();
        List<BoqaResult> allResults = new ArrayList<>();
        boqaCountsList.forEach(bc-> {
            double normalizedScore = rawScores.get(bc.diseaseId()) / sum;
            allResults.add(new BoqaResult(bc, normalizedScore));
        });

        Collections.sort(allResults);
        resultsList.addAll(allResults.stream()
                .limit(resultsLimit)
                .collect(Collectors.toList()));
        //return resultList;
    }

    /**
     * Computes the un-normalized BOQA probability for a given set of BoqaCounts and parameters.
     *
     * @param alpha  False positive rate parameter.
     * @param beta   False negative rate parameter.
     * @param counts The {@link BoqaCounts} for a disease.
     * @return The un-normalized probability score.
     */
    static double computeUnnormalizedProbability(double alpha, double beta, BoqaCounts counts){
        return Math.pow(alpha, counts.fpBoqaCount())*
                Math.pow(beta, counts.fnBoqaCount())*
                Math.pow(1-alpha, counts.tnBoqaCount())*
                Math.pow(1-beta, counts.tpBoqaCount());
    }

}
