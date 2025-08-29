package com.github.p2gx.boqa.core.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.p2gx.boqa.core.Writer;
import com.github.p2gx.boqa.core.analysis.AnalysisResults;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

public class JsonResultWriter implements Writer {

    @Override
    public void writeResults(Set<AnalysisResults> analysisResults,
                             File hpoFile,
                             File hpoa,
                             String cliArgs,
                             Map<String, Object> algorithmParams,
                             File outFile) throws IOException {

        ObjectMapper mapper = new ObjectMapper();

        JsonNode root = mapper.readTree(hpoFile);
        String versionUrl = root
                .path("graphs")
                .get(0)
                .path("meta")
                .path("version")
                .asText();
        String hpoVersion = extractHpVersion(versionUrl);
        String hpoaVersion = readHpoaVersion(hpoa);
        String boqaVersion = JsonResultWriter.class
                .getPackage()
                .getImplementationVersion();
        if (boqaVersion == null) {
            boqaVersion = "unknown";
        }
        Metadata metadata = new Metadata(
                Instant.now().toString(),
                hpoVersion,
                hpoaVersion,
                Map.of("phenopacketStoreVersion", "v0.1.24"),
                algorithmParams,
                cliArgs,
                Map.of(
                        "javaVersion", System.getProperty("java.version"),
                        "os", System.getProperty("os.name"),
                        "arch", System.getProperty("os.arch"),
                        "boqaVersion", boqaVersion
                )
        );

        ResultBundle bundle = new ResultBundle(metadata, analysisResults);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(outFile, bundle);
    }

    public static String extractHpVersion(String versionUrl) {
        return Arrays.stream(versionUrl.split("/"))
                .filter(part -> part.matches("\\d{4}-\\d{2}-\\d{2}"))
                .findFirst()
                .orElse("unknown");
    }

    public static String readHpoaVersion(File hpoaFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(hpoaFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#version:")) {
                    return line.substring("#version:".length()).trim();
                }
            }
        }
        return "unknown"; // fallback if not found
    }

}

