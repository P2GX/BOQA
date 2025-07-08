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