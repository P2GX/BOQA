package com.github.p2gx.boqa.core.output;
import com.github.p2gx.boqa.core.analysis.PatientCountsAnalysis.BoqaResult;

import java.util.List;
import java.util.Map;

public class ResultBundle {
    private final Metadata metadata;
    private final Map<String, List<BoqaResult>> results;

    public ResultBundle(Metadata metadata, Map<String, List<BoqaResult>> analysisResults) {
        this.metadata = metadata;
        this.results = analysisResults;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public Map<String, List<BoqaResult>> getResults() {
        return results;
    }
}
