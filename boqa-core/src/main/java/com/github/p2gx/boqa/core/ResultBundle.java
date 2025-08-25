package com.github.p2gx.boqa.core;
import java.util.Set;

public class ResultBundle {
    private Metadata metadata;
    private Set<AnalysisResults> results;

    public ResultBundle(Metadata metadata, Set<AnalysisResults> results) {
        this.metadata = metadata;
        this.results = results;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public Set<AnalysisResults> getResults() {
        return results;
    }
}
