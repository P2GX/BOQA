package org.p2gx.boqa.core.output;
import org.p2gx.boqa.core.analysis.BoqaAnalysisResult;

import java.util.List;

public class ResultBundle {
    private final Metadata metadata;
    private final List<BoqaAnalysisResult> results;

    public ResultBundle(Metadata metadata, List<BoqaAnalysisResult> boqaAnalysisResults) {
        this.metadata = metadata;
        this.results = boqaAnalysisResults;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public List<BoqaAnalysisResult> getResults() {
        return results;
    }
}
