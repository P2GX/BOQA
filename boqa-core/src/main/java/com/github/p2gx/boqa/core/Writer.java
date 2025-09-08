package com.github.p2gx.boqa.core;

import com.github.p2gx.boqa.core.analysis.PatientCountsAnalysis.BoqaResult;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.io.IOException;
/**
 * Write results to file.
 */
public interface Writer {
    void writeResults(Map<String, List<BoqaResult>> analysisResults,
                      Path hpo,
                      Path hpoa,
                      String cliArgs,
                      Map<String, Object> algorithmParams,
                      Path outPath) throws IOException;
}
