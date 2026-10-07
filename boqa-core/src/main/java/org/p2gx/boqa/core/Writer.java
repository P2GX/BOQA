package org.p2gx.boqa.core;

import org.p2gx.boqa.core.analysis.PatientAnalysisResult;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.io.IOException;
/**
 * Write results to file.
 */
public interface Writer {
    void writeResults(List<PatientAnalysisResult> patientAnalysisResults,
                      Path hpo,
                      Path hpoa,
                      String cliArgs,
                      Map<String, Object> algorithmParams,
                      Path outPath,
                      boolean compress
    ) throws IOException;
}
