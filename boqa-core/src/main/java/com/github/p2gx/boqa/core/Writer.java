package com.github.p2gx.boqa.core;

import java.util.HashMap;
import java.util.Set;
import java.util.Map;
import java.io.File;
import java.io.IOException;
/**
 * Write results to file.
 */
public interface Writer {
    void writeResults(Set<AnalysisResults> analysisResults,
                      File hpo,
                      File hpoa,
                      String cliArgs,
                      Map<String, Object> algorithmParams,
                      File outFile) throws IOException;
}
