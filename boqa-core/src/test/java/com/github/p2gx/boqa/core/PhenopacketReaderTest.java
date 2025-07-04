package com.github.p2gx.boqa.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PhenopacketReaderTest {

    private static PhenopacketReader examplePpkt;

    @BeforeAll
    static void setUp() throws IOException {
        //InputStream ppkt = PhenopacketReaderTest.class.getResourceAsStream("phenopackets/PMID_30569521_proband.json");
        Path ppkt = Path.of(PhenopacketReaderTest.class.getResource("PMID_30569521_proband.json").getPath());
        examplePpkt = new PhenopacketReader(ppkt);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void getObservedPhenotypes() throws IOException {
        // Read the line from a file (assuming it's all on one line)
        String csvLine = Files.readString(Path.of(PhenopacketReaderTest.class.
                getResource("PMID_30569521_proband_features.csv").getPath())).trim();
        // Convert to Set<String>
        Set<String> termSet = Arrays.stream(csvLine.split(","))
                .map(s -> s.replaceAll("^\"|\"$", "")) // Remove surrounding quotes
                .collect(Collectors.toSet());
        assertEquals(termSet, examplePpkt.getObservedPhenotypes());
    }

    @Test
    void getExcludedPhenotypes() throws IOException {
        // Read the line from a file (assuming it's all on one line)
        String csvLine = Files.readString(Path.of(PhenopacketReaderTest.class.
                getResource("PMID_30569521_proband_excluded_features.csv").getPath())).trim();
        // Convert to Set<String>
        Set<String> termSet = Arrays.stream(csvLine.split(","))
                .map(s -> s.replaceAll("^\"|\"$", "")) // Remove surrounding quotes
                .collect(Collectors.toSet());
        assertEquals(termSet, examplePpkt.getExcludedPhenotypes());
    }

    @Test
    void getID() {
        assertEquals("PMID_30569521_proband", examplePpkt.getID());
    }
}