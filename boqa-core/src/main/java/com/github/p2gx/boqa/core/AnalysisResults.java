package com.github.p2gx.boqa.core;

import java.util.HashSet;
import java.util.Set;

/**
 * Book keeping class that contains the query along with the results of a BoqaAnalysis.
 * Functions for calculating scores, ranking and reporting should be implemented here.
 */
public class AnalysisResults {

    private PatientData queryData;
    private Set<BoqaCounts> boqaCountsHashSet = new HashSet<>();

    public AnalysisResults(PatientData queryData) {
        this.queryData = queryData;
    }

    public PatientData getQueryData() {
        return queryData;
    }
    public Set<BoqaCounts> getBoqaCounts() {
        return boqaCountsHashSet;
    }
    public void addBoqaCounts(BoqaCounts boqaCounts) {
        this.boqaCountsHashSet.add(boqaCounts);
    }


}
