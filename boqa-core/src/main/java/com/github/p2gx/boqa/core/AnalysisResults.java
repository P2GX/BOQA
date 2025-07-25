package com.github.p2gx.boqa.core;

import java.util.HashMap;
import java.util.Map;

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

}
