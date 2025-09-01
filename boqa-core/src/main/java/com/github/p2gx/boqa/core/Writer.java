package com.github.p2gx.boqa.core;

import com.github.p2gx.boqa.core.analysis.AnalysisResults;

import java.nio.file.Path;
import java.util.Set;
import java.util.Map;
import java.io.File;
import java.io.IOException;
/**
 * Write results to file.
 */
public interface Writer {
    void writeResults(Set<AnalysisResults> analysisResults,
                      Path hpo,
                      Path hpoa,
                      String cliArgs,
                      Map<String, Object> algorithmParams,
                      Path outPath) throws IOException;
}
