package com.github.p2gx.boqa.core;
import java.util.Set;

public class ResultBundle {
    private final Metadata metadata;
    private final Set<AnalysisResults> results;

    public ResultBundle(Metadata metadata, Set<AnalysisResults> analysisResults) {
        this.metadata = metadata;
        this.results = analysisResults;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public Set<AnalysisResults> getResults() {
        return results;
    }
}
