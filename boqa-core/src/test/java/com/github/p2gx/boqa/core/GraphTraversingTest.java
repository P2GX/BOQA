package com.github.p2gx.boqa.core;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.monarchinitiative.phenol.graph.OntologyGraph;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GraphTraversingTest {

    GraphTraversing graphTraverser;
    OntologyGraph<TermId> hpoGraph;

    @BeforeAll
    void setUp() throws IOException {
        try (
            InputStream ontologyStream = new GZIPInputStream(Objects.requireNonNull(GraphTraversingTest.class.getResourceAsStream("hp.v2025-05-06.json.gz")))
        ) {
            OntologyGraph<TermId> hpoGraph = OntologyLoader.loadOntology(ontologyStream).graph();
            this.graphTraverser = new GraphTraversing(hpoGraph);
        }
    }

    @Test
    void initLayer() {
        // Manually curated example: start from Arachnodactyly, HP:0001166, and go up
        Set<String> queryTermsFromOne = Set.of("HP:0001166","HP:0001238","HP:0100807","HP:0001167","HP:0001155","HP:0002817",
                "HP:0040064","HP:0000118","HP:0011297","HP:0002813","HP:0040068","HP:0000924","HP:0011844",
                "HP:0011842","HP:0033127");
        Set<TermId> queryTermIdsFromOne = queryTermsFromOne.stream()
                .map(TermId::of)
                .collect(Collectors.toSet());
        assertEquals(queryTermIdsFromOne, graphTraverser.initLayer(Set.of(TermId.of("HP:0001166"))));

        // Now two terms on:
        // HP:0001635 "Congestive heart failure" and HP:0010787 "Genital neoplasm"
        Set<String> queryTermsFromTwo = Set.of("HP:0001635","HP:0011025","HP:0001626","HP:0000118","HP:0010787",
                "HP:0007379","HP:0011793","HP:0002664","HP:0000119","HP:0000078");
        Set<TermId> queryTermIdsFromTwo = queryTermsFromTwo.stream()
                .map(TermId::of)
                .collect(Collectors.toSet());
        assertEquals(queryTermIdsFromTwo, graphTraverser.initLayer(Set.of(
                TermId.of("HP:0010787"),TermId.of("HP:0001635")) ));
    }

    @Test
    void extendWithParents() {
    }

    @Test
    void extendWithChildren() {
    }

    //TODO should we try ParametrizedTest here? When running this counts as 1 test as of now.
    @Test
    void allParentsActive() {
        // HP:0012718 has two parents: HP:0011024 and HP:0025033
        // Both on
        assertTrue(graphTraverser.allParentsActive(
                TermId.of("HP:0012718"),
                Set.of(TermId.of("HP:0011024"), TermId.of("HP:0025033"))));
        // One on and one off
        assertFalse(graphTraverser.allParentsActive(
                TermId.of("HP:0012718"),
                Set.of(TermId.of("HP:0011024"))));
        // Both off
        assertFalse(graphTraverser.allParentsActive(
                TermId.of("HP:0012718"),
                Set.of()));
    }
}