package com.github.p2gx.boqa.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

public class ResultWriter implements Writer{

    public static void writeResults(Set<AnalysisResults> analysisResults,
                                    String cliArgs,
                                    Map<String, Object> algorithmParams,
                                    File outFile) throws IOException {

        Metadata metadata = new Metadata(
                Instant.now().toString(),
                "2025-08-01",  // hpoVersion
                "2025-08-01",  // hpoaVersion
                Map.of("phenopacketStoreVersion", "1.3.0"),
                algorithmParams,
                cliArgs,
                Map.of(
                        "javaVersion", System.getProperty("java.version"),
                        "os", System.getProperty("os.name"),
                        "arch", System.getProperty("os.arch"),
                        "boqaVersion", "2.1.0"
                )
        );

        ResultBundle bundle = new ResultBundle(metadata, analysisResults);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(outFile, bundle);
    }
}

