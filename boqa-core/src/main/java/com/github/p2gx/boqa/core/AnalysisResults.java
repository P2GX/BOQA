package com.github.p2gx.boqa.core;

import java.util.Set;

public class AnalysisResults {

    QueryData queryData;
    Set<BoqaCounts> boqaCountsSet;

    public AnalysisResults(QueryData queryData) {
        this.queryData = queryData;
    }

    public void addBoqaCounts(BoqaCounts boqaCounts) {
        this.boqaCountsSet.add(boqaCounts);
    }
}
