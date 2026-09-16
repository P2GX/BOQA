package org.p2gx.boqa.core.output;
import org.p2gx.boqa.core.analysis.PatientAnalysisResult;

import java.util.List;

public class ResultBundle {
    private final Metadata metadata;
    private final List<PatientAnalysisResult> results;

    public ResultBundle(Metadata metadata, List<PatientAnalysisResult> patientAnalysisResults) {
        this.metadata = metadata;
        this.results = patientAnalysisResults;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public List<PatientAnalysisResult> getResults() {
        return results;
    }
}
