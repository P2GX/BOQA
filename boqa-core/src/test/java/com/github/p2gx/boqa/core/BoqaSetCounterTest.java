package com.github.p2gx.boqa.core;

import org.junit.jupiter.api.*;
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
    static void enableTestMode() {
        System.setProperty("test.mode", "true");
    }

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
        counter = new BoqaSetCounter(diseaseData, hpoGraph);
    }

    @AfterAll
    static void disableTestMode() {
        System.clearProperty("test.mode");
    }

    // As a first idea, test against pyboqa results
    @ParameterizedTest(name = "[{index}] {arguments}")
    @CsvFileSource(
            resources = "pyboqa_counts_for_top_ranked_diseases.csv",
            delimiter = ',',
            numLinesToSkip = 2
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
        assertEquals(pyboqaCounts, boqaCountsMap.get(diagnosedDiseaseId));
    }

    @Test
    void testGetDiseaseIds() {
    }
}