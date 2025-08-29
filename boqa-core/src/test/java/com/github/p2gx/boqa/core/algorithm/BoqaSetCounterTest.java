package com.github.p2gx.boqa.core.algorithm;

import com.github.p2gx.boqa.core.Analysis;
import com.github.p2gx.boqa.core.Counter;
import com.github.p2gx.boqa.core.analysis.PatientCountsAnalysis;
import com.github.p2gx.boqa.core.diseases.DiseaseDataParseIngest;
import com.github.p2gx.boqa.core.patient.PhenopacketData;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.monarchinitiative.phenol.graph.OntologyGraph;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

class BoqaSetCounterTest {

    private static DiseaseDataParseIngest diseaseData;
    private static OntologyGraph<TermId> hpoGraph;
    private static Counter counter;


    @BeforeAll
    static void setup() throws IOException {
        try (InputStream annotationStream = new GZIPInputStream(BoqaSetCounterTest.class
                .getResourceAsStream("/com/github/p2gx/boqa/core/phenotype.v2025-05-06.hpoa.gz"))) {
            diseaseData = new DiseaseDataParseIngest(annotationStream);
        }
        try (
            InputStream ontologyStream = new GZIPInputStream(Objects.requireNonNull(GraphTraversingTest.class
                    .getResourceAsStream("/com/github/p2gx/boqa/core/hp.v2025-05-06.json.gz")))
        ) {
            hpoGraph = OntologyLoader.loadOntology(ontologyStream).graph();
        }
        counter = new BoqaSetCounter(diseaseData, hpoGraph, true);
    }

    @Tag("expensive_test")
    @ParameterizedTest(name = "[{index}] {arguments}")
    @CsvFileSource(
            resources = "pyboqa_counts_for_top_ranked_diseases.csv",
            delimiter = ',',
            numLinesToSkip = 2
            // useHeadersInDisplayName = true // does not work, don't use it
    )
    void testPyboqaFull(
            String jsonFile,
            String diagnosedDiseaseId,
            String tnExp,
            String fnExp,
            String fpExp,
            String tpExp
    ) throws URISyntaxException, IOException {
        testComputeBoqaCountsAgainstPyboqa(jsonFile, diagnosedDiseaseId, tnExp, fnExp, fpExp, tpExp);
    }

    // As a first idea, test against pyboqa results
    @ParameterizedTest(name = "[{index}] {arguments}")
    @CsvFileSource(resources = "few_examples_pyboqa_counts_for_top_ranked_diseases.csv", numLinesToSkip = 2)
    void testPyboqaSubset(
            String jsonFile,
            String diagnosedDiseaseId,
            String tnExp,
            String fnExp,
            String fpExp,
            String tpExp
    ) throws URISyntaxException, IOException {
        testComputeBoqaCountsAgainstPyboqa(jsonFile, diagnosedDiseaseId, tnExp, fnExp, fpExp, tpExp);
    }

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
        HashMap<String,String> idToLabel = diseaseData.getIdToLabel();
        BoqaCounts pyboqaCounts = new BoqaCounts(
                diagnosedDiseaseId,
                idToLabel.get(diagnosedDiseaseId),
                tpExpInt,
                fpExpInt,
                tnExpInt,
                fnExpInt
        );

        URL resourceUrl = BoqaSetCounterTest.class
                .getResource("/com/github/p2gx/boqa/core/phenopackets/" + jsonFile);
        if (resourceUrl == null) {
            throw new IOException("Resource not found: " + jsonFile);
        }
        Path ppkt = Path.of(resourceUrl.toURI());
        Analysis analysis = new PatientCountsAnalysis(new PhenopacketData(ppkt), counter);
        analysis.run();
        Map<String, BoqaCounts> boqaCountsMap = analysis.getResults().getBoqaCounts();
        assertEquals(pyboqaCounts, boqaCountsMap.get(diagnosedDiseaseId));
    }

    @Test
    void testGetDiseaseIds() {
    }
}