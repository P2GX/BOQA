package com.github.p2gx.boqa.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.monarchinitiative.phenol.graph.OntologyGraph;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.RecordComponent;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

class BoqaSetCounterTest {

    private static DiseaseDataParseIngest diseaseData;
    private static OntologyGraph<TermId> hpoGraph;
    private static Counter counter;

    @BeforeAll
    static void setup() throws IOException {
        // Getting the versions used in pyboqa since, for now, that is the only main test happening here.
        //TODO if we confront with counts, extract a certain set of OMIMs from HPOA and create a limited diseaseData
        try (InputStream annotationStream = new GZIPInputStream(DiseaseDataParseIngestTest.class.getResourceAsStream("phenotype.v2025-05-06.hpoa.gz"))) {
            diseaseData = new DiseaseDataParseIngest(annotationStream);
        }
        try (
            InputStream ontologyStream = new GZIPInputStream(Objects.requireNonNull(GraphTraversingTest.class.getResourceAsStream("hp.v2025-05-06.json.gz")))
        ) {
            hpoGraph = OntologyLoader.loadOntology(ontologyStream).graph();
        }
        counter = new BoqaSetCounter(diseaseData, hpoGraph);
    }
    @Test
    void testCsvPresence() {
        InputStream is = getClass().getClassLoader().getResourceAsStream("pyboqa_hpo2025-05-06_ppkt-v0-1-24.csv");
        assertNotNull(is, "CSV file not found in classpath!");
    }
    @AfterEach
    void tearDown() {
    }

    // As a first idea, test against pyboqa results
    @ParameterizedTest(name = "[{index}] {arguments}")
    @CsvFileSource(
            resources = "pyboqa_hpo2025-05-06_ppkt-v0-1-24.csv",
            delimiter = ',',
            numLinesToSkip = 1
            // useHeadersInDisplayName = true // does not work, don't use it
    )
    void testComputeBoqaCountsAgainstPyboqa(
            String jsonFile,
            String diagnosedDiseaseId,
            String tnExp,
            String fnExp,
            String fpExp,
            String tpExp
            ) throws URISyntaxException, IOException {
        int tnExpInt = Integer.parseInt(tnExp.trim());
        int fnExpInt = Integer.parseInt(fnExp.trim());
        int tpExpInt = Integer.parseInt(tpExp.trim());
        int fpExpInt = Integer.parseInt(fpExp.trim());

        BoqaCounts pyboqaCounts = new BoqaCounts(
                diagnosedDiseaseId,
                tpExpInt,
                fpExpInt,
                tnExpInt,
                fnExpInt
        );

        URL resourceUrl = PhenopacketReaderTest.class.getResource("phenopackets/" + jsonFile);
        if (resourceUrl == null) {
            throw new IOException("Resource not found: " + jsonFile);
        }
        Path ppkt = Path.of(resourceUrl.toURI());
        Analysis analysis = new AnalysisDummy(new PhenopacketReader(ppkt), counter);
        analysis.run();
        Map<String, BoqaCounts> boqaCountsMap = analysis.getResults().getBoqaCounts();
        assertEquals(boqaCountsMap.get(diagnosedDiseaseId), pyboqaCounts);
    }

    //TODO move the next two methods elsewhere
    private static double getJavaBoqaScore(Path ppkt, Counter counter, String diseaseToTest) {
        double alpha = 1.0/19077; // the denominator is the size of onto_dict for the version used
        double beta = 0.1;
        Analysis analysis = new AnalysisDummy(new PhenopacketReader(ppkt), counter);
        analysis.run();
        Map<String, BoqaCounts> boqaCountsMap = analysis.getResults().getBoqaCounts();
        Map<String, Double> probabilityMap = boqaCountsMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> computeUnnormalizedProbability(alpha, beta, entry.getValue())
                ));
        return probabilityMap.get(diseaseToTest)/probabilityMap.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

    }

    private static double computeUnnormalizedProbability(double alpha, double beta, BoqaCounts counts){
        return Math.pow(alpha, counts.fpExponent())*
                Math.pow(beta, counts.fnExponent())*
                Math.pow(1-alpha, counts.tnExponent())*
                Math.pow(1-beta, counts.tpExponent());
    }

    @Test
    void testGetDiseaseIds() {
    }
}