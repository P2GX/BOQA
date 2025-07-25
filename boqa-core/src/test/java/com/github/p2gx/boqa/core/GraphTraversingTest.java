package com.github.p2gx.boqa.core;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.monarchinitiative.phenol.graph.OntologyGraph;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GraphTraversingTest {

    GraphTraversing graphTraverser;

    // TODO: Daniel suggests using Extensions API rather then TestBase and extensions thereof, more modern.
    @BeforeAll
    void setUp() throws IOException {
        try (
            InputStream ontologyStream = new GZIPInputStream(Objects.requireNonNull(GraphTraversingTest.class.getResourceAsStream("hp.v2025-05-06.json.gz")))
        ) {
            OntologyGraph<TermId> hpoGraph = OntologyLoader.loadOntology(ontologyStream).graph();
            this.graphTraverser = new GraphTraversing(hpoGraph);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("activeLayers")
    void testInitLayer(String testName, Set<String> expectedNodes, Set<TermId> observedNodes ) {
        Set<TermId> expectedNodesTermIds = expectedNodes.stream()
                .map(TermId::of)
                .collect(Collectors.toSet());
        assertEquals(expectedNodesTermIds, graphTraverser.initLayer(observedNodes));
    }
    // TODO: Daniel suggests using @CsvSource
    private static Stream<Arguments> activeLayers(){
        // Manually curated example: start from Arachnodactyly, HP:0001166, and go up
        Set<String> queryTermsFromOne = Set.of("HP:0001166","HP:0001238","HP:0100807","HP:0001167","HP:0001155","HP:0002817",
                "HP:0040064","HP:0000118","HP:0011297","HP:0002813","HP:0040068","HP:0000924","HP:0011844",
                "HP:0011842","HP:0033127");
        // Now two terms on:
        // HP:0001635 "Congestive heart failure" and HP:0010787 "Genital neoplasm"
        Set<String> queryTermsFromTwo = Set.of("HP:0001635","HP:0011025","HP:0001626","HP:0000118","HP:0010787",
                "HP:0007379","HP:0011793","HP:0002664","HP:0000119","HP:0000078");
        return Stream.of(
                Arguments.of("One observed HPO",
                        queryTermsFromOne,
                        Set.of(TermId.of("HP:0001166"))),
                Arguments.of("Two observed HPOs",
                        queryTermsFromTwo,
                        Set.of(TermId.of("HP:0010787"),TermId.of("HP:0001635")))
                );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parentsActiveCases")
    void testAllParentsActive(String testName, TermId node, Set<TermId> activeParents, boolean expectation) {
        assertEquals(expectation, graphTraverser.allParentsActive(node, activeParents));
    }

    private static Stream<Arguments> parentsActiveCases(){
        return Stream.of(
                Arguments.of("Both parents ON",
                        TermId.of("HP:0012718"),
                        Set.of(TermId.of("HP:0011024"), TermId.of("HP:0025033")),
                        true),
                Arguments.of("One parent ON",
                        TermId.of("HP:0012718"),
                        Set.of(TermId.of("HP:0011024")),
                        false),
                Arguments.of("Both parents OFF",
                        TermId.of("HP:0012718"),
                        Set.of(),
                        false)
        );
    }
}