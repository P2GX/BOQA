package com.github.p2gx.boqa.core.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.p2gx.boqa.core.Writer;
import com.github.p2gx.boqa.core.analysis.PatientCountsAnalysis.BoqaResult;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Writes BOQA analysis results to a JSON file.
 * <p>
 * This implementation of {@link Writer} serializes a set of {@link BoqaResult} along with
 * metadata about the HPO ontology, HPOA annotations, algorithm parameters, CLI arguments, and
 * system information into a human-readable JSON file.
 * <p>
 * The JSON output includes:
 * <ul>
 *   <li>Metadata with timestamps, versions, and system details.</li>
 *   <li>The BOQA analysis results.</li>
 * </ul>
 * <p>
 * Version extraction for HPO and HPOA files is handled via {@link #extractHpVersion(String)}
 * and {@link #readHpoaVersion(Path)} (or the InputStream overload for testing).
 *
 * @author
 *   <a href="mailto:leonardo.chimirri@bih-charite.de">Leonardo Chimirri</a>
 * @implNote The JSON is indented for readability via {@link com.fasterxml.jackson.databind.SerializationFeature#INDENT_OUTPUT}.
 * @implNote Future enhancement: Consider parameterizing or customizing JSON output formatting (e.g., compact vs. indented).
 */
public class JsonResultWriter implements Writer {

    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    @Override
    public void writeResults(Map<String, List<BoqaResult>> analysisResults,
                             Path hpoPath,
                             Path hpoaPath,
                             String cliArgs,
                             Map<String, Object> algorithmParams,
                             Path outPath) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root;
        try (InputStream in = Files.newInputStream(hpoPath)) {
            root = mapper.readTree(in);
        }

        String versionUrl = root
                .path("graphs")
                .get(0)
                .path("meta")
                .path("version")
                .asText();

        String hpoVersion = extractHpVersion(versionUrl);
        String hpoaVersion = readHpoaVersion(hpoaPath);
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
                .filter(part -> DATE_PATTERN.matcher(part).matches())
                .findFirst()
                .orElse("unknown");
    }

    public static String readHpoaVersion(Path hpoaPath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(hpoaPath, StandardCharsets.UTF_8)) {
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

