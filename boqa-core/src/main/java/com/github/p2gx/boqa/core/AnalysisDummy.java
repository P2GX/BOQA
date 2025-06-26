package com.github.p2gx.boqa.core;

public class AnalysisDummy implements Analysis {

    Counter counter;
    AnalysisResults results;

    public AnalysisDummy(QueryData queryData, Counter counter) {
        this.results = new AnalysisResults(queryData);
        this.counter = counter;
    }

    @Override
    public void run() {
        // This for loop should be parallelized
        for (String diseaseId : counter.getDiseaseIds()) {
            results.addBoqaCounts(counter.getBoqaCounts(diseaseId));
        }
    }

    @Override
    public AnalysisResults getResults() {
       return this.results;
    }
}
