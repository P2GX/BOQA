package com.github.p2gx.boqa.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

class BoqaSetCounterTest {

    private static DiseaseDataParseIngest diseaseData;
    private static OntologyGraph<TermId> hpoGraph;

    @BeforeAll
    static void setup() throws IOException {
        try (InputStream annotationStream = new GZIPInputStream(DiseaseDataParseIngestTest.class.getResourceAsStream("phenotype.v2025-05-06.hpoa.gz"))) {
            diseaseData = new DiseaseDataParseIngest(annotationStream);
        }
        try (
            InputStream ontologyStream = new GZIPInputStream(Objects.requireNonNull(GraphTraversingTest.class.getResourceAsStream("hp.v2025-05-06.json.gz")))
        ) {
            hpoGraph = OntologyLoader.loadOntology(ontologyStream).graph();
        }
    }

    @AfterEach
    void tearDown() {
    }


    @Test
    void testComputeBoqaCounts() throws URISyntaxException {
        // As a first idea, test against pyboqa results
        // E.g. PMID_24369382_Family2II1.json has in pyboqa, has OMIM:614322
        String diseaseToTest = "OMIM:614322";
        double pyboqaScore = 0.999999999994057;
        String filename = "PMID_24369382_Family2II1.json";
        // TODO add more test cases and use parametrized test

        URL resourceUrl = PhenopacketReaderTest.class.getResource(filename);
        assert resourceUrl != null;
        Path ppkt = Path.of(resourceUrl.toURI());

        Counter counter = new BoqaSetCounter(diseaseData, hpoGraph);
        // take alpha and beta and compute P
        double javaBoqaScore = getJavaBoqaScore(ppkt, counter, diseaseToTest);
        assertEquals(pyboqaScore, javaBoqaScore, 1e-9);
    }

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