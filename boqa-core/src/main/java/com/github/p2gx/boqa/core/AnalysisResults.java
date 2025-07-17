package com.github.p2gx.boqa.core;

import java.util.HashSet;
import java.util.Set;

/**
 * Book keeping class that contains the query along with the results of a BoqaAnalysis.
 * Functions for calculating scores, ranking and reporting should be implemented here.
 */
public class AnalysisResults {

    private PatientData patientData;
    private Set<BoqaCounts> boqaCountsHashSet = new HashSet<>();

    public AnalysisResults(PatientData patientData) {
        this.patientData = patientData;
    }

    public PatientData getPatientData() {
        return patientData;
    }
    public Set<BoqaCounts> getBoqaCounts() {
        return boqaCountsHashSet;
    }
    public void addBoqaCounts(BoqaCounts boqaCounts) {
        this.boqaCountsHashSet.add(boqaCounts);
    }


}
