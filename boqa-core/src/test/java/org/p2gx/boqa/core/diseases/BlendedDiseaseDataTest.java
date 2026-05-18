package org.p2gx.boqa.core.diseases;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.p2gx.boqa.core.DiseaseData;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

class BlendedDiseaseDataTest {

    private static final Set<String> ANCHOR_DISEASES = Set.of(
            "OMIM:617898", "OMIM:615360", "OMIM:613094", "OMIM:118100", "OMIM:613703");
    private static final List<String> ANCHOR_GENE = List.of("NCBIGene:392255");

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
        BlendedDiseaseData blendedDiseaseData = new BlendedDiseaseData(testDiseaseData, ANCHOR_GENE);
        assertEquals(ANCHOR_DISEASES, blendedDiseaseData.geneIdAssociatedDiseases(ANCHOR_GENE));
    }

    @Test
    void testAnchorVsAllSize() {
        BlendedDiseaseData blendedDiseaseData = new BlendedDiseaseData(testDiseaseData, ANCHOR_GENE);
        assertEquals(41790, blendedDiseaseData.size());
    }

    @Test
    void testAnchorVsAllThrowsWithMultipleGenes() {
        assertThrows(IllegalArgumentException.class, () ->
                new BlendedDiseaseData(testDiseaseData, List.of("NCBIGene:392255", "NCBIGene:1234"),
                        BlendedDiseaseData.PairingStrategy.ANCHOR_VS_ALL));
    }

    @Test
    void testAnchorVsAnchorSize() {
        BlendedDiseaseData blendedDiseaseData = new BlendedDiseaseData(testDiseaseData, ANCHOR_GENE,
                BlendedDiseaseData.PairingStrategy.ANCHOR_VS_ANCHOR);
        // All 5 anchor diseases share NCBIGene:392255, so no cross-gene pairs form — only 5 singletons
        assertEquals(5, blendedDiseaseData.size());
    }

    @Test
    void testAnchorVsAllPairingContent() {
        BlendedDiseaseData blendedDiseaseData = new BlendedDiseaseData(testDiseaseData, ANCHOR_GENE,
                BlendedDiseaseData.PairingStrategy.ANCHOR_VS_ALL);
        for (String diseaseId : blendedDiseaseData.getDiseaseIds()) {
            if (!ANCHOR_DISEASES.contains(diseaseId)) {
                String[] parts = diseaseId.split("-", 2);
                assertTrue(ANCHOR_DISEASES.contains(parts[0]),
                        "First part of blended pair should be an anchor disease: " + parts[0]);
                assertFalse(ANCHOR_DISEASES.contains(parts[1]),
                        "Second part of blended pair should not be an anchor disease: " + parts[1]);
            }
        }
    }

    @Test
    void testAnchorVsAnchorPairingContent() {
        // Use two genes with distinct disease sets to ensure cross-gene pairs are formed
        List<String> twoGenes = List.of("NCBIGene:5781", "NCBIGene:9871");
        BlendedDiseaseData blendedDiseaseData = new BlendedDiseaseData(testDiseaseData, twoGenes,
                BlendedDiseaseData.PairingStrategy.ANCHOR_VS_ANCHOR);
        Set<String> anchorDiseases = blendedDiseaseData.geneIdAssociatedDiseases(twoGenes);
        Set<String> diseaseIds = blendedDiseaseData.getDiseaseIds();
        // At least one cross-gene pair must be present
        assertTrue(diseaseIds.size() > anchorDiseases.size(), "Expected at least one blended pair to be formed");
        // All blended pairs must have both parts in the anchor set and disjoint gene associations
        for (String diseaseId : diseaseIds) {
            if (!anchorDiseases.contains(diseaseId)) {
                String[] parts = diseaseId.split("-", 2);
                assertTrue(anchorDiseases.contains(parts[0]),
                        "First part of blended pair should be an anchor disease: " + parts[0]);
                assertTrue(anchorDiseases.contains(parts[1]),
                        "Second part of blended pair should be an anchor disease: " + parts[1]);
                assertTrue(Collections.disjoint(
                                testDiseaseData.getDiseaseGeneIds(parts[0]),
                                testDiseaseData.getDiseaseGeneIds(parts[1])),
                        "Paired diseases should not share a gene: " + diseaseId);
            }
        }
    }
}
