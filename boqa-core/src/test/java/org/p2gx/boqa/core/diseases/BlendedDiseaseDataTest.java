package org.p2gx.boqa.core.diseases;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.p2gx.boqa.core.DiseaseData;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

class BlendedDiseaseDataTest {

    private static final Set<String> ANCHOR_DISEASES = Set.of(
            "OMIM:617898", "OMIM:615360", "OMIM:613094", "OMIM:118100", "OMIM:613703");

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

    // Resolves the disease IDs associated with the given genes, for setting up test fixtures.
    private static Set<String> diseaseIdsForGenes(DiseaseData diseaseData, Collection<String> geneIds) {
        return diseaseData.getDiseaseIds().stream()
                .filter(d -> geneIds.stream().anyMatch(geneId -> diseaseData.getDiseaseGeneIds(d).contains(geneId)))
                .collect(Collectors.toSet());
    }

    private static BiPredicate<String, String> disjointGenes(DiseaseData diseaseData) {
        return (d1, d2) -> Collections.disjoint(diseaseData.getDiseaseGeneIds(d1), diseaseData.getDiseaseGeneIds(d2));
    }

    @Test
    void testAnchorVsAllSize() {
        BlendedDiseaseData blendedDiseaseData = new BlendedDiseaseData(testDiseaseData, ANCHOR_DISEASES);
        assertEquals(41790, blendedDiseaseData.size());
    }

    @Test
    void testAnchorVsAnchorSize() {
        BlendedDiseaseData blendedDiseaseData = new BlendedDiseaseData(testDiseaseData, ANCHOR_DISEASES,
                BlendedDiseaseData.PairingStrategy.ANCHOR_VS_ANCHOR, disjointGenes(testDiseaseData));
        // All 5 anchor diseases share NCBIGene:392255, so no cross-gene pairs form — only 5 singletons
        assertEquals(5, blendedDiseaseData.size());
    }

    @Test
    void testAnchorVsAllPairingContent() {
        BlendedDiseaseData blendedDiseaseData = new BlendedDiseaseData(testDiseaseData, ANCHOR_DISEASES);
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
        Set<String> anchorDiseases = diseaseIdsForGenes(testDiseaseData, List.of("NCBIGene:5781", "NCBIGene:9871"));
        BlendedDiseaseData blendedDiseaseData = new BlendedDiseaseData(testDiseaseData, anchorDiseases,
                BlendedDiseaseData.PairingStrategy.ANCHOR_VS_ANCHOR, disjointGenes(testDiseaseData));
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
