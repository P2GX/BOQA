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
        // Create example, start from Arachnodactyly and work our way up (only one active term, add test with more)
        Set<String> queryTerms = Set.of("HP:0001166","HP:0001238","HP:0100807","HP:0001167","HP:0001155","HP:0002817",
                "HP:0040064","HP:0000118","HP:0011297","HP:0002813","HP:0040068","HP:0000924","HP:0011844",
                "HP:0011842","HP:0033127");
        Set<TermId> queryTermIds = queryTerms.stream()
                .map(TermId::of)
                .collect(Collectors.toSet());
        assertEquals(queryTermIds, graphTraverser.initLayer(Set.of(TermId.of("HP:0001166"))));
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