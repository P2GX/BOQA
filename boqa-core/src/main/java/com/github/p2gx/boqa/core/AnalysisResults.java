package com.github.p2gx.boqa.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Bookkeeping class that contains the query along with the results of a BoqaAnalysis.
 * Functions for calculating scores, ranking and reporting should be implemented here.
 */
public class AnalysisResults {

    private double alpha = 1.0/19077; // TODO move elsewhere?
    private double beta = 0.1; // TODO move elsewhere?

    private PatientData patientData;
    // Extra record with score to ease development phase without changing future code
    public record BoqaResult(BoqaCounts counts, Double rawScore) {}
    private Map<String, BoqaResult> resultsMap = new HashMap<>();

    public AnalysisResults(PatientData patientData) {
        this.patientData = patientData;
    }

    public PatientData getPatientData() {
        return patientData;
    }
    public Map<String, BoqaResult> getBoqaResult(){
        return resultsMap;
    }
    public Map<String, BoqaCounts> getBoqaCounts() {
        // Create a new map containing only the BoqaCounts, for testing against pyboqa
        Map<String, BoqaCounts> boqaCountsMap = new HashMap<>();
        for (Map.Entry<String, BoqaResult> entry : resultsMap.entrySet()) {
            boqaCountsMap.put(entry.getKey(), entry.getValue().counts());
        }
        return boqaCountsMap;
    }

    public void computeBoqaResults(List<BoqaCounts> boqaCountsList) {
        Map<String, Double> rawScores = boqaCountsList.stream()
                .collect(Collectors.toMap(
                        BoqaCounts::diseaseId,
                        bc -> computeUnnormalizedProbability(alpha, beta, bc)
                ));
        double sum = rawScores.values().stream().mapToDouble(Double::doubleValue).sum();
        boqaCountsList.forEach(bc-> {
            double normalizedScore = rawScores.get(bc.diseaseId()) / sum;
            resultsMap.put(bc.diseaseId(),
                    new BoqaResult(bc, normalizedScore)
            );
        });
    }

    private static double computeUnnormalizedProbability(double alpha, double beta, BoqaCounts counts){
        return Math.pow(alpha, counts.fpBoqaCount())*
                Math.pow(beta, counts.fnBoqaCount())*
                Math.pow(1-alpha, counts.tnBoqaCount())*
                Math.pow(1-beta, counts.tpBoqaCount());
    }

}
