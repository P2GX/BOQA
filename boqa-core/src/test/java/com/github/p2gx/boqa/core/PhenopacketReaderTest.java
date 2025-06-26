package com.github.p2gx.boqa.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

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
    void setter() {
    }

    @Test
    void getPhenotypes() throws IOException {
        // Read the line from a file (assuming it's all on one line)
        //String csvLine = Files.readString(Paths.get("terms.csv")).trim();
        String csvLine = Files.readString(Path.of(PhenopacketReaderTest.class.
                getResource("PMID_30569521_proband_features.csv").getPath())).trim();
        // Convert to Set<String>
        Set<String> termSet = Arrays.stream(csvLine.split(","))
                .map(s -> s.replaceAll("^\"|\"$", "")) // Remove surrounding quotes
                .collect(Collectors.toSet());
        assertEquals(termSet, examplePpkt.getPhenotypes());
    }

    @Test
    void getID() {
    }
}