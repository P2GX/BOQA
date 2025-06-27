package com.github.p2gx.boqa.core;

import java.util.Set;
/**
 * Book keeping class that contains the query along with the results of a BoqaAnalysis.
 * Functions for calculating scores, ranking and reporting should be implemented here.
 */
public class AnalysisResults {

    QueryData queryData; //TODO add getter method
    Set<BoqaCounts> boqaCountsSet; //TODO add getter method

    public AnalysisResults(QueryData queryData) {
        this.queryData = queryData;
    }

    public void addBoqaCounts(BoqaCounts boqaCounts) {
        this.boqaCountsSet.add(boqaCounts);
    }
}
