package com.github.p2gx.boqa.core.output;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import static org.junit.jupiter.api.Assertions.*;

class JsonResultWriterTest {

    static JsonResultWriter writer;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void setUp() {
        writer = new JsonResultWriter();
    }

    @Test
    void testWriteResults(){
        //TODO add integration test if needed
    }

    @Test
    void testExtractHpVersion() {
        assertEquals("2025-05-06", JsonResultWriter.extractHpVersion(
                "http://purl.obolibrary.org/obo/hp/releases/2025-05-06/hp.json"));
        assertEquals("unknown", JsonResultWriter.extractHpVersion(
                "http://purl.obolibrary.org/obo/hp/releases/225-05-06/hp.json"));
        assertEquals("unknown", JsonResultWriter.extractHpVersion(
                "http://purl.obolion"));
    }

    @Test
    void testReadHpoaVersion() throws IOException {
        // HPOA from resources
        String extractedHpoaVersion;
        try (InputStream annotationStream = new GZIPInputStream(JsonResultWriterTest.class
                .getResourceAsStream("/com/github/p2gx/boqa/core/phenotype.v2025-05-06.hpoa.gz"))) {
            extractedHpoaVersion = JsonResultWriter.readHpoaVersion(annotationStream);
        }
        assertEquals("2025-05-06", extractedHpoaVersion);

        // File without a line starting with #version, such as this phenopacket.
        String invalidFile = JsonResultWriter.readHpoaVersion(JsonResultWriterTest.class
                .getResourceAsStream("/com/github/p2gx/boqa/core/phenopackets/PMID_10077612_FamilyB.json"));
        assertEquals("unknown", invalidFile);
    }
}