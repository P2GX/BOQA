package com.github.p2gx.boqa.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.graph.OntologyGraph;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BoqaSetCounterTest {

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }


    @Test
    void testComputeBoqaCounts() throws URISyntaxException, IOException {
        // As a first idea, test against pyboqa results
        // E.g. PMID_24369382_Family2II1.json has in pyboqa, has OMIM:614322
        String filename = "PMID_24369382_Family2II1.json";

        URL resourceUrl = PhenopacketReaderTest.class.getResource(filename);
        assert resourceUrl != null;
        Path ppkt = Path.of(resourceUrl.toURI());

        // Need to construct a DiseaseData object with this one disease, run computeBoqaCounts on it
        // Create a mini_phenotype.hpoa file with just the omims I want to test.
        Path phenotypeAnnotationFile = Path.of(BoqaSetCounterTest.class.getResource("mini_phenotype.hpoa").toURI());
        //Path phenotypeAnnotationFile = Path.of(BoqaSetCounterTest.class.getResource("mini_phenotype.hpoa").toURI());
        DiseaseData diseaseData = DiseaseDataParseIngest.fromPath(phenotypeAnnotationFile);
        String ontologyFile = "data/human-phenotype-ontology/latest_20250701/hp.json";
        OntologyGraph<TermId> hpoGraph = OntologyLoader.loadOntology(Paths.get(ontologyFile).toFile()).graph();
        Counter counter = new BoqaSetCounter(diseaseData, hpoGraph);

        // take alpha and beta and compute P
        double pyboqaScore = 0.999999999994057;
        double javaBoqaScore = getJavaBoqaScore(ppkt, counter);
        assertEquals(pyboqaScore, javaBoqaScore);

    }

    private static double getJavaBoqaScore(Path ppkt, Counter counter) {
        double alpha = 1.0/10000000; // TODO 10000000 is placeholder, it's supposed to be onto_dict.size()
        double beta = 0.1;
        Analysis analysis = new AnalysisDummy(new PhenopacketReader(ppkt), counter);
        Set<BoqaCounts> mycount = analysis.getResults().getBoqaCounts();
        // Extract counts TODO fix this and above
        double javaBoqaScore = Math.pow(alpha, count1)*
                Math.pow(beta, count1)*
                Math.pow(1-alpha, count1)*
                Math.pow(1-beta, count1);
        return javaBoqaScore;
    }

    @Test
    void testGetDiseaseIds() {
    }
}