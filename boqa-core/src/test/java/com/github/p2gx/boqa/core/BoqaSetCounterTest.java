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

    @BeforeAll
    static void setup() throws IOException {
        // Getting the versions used in pyboqa since, for now, that is the only main test happening here.
        try (InputStream annotationStream = new GZIPInputStream(DiseaseDataParseIngestTest.class.getResourceAsStream("phenotype.v2025-03-03.hpoa.gz"))) {
            diseaseData = new DiseaseDataParseIngest(annotationStream);
        }
        try (
            InputStream ontologyStream = new GZIPInputStream(Objects.requireNonNull(GraphTraversingTest.class.getResourceAsStream("hp.v2025-03-03.json.gz")))
        ) {
            hpoGraph = OntologyLoader.loadOntology(ontologyStream).graph();
        }
    }

    @AfterEach
    void tearDown() {
    }

    @ParameterizedTest(name = "[{index}] {arguments}")
    @CsvFileSource(
            resources = "PYBOQA_phenopacket_store_0.1.24_results_table.tsv",
            delimiter = '\t',
            numLinesToSkip = 1,
            useHeadersInDisplayName = true
    )
    void testComputeBoqaCounts(
            String phenopacketId,
            String status,
            String diagnosedDiseaseId,
            String nAnnotated,
            String nIncluded,
            String nExcluded,
            String nIntersect,
            String topRankedId,
            String topScore,
            String diagnosedRank,
            String diagnosedScore,
            String jsonFile
    ) throws URISyntaxException {
        // As a first idea, test against pyboqa results
        double expectedScore = Double.parseDouble(diagnosedScore.trim());
        // TODO add phenopacket store to resource?
        Path ppkt = Path.of("/Users/leonardo/data/ppkt-store-0.1.24")
                .resolve(Path.of(jsonFile).getParent().getFileName())
                .resolve(Path.of(jsonFile).getFileName());
        Counter counter = new BoqaSetCounter(diseaseData, hpoGraph);

        // Take alpha and beta and compute P
        double javaBoqaScore = getJavaBoqaScore(ppkt, counter, diagnosedDiseaseId);
        assertEquals(expectedScore, javaBoqaScore, 1e-6);
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