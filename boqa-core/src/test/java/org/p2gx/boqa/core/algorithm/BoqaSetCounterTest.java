package org.p2gx.boqa.core.algorithm;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.boqa.core.Counter;
import org.p2gx.boqa.core.DiseaseData;
import org.p2gx.boqa.core.analysis.CandidateResult;
import org.p2gx.boqa.core.diseases.CandidateDisease;
import org.p2gx.boqa.core.diseases.DiseaseDataParser;
import org.p2gx.boqa.core.diseases.TargetDisease;
import org.p2gx.boqa.core.internal.OntologyTraverserTest;
import org.p2gx.boqa.core.patient.PhenopacketData;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static org.p2gx.boqa.core.analysis.BoqaPatientAnalyzer.computeBoqaResults;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BoqaSetCounterTest {

    private DiseaseData diseaseData;
    private List<CandidateDisease> diseaseCandidateList;
    private Ontology hpo;

    @BeforeAll
    void setup() throws IOException {
        try (InputStream annotationStream = new GZIPInputStream(BoqaSetCounterTest.class
                .getResourceAsStream("/org/p2gx/boqa/core/phenotype.v2025-05-06.hpoa.gz"))) {
            this.diseaseData = DiseaseDataParser.parseDiseaseDataFromHpoa(annotationStream);
            List<TargetDisease.PhenotypeOnly> targetDiseaseList = diseaseData.getDiseaseIds().stream()
                    .map(d -> new TargetDisease.PhenotypeOnly(d,"LABEL",
                            diseaseData.getObservedDiseaseFeatures(d).stream()
                                    .map(TermId::of).collect(Collectors.toSet())
                    )).toList();
            // TODO only single diseases ok?
            this.diseaseCandidateList = CandidateDisease.createSingleDiseaseCandidates(targetDiseaseList);
        }
        try (
            InputStream ontologyStream = new GZIPInputStream(Objects.requireNonNull(OntologyTraverserTest.class
                    .getResourceAsStream("/org/p2gx/boqa/core/hp.v2025-05-06.json.gz")))
        ) {
            this.hpo = OntologyLoader.loadOntology(ontologyStream);
        }
    }

    @Tag("expensive_test")
    @ParameterizedTest(name = "[{index}] {arguments}")
    @CsvFileSource(
            resources = "boqa_counts_for_top_ranked_diseases.csv",
            delimiter = ',',
            numLinesToSkip = 2
            // useHeadersInDisplayName = true // does not work, don't use it
    )
    void testAgainstReferenceFull(
            String jsonFile,
            String diagnosedDiseaseId,
            int tnExp,
            int fnExp,
            int fpExp,
            int tpExp
    ) throws URISyntaxException, IOException {
        testComputeBoqaCountsAgainstReference(jsonFile, diagnosedDiseaseId, tnExp, fnExp, fpExp, tpExp);
    }

    @ParameterizedTest(name = "[{index}] {arguments}")
    @CsvFileSource(resources = "few_examples_boqa_counts_for_top_ranked_diseases.csv", numLinesToSkip = 2)
    void testAgainstReferenceSubset(
            String jsonFile,
            String diagnosedDiseaseId,
            int tnExp,
            int fnExp,
            int fpExp,
            int tpExp
    ) throws URISyntaxException, IOException {
        testComputeBoqaCountsAgainstReference(jsonFile, diagnosedDiseaseId, tnExp, fnExp, fpExp, tpExp);
    }

    void testComputeBoqaCountsAgainstReference(
            String jsonFile,
            String diagnosedDiseaseId,
            int tnExp,
            int fnExp,
            int fpExp,
            int tpExp
    ) throws URISyntaxException, IOException {
        BoqaCounts referenceBoqaCounts = new BoqaCounts(
                tpExp,
                fpExp,
                tnExp,
                fnExp
        );

        URL resourceUrl = BoqaSetCounterTest.class
                .getResource("/org/p2gx/boqa/core/phenopackets/" + jsonFile);
        if (resourceUrl == null) {
            throw new IOException("Resource not found: " + jsonFile);
        }
        Path ppkt = Path.of(resourceUrl.toURI());
        int limit =  Integer.MAX_VALUE;
        AlgorithmParameters params = AlgorithmParameters.create(0.2,0.3); // numbers don't matter
        List<CandidateDisease> diagnosedCandidate = diseaseCandidateList.stream()
                .filter(d -> Objects.equals(d.diseaseId(), Set.of(diagnosedDiseaseId)))
                .toList();
        PhenopacketData patient = new PhenopacketData(ppkt, hpo);
        Counter counter = new BlendedCounter(hpo, patient.getObservedTerms());

        List<CandidateResult> candidateResults = computeBoqaResults(patient, counter, limit, params, diagnosedCandidate);
        assertEquals(referenceBoqaCounts, candidateResults.getFirst().counts());
    }
}
