package com.github.p2gx.boqa.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhenopacketReaderTest {

    private static List<PhenopacketReader> examplePpkts = new ArrayList<>();

    @BeforeAll
    static void setUp() throws IOException {
        String[] filenames = {
                "PMID_30569521_proband.json",
                "PMID_10580070_A_III-5.json", // no observed terms, only excluded
                "PMID_36996813_Individual13.json", // only observed terms, no excluded
                "PMID_24369382_Family2II1.json", // id contains a dot "."
                "PMID_25835445_7-II1.json" // id contains a star "*" and a colon ":"
        };
        for (String filename : filenames) {
            try {
                URL resourceUrl = PhenopacketReaderTest.class.getResource(filename);
                if (resourceUrl == null) {
                    throw new IOException("Resource not found: " + filename);
                }
                Path ppkt = Path.of(resourceUrl.toURI());
                examplePpkts.add(new PhenopacketReader(ppkt));
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
        String csvLine = Files.readString(Path.of(PhenopacketReaderTest.class.
                getResource("PMID_30569521_proband_features.csv").toURI())).trim();
        // Convert to Set<String>
        Set<String> termSet = Arrays.stream(csvLine.split(","))
                .map(s -> s.replaceAll("^\"|\"$", "")) // Remove surrounding quotes
                .collect(Collectors.toSet());
        assertEquals(termSet, examplePpkts.get(0).getObservedTerms());

        // Only excluded terms in phenopacket
        assertTrue(examplePpkts.get(1).getObservedTerms().isEmpty());
    }

    @Test
    void testGetExcludedTerms() throws IOException, URISyntaxException {
        // Read the line from a file (assuming it's all on one line)
        String csvLine = Files.readString(Path.of(PhenopacketReaderTest.class.
                getResource("PMID_30569521_proband_excluded_features.csv").toURI())).trim();
        // Convert to Set<String>
        Set<String> termSet = Arrays.stream(csvLine.split(","))
                .map(s -> s.replaceAll("^\"|\"$", "")) // Remove surrounding quotes
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