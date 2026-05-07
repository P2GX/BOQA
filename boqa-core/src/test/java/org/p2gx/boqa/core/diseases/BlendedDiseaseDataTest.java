package org.p2gx.boqa.core.diseases;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.p2gx.boqa.core.DiseaseData;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

class BlendedDiseaseDataTest {

    //private static BlendedDiseaseData testBlendedDiseaseData;
    private static DiseaseData testDiseaseData;

    @BeforeAll
    static void setup() throws IOException {
        try (InputStream hpoa = new GZIPInputStream(Objects.requireNonNull(DiseaseDataParseIngestTest.class.
                getResourceAsStream("/org/p2gx/boqa/core/phenotype.v2025-05-06.hpoa.gz")));
             InputStream geneAssociations = new GZIPInputStream(Objects.requireNonNull(DiseaseDataParseIngestTest.class.
                     getResourceAsStream("/org/p2gx/boqa/core/genes_to_disease.v2025-05-06.txt.gz")))) {
            testDiseaseData = DiseaseDataParser.parseDiseaseDataFromHpoaWithGeneAssociations(hpoa, geneAssociations);
        }
    }

    @Test
    void testGeneIdAssociatedDiseases() {
        BlendedDiseaseData testBlendedDiseaseDataAnchorGene = new BlendedDiseaseData(testDiseaseData, List.of("NCBIGene:392255"));
        Set<String> geneIdAssociatedDiseasesExpected = Set.of("OMIM:617898", "OMIM:615360", "OMIM:613094", "OMIM:118100", "OMIM:613703");
        Set<String> geneIdAssociatedDiseasesActual = testBlendedDiseaseDataAnchorGene.geneIdAssociatedDiseases(List.of("NCBIGene:392255"));
        assertEquals(geneIdAssociatedDiseasesExpected, geneIdAssociatedDiseasesActual);
    }

    @Test
    void testSize() {
        BlendedDiseaseData testBlendedDiseaseData = new BlendedDiseaseData(testDiseaseData, List.of("NCBIGene:392255"));
        Integer SizeExpected = 41790;
        Integer SizeActual = testBlendedDiseaseData.size();
        assertEquals(SizeExpected, SizeActual);
    }
}
