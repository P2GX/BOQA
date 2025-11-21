package org.p2gx.boqa.core.patient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.boqa.core.internal.OntologyTraverser;
import org.p2gx.boqa.core.internal.OntologyTraverserTest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PhenopacketDataTest {

    private static List<PhenopacketData> examplePpkts = new ArrayList<>();
    OntologyTraverser ontologyTraverser;

    @BeforeAll
    void setUp() throws IOException {
        try (
                InputStream ontologyStream = new GZIPInputStream(Objects.requireNonNull(OntologyTraverserTest.class
                        .getResourceAsStream("/org/p2gx/boqa/core/hp.v2025-05-06.json.gz")))
        ) {
            Ontology hpo = OntologyLoader.loadOntology(ontologyStream);
            this.ontologyTraverser = new OntologyTraverser(hpo);
        }


        String[] filenames = {
                "PMID_30569521_proband.json",
                "PMID_10580070_A_III-5.json", // no observed terms, only excluded
                "PMID_36996813_Individual13.json", // only observed terms, no excluded
                "PMID_24369382_Family2II1.json", // id contains a dot "."
                "PMID_25835445_7-II1.json" // id contains a star "*" and a colon ":"
        };
        for (String filename : filenames) {
            try {
                URL resourceUrl = PhenopacketDataTest.class
                        .getResource("/org/p2gx/boqa/core/phenopackets/" + filename);
                if (resourceUrl == null) {
                    throw new IOException("Resource not found: " + filename);
                }
                Path ppkt = Path.of(resourceUrl.toURI());
                examplePpkts.add(new PhenopacketData(ppkt));
            } catch (URISyntaxException e) {
                throw new IOException("Failed to resolve resource URI", e);
            }
        }
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testGetObservedTerms() throws IOException, URISyntaxException {
        // Read the line from a file (assuming it's all on one line)
        String csvLine = Files.readString(Path.of(PhenopacketDataTest.class
                .getResource("PMID_30569521_proband_features.csv").toURI())).trim();
        // Convert to Set<String>
        Set<TermId> termSet = Arrays.stream(csvLine.split(","))
                .map(s -> s.replaceAll("^\"|\"$", "")) // Remove surrounding quotes
                .map(TermId::of)
                .collect(Collectors.toSet());
        assertEquals(termSet, examplePpkts.get(0).getObservedTerms());

        // Only excluded terms in phenopacket
        assertTrue(examplePpkts.get(1).getObservedTerms().isEmpty());
    }

    @Test
    void testGetExcludedTerms() throws IOException, URISyntaxException {
        // Read the line from a file (assuming it's all on one line)
        String csvLine = Files.readString(Path.of(PhenopacketDataTest.class.
                getResource("PMID_30569521_proband_excluded_features.csv").toURI())).trim();
        // Convert to Set<String>
        Set<TermId> termSet = Arrays.stream(csvLine.split(","))
                .map(s -> s.replaceAll("^\"|\"$", "")) // Remove surrounding quotes
                .map(TermId::of)
                .collect(Collectors.toSet());
        assertEquals(termSet, examplePpkts.get(0).getExcludedTerms());

        // Phenopacket with no excluded terms
        assertTrue(examplePpkts.get(2).getExcludedTerms().isEmpty());
    }

    @Test
    void getID() {
        // Standard
        assertEquals("PMID_30569521_proband", examplePpkts.get(0).getID());
        // With "."
        assertEquals("PMID_24369382_Family_2_II.1", examplePpkts.get(3).getID());
        // With ":" and "*"
        assertEquals("PMID_25835445_7-II:1*", examplePpkts.get(4).getID());
    }
}