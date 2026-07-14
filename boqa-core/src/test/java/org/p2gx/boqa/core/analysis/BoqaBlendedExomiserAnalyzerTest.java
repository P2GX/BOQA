package org.p2gx.boqa.core.analysis;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.p2gx.boqa.core.DiseaseData;
import org.p2gx.boqa.core.PatientData;
import org.p2gx.boqa.core.TestBase;
import org.p2gx.boqa.core.algorithm.AlgorithmParameters;
import org.p2gx.boqa.core.diseases.DiseaseDataPhenolIngest;
import org.p2gx.boqa.core.patient.QueryDataFromString;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BoqaBlendedExomiserAnalyzerTest {

    private static Ontology hpo;
    private static DiseaseData diseaseData;
    private static Map<String, Set<String>> genesByDisease;
    private static BoqaBlendedExomiserAnalyzer analyzer;

    @BeforeAll
    static void setup() throws IOException {
        hpo = TestBase.hpo();
        HpoDiseases diseases = TestBase.hpoDiseases();
        // Build the same DiseaseData the wrapper builds internally, so the test's disease IDs
        // and features match the ones the analyzer scores against.
        diseaseData = DiseaseDataPhenolIngest.of(hpo, diseases);
        try (InputStream geneAssociations = new GZIPInputStream(Objects.requireNonNull(BoqaBlendedExomiserAnalyzerTest.class
                .getResourceAsStream("/org/p2gx/boqa/core/genes_to_disease.v2025-05-06.txt.gz")))) {
            genesByDisease = parseGenesByDisease(geneAssociations);
        }
        analyzer = new BoqaBlendedExomiserAnalyzer(
                hpo, diseases, genesByDisease, AlgorithmParameters.create(null, null));
    }

    @Test
    void geneDisjointAnchors_produceBlendedPair() {
        // Two anchor diseases with disjoint gene sets pass the shared-gene guard, so at least
        // one blended (paired) entry must appear among the results.
        String anchorDisease1 = mostAnnotatedDiseaseForGene("NCBIGene:5781");
        String anchorDisease2 = mostAnnotatedDiseaseForGene("NCBIGene:9871");
        assumeGeneDisjoint(anchorDisease1, anchorDisease2);
        PatientData patient = patientFromDisease(anchorDisease1);

        BoqaAnalysisResult result = analyzer.analyze(patient, Set.of(anchorDisease1, anchorDisease2), Integer.MAX_VALUE);

        boolean hasBlendedPair = result.boqaResults().stream()
                .anyMatch(r -> r.counts().diseaseId().contains("-"));
        assertTrue(hasBlendedPair, "Gene-disjoint anchor diseases should produce a blended pair");
    }

    @Test
    void sharedGeneAnchors_areNotBlended() {
        // Two anchor diseases that share a gene must fail the guard: only the two singletons
        // are scored, with no blended pair.
        String anchorDisease1 = "OMIM:617898";
        String anchorDisease2 = "OMIM:615360";
        assumeTrue(diseaseData.getDiseaseIds().containsAll(Set.of(anchorDisease1, anchorDisease2)),
                "Both anchor diseases must be annotated");
        assumeFalse(Collections.disjoint(
                        genesByDisease.getOrDefault(anchorDisease1, Set.of()),
                        genesByDisease.getOrDefault(anchorDisease2, Set.of())),
                "Anchor diseases must share a gene for this test");
        PatientData patient = patientFromDisease(anchorDisease1);

        BoqaAnalysisResult result = analyzer.analyze(patient, Set.of(anchorDisease1, anchorDisease2), Integer.MAX_VALUE);

        boolean hasBlendedPair = result.boqaResults().stream()
                .anyMatch(r -> r.counts().diseaseId().contains("-"));
        assertFalse(hasBlendedPair, "Anchor diseases sharing a gene must not be blended");
        assertEquals(2, result.boqaResults().size(), "Only the two anchor singletons should be scored");
    }

    @Test
    void patientMatchingOneAnchor_ranksItAtTop() {
        // A patient whose phenotypes exactly match one anchor disease should rank that disease
        // (or a blend containing it) highest, since blending in the other anchor only adds
        // unmatched features (false negatives) and lowers the score.
        String matchedAnchor = mostAnnotatedDiseaseForGene("NCBIGene:5781");
        String otherAnchor = mostAnnotatedDiseaseForGene("NCBIGene:9871");
        assumeGeneDisjoint(matchedAnchor, otherAnchor);
        PatientData patient = patientFromDisease(matchedAnchor);

        BoqaAnalysisResult result = analyzer.analyze(patient, Set.of(matchedAnchor, otherAnchor), Integer.MAX_VALUE);

        assertFalse(result.boqaResults().isEmpty(), "Analysis should produce results");
        assertSortedByScoreDescending(result);
        String topDiseaseId = result.boqaResults().getFirst().counts().diseaseId();
        assertTrue(idContainsDisease(topDiseaseId, matchedAnchor),
                "Top result should be the matched anchor or a blend containing it, but was: " + topDiseaseId);
    }

    @Test
    void fewerThanTwoAnchors_throws() {
        // ANCHOR_VS_ANCHOR needs at least two anchors; a single anchor or none is a misuse.
        String anchor = mostAnnotatedDiseaseForGene("NCBIGene:5781");
        PatientData patient = patientFromDisease(anchor);

        assertThrows(IllegalArgumentException.class,
                () -> analyzer.analyze(patient, Set.of(anchor), Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class,
                () -> analyzer.analyze(patient, Set.of(), Integer.MAX_VALUE));
    }

    @Test
    void resultsLimit_capsNumberOfResults() {
        String anchorDisease1 = mostAnnotatedDiseaseForGene("NCBIGene:5781");
        String anchorDisease2 = mostAnnotatedDiseaseForGene("NCBIGene:9871");
        assumeGeneDisjoint(anchorDisease1, anchorDisease2);
        PatientData patient = patientFromDisease(anchorDisease1);

        // Two gene-disjoint anchors yield 2 singletons + 1 pair = 3 entries; cap at 2.
        BoqaAnalysisResult result = analyzer.analyze(patient, Set.of(anchorDisease1, anchorDisease2), 2);

        assertTrue(result.boqaResults().size() <= 2, "Result list must respect the results limit");
    }

    // A blended disease ID is "D1-D2"; a singleton is just the disease ID (no hyphen).
    private static boolean idContainsDisease(String diseaseId, String target) {
        return diseaseId.equals(target)
                || diseaseId.startsWith(target + "-")
                || diseaseId.endsWith("-" + target);
    }

    private static void assertSortedByScoreDescending(BoqaAnalysisResult result) {
        List<BoqaResult> results = result.boqaResults();
        for (int i = 1; i < results.size(); i++) {
            assertTrue(results.get(i - 1).boqaScore() >= results.get(i).boqaScore(),
                    "Results should be sorted by score in descending order");
        }
    }

    // Skips the test unless the two diseases are distinct and gene-disjoint (so a blend can form).
    private static void assumeGeneDisjoint(String diseaseId1, String diseaseId2) {
        assumeFalse(diseaseId1.equals(diseaseId2), "Anchor diseases must be distinct");
        assumeTrue(Collections.disjoint(
                        genesByDisease.getOrDefault(diseaseId1, Set.of()),
                        genesByDisease.getOrDefault(diseaseId2, Set.of())),
                "Anchor diseases must be gene-disjoint");
    }

    // Picks the annotated disease for a gene with the most observed features (most discriminative).
    private static String mostAnnotatedDiseaseForGene(String geneId) {
        String disease = genesByDisease.entrySet().stream()
                .filter(entry -> entry.getValue().contains(geneId))
                .map(Map.Entry::getKey)
                .filter(diseaseData.getDiseaseIds()::contains)
                .max(Comparator.comparingInt(d -> diseaseData.getObservedDiseaseFeatures(d).size()))
                .orElse(null);
        assumeFalse(disease == null, "Fixture must contain an annotated disease for gene " + geneId);
        return disease;
    }

    // Builds a patient whose observed HPO terms are exactly the disease's annotated features.
    private static PatientData patientFromDisease(String diseaseId) {
        Set<String> features = diseaseData.getObservedDiseaseFeatures(diseaseId);
        assumeFalse(features.isEmpty(), "Target disease must have observed features");
        return new QueryDataFromString(String.join(",", features), "");
    }

    // Parses a genes_to_disease.txt fixture (tab-separated, with header) into a disease -> gene ID map.
    private static Map<String, Set<String>> parseGenesByDisease(InputStream geneAssociationsStream) throws IOException {
        Map<String, Set<String>> genesByDisease = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(geneAssociationsStream))) {
            reader.lines()
                    .skip(1) // Skip header line
                    .map(line -> line.split("\t"))
                    .filter(cols -> cols.length >= 4)
                    .forEach(cols -> {
                        String geneId = cols[0];
                        String diseaseId = cols[3];
                        genesByDisease.computeIfAbsent(diseaseId, k -> new HashSet<>()).add(geneId);
                    });
        }
        return genesByDisease;
    }
}
