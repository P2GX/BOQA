package org.p2gx.boqa.core.diseases;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.p2gx.boqa.core.DiseaseData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

class BlendedDiseaseNewDiseaseResultPhenotypeOnlyDataTest {

    private static final Set<String> ANCHOR_DISEASES = Set.of(
            "OMIM:617898", "OMIM:615360", "OMIM:613094", "OMIM:118100", "OMIM:613703");

    private static DiseaseData testDiseaseData;

    // Disease-gene associations for setting up test fixtures; genes are a CLI concern in production,
    // so this test parses the fixture itself instead of relying on DiseaseData for gene lookups.
    private static Map<String, Set<String>> geneIdsByDisease;

    @BeforeAll
    static void setup() throws IOException {
        try (InputStream hpoa = new GZIPInputStream(Objects.requireNonNull(PhenotypeOnlyDataParseIngestTest.class.
                getResourceAsStream("/org/p2gx/boqa/core/phenotype.v2025-05-06.hpoa.gz")))) {
            testDiseaseData = DiseaseDataParser.parseDiseaseDataFromHpoa(hpoa);
        }
        try (InputStream geneAssociations = new GZIPInputStream(Objects.requireNonNull(PhenotypeOnlyDataParseIngestTest.class.
                getResourceAsStream("/org/p2gx/boqa/core/genes_to_disease.v2025-05-06.txt.gz")))) {
            geneIdsByDisease = parseGeneIdsByDisease(geneAssociations);
        }
    }

    // Parses a genes_to_disease.txt fixture (tab-separated, with header) into a disease -> gene ID map.
    private static Map<String, Set<String>> parseGeneIdsByDisease(InputStream geneAssociationsStream) throws IOException {
        Map<String, Set<String>> geneIdsByDisease = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(geneAssociationsStream))) {
            reader.lines()
                    .skip(1) // Skip header line
                    .map(line -> line.split("\t"))
                    .filter(cols -> cols.length >= 4)
                    .forEach(cols -> {
                        String geneId = cols[0];
                        String diseaseId = cols[3];
                        geneIdsByDisease.computeIfAbsent(diseaseId, k -> new HashSet<>()).add(geneId);
                    });
        }
        return geneIdsByDisease;
    }

    // Resolves the disease IDs associated with the given genes, for setting up test fixtures.
    private static Set<String> diseaseIdsForGenes(DiseaseData diseaseData, Collection<String> geneIds) {
        return diseaseData.getDiseaseIds().stream()
                .filter(d -> geneIds.stream().anyMatch(geneId -> geneIdsByDisease.getOrDefault(d, Set.of()).contains(geneId)))
                .collect(Collectors.toSet());
    }

    private static BiPredicate<String, String> disjointGenes() {
        return (d1, d2) -> Collections.disjoint(
                geneIdsByDisease.getOrDefault(d1, Set.of()), geneIdsByDisease.getOrDefault(d2, Set.of()));
    }

    @Test
    void testAnchorVsAllSize() {
        BlendedDiseaseData blendedDiseaseData = new BlendedDiseaseData(testDiseaseData, ANCHOR_DISEASES);
        assertEquals(41790, blendedDiseaseData.size());
    }

    @Test
    void testAnchorVsAnchorSize() {
        BlendedDiseaseData blendedDiseaseData = new BlendedDiseaseData(testDiseaseData, ANCHOR_DISEASES,
                BlendedDiseaseData.PairingStrategy.ANCHOR_VS_ANCHOR, disjointGenes());
        // All 5 anchor diseases share NCBIGene:392255, so no cross-gene pairs form — only 5 singletons
        assertEquals(5, blendedDiseaseData.size());
    }

    @Test
    void testAnchorVsAllPairingContent() {
        BlendedDiseaseData blendedDiseaseData = new BlendedDiseaseData(testDiseaseData, ANCHOR_DISEASES);
        for (String diseaseId : blendedDiseaseData.getDiseaseIds()) {
            if (!ANCHOR_DISEASES.contains(diseaseId)) {
                List<String> components = blendedDiseaseData.componentsOf(diseaseId);
                assertTrue(ANCHOR_DISEASES.contains(components.get(0)),
                        "First part of blended pair should be an anchor disease: " + components.get(0));
                assertFalse(ANCHOR_DISEASES.contains(components.get(1)),
                        "Second part of blended pair should not be an anchor disease: " + components.get(1));
            }
        }
    }

    @Test
    void testAnchorVsAnchorPairingContent() {
        // Use two genes with distinct disease sets to ensure cross-gene pairs are formed
        Set<String> anchorDiseases = diseaseIdsForGenes(testDiseaseData, List.of("NCBIGene:5781", "NCBIGene:9871"));
        BlendedDiseaseData blendedDiseaseData = new BlendedDiseaseData(testDiseaseData, anchorDiseases,
                BlendedDiseaseData.PairingStrategy.ANCHOR_VS_ANCHOR, disjointGenes());
        Set<String> diseaseIds = blendedDiseaseData.getDiseaseIds();
        // At least one cross-gene pair must be present
        assertTrue(diseaseIds.size() > anchorDiseases.size(), "Expected at least one blended pair to be formed");
        // All blended pairs must have both parts in the anchor set and disjoint gene associations
        for (String diseaseId : diseaseIds) {
            if (!anchorDiseases.contains(diseaseId)) {
                List<String> components = blendedDiseaseData.componentsOf(diseaseId);
                assertTrue(anchorDiseases.contains(components.get(0)),
                        "First part of blended pair should be an anchor disease: " + components.get(0));
                assertTrue(anchorDiseases.contains(components.get(1)),
                        "Second part of blended pair should be an anchor disease: " + components.get(1));
                assertTrue(Collections.disjoint(
                                geneIdsByDisease.getOrDefault(components.get(0), Set.of()),
                                geneIdsByDisease.getOrDefault(components.get(1), Set.of())),
                        "Paired diseases should not share a gene: " + diseaseId);
            }
        }
    }

    @Test
    void testComponentsOfSingletonAndBlendedDisease() {
        Set<String> anchorDiseases = diseaseIdsForGenes(testDiseaseData, List.of("NCBIGene:5781", "NCBIGene:9871"));
        BlendedDiseaseData blendedDiseaseData = new BlendedDiseaseData(testDiseaseData, anchorDiseases,
                BlendedDiseaseData.PairingStrategy.ANCHOR_VS_ANCHOR, disjointGenes());

        // A singleton anchor is composed of itself
        String anchorDiseaseId = anchorDiseases.iterator().next();
        assertEquals(List.of(anchorDiseaseId), blendedDiseaseData.componentsOf(anchorDiseaseId));

        // A blended pair is composed of its two diseases, and reports them in pairing order
        String blendedDiseaseId = blendedDiseaseData.getDiseaseIds().stream()
                .filter(diseaseId -> !anchorDiseases.contains(diseaseId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected at least one blended pair to be formed"));
        List<String> components = blendedDiseaseData.componentsOf(blendedDiseaseId);
        assertEquals(2, components.size());
        assertEquals(blendedDiseaseId, components.get(0) + '-' + components.get(1));
    }

    @Test
    void testComponentsOfUnknownDiseaseThrows() {
        BlendedDiseaseData blendedDiseaseData = new BlendedDiseaseData(testDiseaseData, ANCHOR_DISEASES,
                BlendedDiseaseData.PairingStrategy.ANCHOR_VS_ANCHOR, disjointGenes());
        assertThrows(IllegalArgumentException.class, () -> blendedDiseaseData.componentsOf("OMIM:000000"));
    }
}
