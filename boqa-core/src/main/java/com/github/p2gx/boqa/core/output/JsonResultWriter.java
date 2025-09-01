package com.github.p2gx.boqa.core.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.p2gx.boqa.core.Writer;
import com.github.p2gx.boqa.core.analysis.AnalysisResults;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

public class JsonResultWriter implements Writer {

    @Override
    public void writeResults(Set<AnalysisResults> analysisResults,
                             Path hpoFile,
                             Path hpoa,
                             String cliArgs,
                             Map<String, Object> algorithmParams,
                             Path outPath) throws IOException {

        ObjectMapper mapper = new ObjectMapper();

        JsonNode root = mapper.readTree(hpoFile.toFile());
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
        try (OutputStream out = Files.newOutputStream(outPath)) {
            mapper.writeValue(out, bundle);
        }    }

    public static String extractHpVersion(String versionUrl) {
        return Arrays.stream(versionUrl.split("/"))
                .filter(part -> part.matches("\\d{4}-\\d{2}-\\d{2}"))
                .findFirst()
                .orElse("unknown");
    }

    public static String readHpoaVersion(Path hpoaFile) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(hpoaFile, StandardCharsets.UTF_8)) {
            return readVersionFromReader(reader);
        }
    }
    // Overload for testing purposes.
    public static String readHpoaVersion(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return readVersionFromReader(reader);
        }
    }

    private static String readVersionFromReader(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("#version:")) {
                return line.substring("#version:".length()).trim();
            }
        }
        return "unknown";
    }

}

