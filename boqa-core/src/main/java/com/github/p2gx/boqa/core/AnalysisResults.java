package com.github.p2gx.boqa.core;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Bookkeeping class that contains the query along with the results of a BoqaAnalysis.
 * Functions for calculating scores, ranking and reporting should be implemented here.
 */
public class AnalysisResults {

    private PatientData patientData;
    private Map<String, BoqaCounts> boqaCountsMap = new HashMap<>();

    public AnalysisResults(PatientData patientData) {
        this.patientData = patientData;
    }

    public PatientData getPatientData() {
        return patientData;
    }

    public Map<String, BoqaCounts> getBoqaCounts() {
        return boqaCountsMap;
    }

    public void addBoqaCounts(BoqaCounts boqaCounts) {
        boqaCountsMap.put(boqaCounts.diseaseId(), boqaCounts);
    }

    private Map<String, Double> computeBoqaScore(String diseaseToTest) {
        double alpha = 1.0/19077; // TODO move elsewhere
        double beta = 0.1; // TODO move elsewhere
        Map<String, Double> probabilityMap = boqaCountsMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> computeUnnormalizedProbability(alpha, beta, entry.getValue())
                ));
        double normalizationFactor = probabilityMap.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        //TODO the following return needs checking
        return probabilityMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() / normalizationFactor
                ));
    }

    private static double computeUnnormalizedProbability(double alpha, double beta, BoqaCounts counts){
        return Math.pow(alpha, counts.fpBoqaCount())*
                Math.pow(beta, counts.fnBoqaCount())*
                Math.pow(1-alpha, counts.tnBoqaCount())*
                Math.pow(1-beta, counts.tpBoqaCount());
    }
}
