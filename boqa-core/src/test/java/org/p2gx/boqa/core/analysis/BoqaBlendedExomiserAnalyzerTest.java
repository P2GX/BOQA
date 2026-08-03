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

class BoqaBlendedExomiserAnalyzerTest {

    // Two genes whose diseases are annotated and do not overlap, so their anchors can blend.
    private static final String FBN1 = "NCBIGene:5781";
    private static final String OTHER_GENE = "NCBIGene:9871";

    private static DiseaseData diseaseData;
    private static Map<String, Set<String>> genesByDisease;
    private static BoqaBlendedExomiserAnalyser analyzer;

  /*  @BeforeAll
    static void setup() throws IOException {
        Ontology hpo = TestBase.hpo();
        HpoDiseases diseases = TestBase.hpoDiseases();
        // Build the same DiseaseData the wrapper builds internally, so the test's disease IDs
        // and features match the ones the analyzer scores against.
        diseaseData = DiseaseDataPhenolIngest.of(hpo, diseases);
        try (InputStream geneAssociations = new GZIPInputStream(Objects.requireNonNull(BoqaBlendedExomiserAnalyzerTest.class
                .getResourceAsStream("/org/p2gx/boqa/core/genes_to_disease.v2025-05-06.txt.gz")))) {
            genesByDisease = parseGenesByDisease(geneAssociations);
        }
        analyzer = new BoqaBlendedExomiserAnalyser(hpo, diseases);
    }

    @Test
    void anchorsOnDifferentGenes_produceBlendedPair() {
        // Two candidates whose genes differ pass the shared-gene guard, so a blended entry must
        // appear, and it must report both candidates and both their counts.
        TargetDisease anchor1 = anchorOnGene(FBN1);
        TargetDisease anchor2 = anchorOnGene(OTHER_GENE);
        PatientData patient = patientFromDisease(anchor1.diseaseId());

        List<BlendedResult> results = analyzer.analyze(patient, Set.of(anchor1, anchor2), Integer.MAX_VALUE);

        BlendedResult blended = results.stream()
                .filter(BlendedResult::isBlended)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Anchors on differing genes should produce a blended pair"));
        assertEquals(Set.of(anchor1, anchor2), Set.copyOf(blended.components()));
        assertEquals(2, blended.componentCounts().size());
    }

    @Test
    void anchorsOnSameGene_areNotBlended() {
        // Two candidates put forward by the same gene must fail the guard: only the two
        // singletons are scored, with no blended pair.
        List<String> sameGeneDiseases = diseasesForGene(FBN1);
        assumeFalse(sameGeneDiseases.size() < 2, "Fixture must have two diseases for the same gene");
        TargetDisease anchor1 = new TargetDisease(sameGeneDiseases.get(0), "first", 5781, "FBN1", Set.of());
        TargetDisease anchor2 = new TargetDisease(sameGeneDiseases.get(1), "second", 5781, "FBN1", Set.of());
        PatientData patient = patientFromDisease(anchor1.diseaseId());

        List<BlendedResult> results = analyzer.analyze(patient, Set.of(anchor1, anchor2), Integer.MAX_VALUE);

        assertTrue(results.stream().noneMatch(BlendedResult::isBlended),
                "Candidates sharing a gene must not be blended");
        assertEquals(2, results.size(), "Only the two anchor singletons should be scored");
    }

    @Test
    void singleDiseaseResult_reportsItselfAsItsOnlyComponent() {
        TargetDisease anchor1 = anchorOnGene(FBN1);
        TargetDisease anchor2 = anchorOnGene(OTHER_GENE);
        PatientData patient = patientFromDisease(anchor1.diseaseId());

        List<BlendedResult> results = analyzer.analyze(patient, Set.of(anchor1, anchor2), Integer.MAX_VALUE);

        BlendedResult single = results.stream()
                .filter(result -> !result.isBlended())
                .findFirst()
                .orElseThrow(() -> new AssertionError("The anchor singletons should be scored too"));
        // A single disease is its own component, and is scored on its own counts
        assertEquals(single.components().getFirst().diseaseId(), single.blendedCounts().diseaseId());
        assertEquals(List.of(single.blendedCounts()), single.componentCounts());
    }

    @Test
    void patientMatchingOneAnchor_ranksItAtTop() {
        // A patient whose phenotypes exactly match one candidate should rank that disease (or a
        // blend containing it) highest, since blending in the other candidate only adds unmatched
        // features (false negatives) and lowers the score.
        TargetDisease matchedAnchor = anchorOnGene(FBN1);
        TargetDisease otherAnchor = anchorOnGene(OTHER_GENE);
        PatientData patient = patientFromDisease(matchedAnchor.diseaseId());

        List<BlendedResult> results = analyzer.analyze(patient, Set.of(matchedAnchor, otherAnchor), Integer.MAX_VALUE);

        assertFalse(results.isEmpty(), "Analysis should produce results");
        assertSortedByScoreDescending(results);
        assertTrue(results.getFirst().components().contains(matchedAnchor),
                "Top result should be the matched candidate or a blend containing it");
    }

    @Test
    void fewerThanTwoAnchors_throws() {
        // Pairing anchors against each other needs at least two; a single anchor or none is a misuse.
        TargetDisease anchor = anchorOnGene(FBN1);
        PatientData patient = patientFromDisease(anchor.diseaseId());

        assertThrows(IllegalArgumentException.class,
                () -> analyzer.analyze(patient, Set.of(anchor), Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class,
                () -> analyzer.analyze(patient, Set.of(), Integer.MAX_VALUE));
    }

    @Test
    void diseaseAnchoredOnTwoGenes_throws() {
        // The same disease cannot be anchored twice, since an entry is keyed by its disease ID.
        String diseaseId = anchorOnGene(FBN1).diseaseId();
        TargetDisease anchor1 = new TargetDisease(diseaseId, "label", 5781, "FBN1", Set.of());
        TargetDisease anchor2 = new TargetDisease(diseaseId, "label", 9871, "OTHER", Set.of());
        PatientData patient = patientFromDisease(diseaseId);

        assertThrows(IllegalArgumentException.class,
                () -> analyzer.analyze(patient, Set.of(anchor1, anchor2), Integer.MAX_VALUE));
    }

    @Test
    void resultsLimit_capsResultsButKeepsComponentCounts() {
        TargetDisease anchor1 = anchorOnGene(FBN1);
        TargetDisease anchor2 = anchorOnGene(OTHER_GENE);
        PatientData patient = patientFromDisease(anchor1.diseaseId());

        // Two anchors on differing genes yield 2 singletons + 1 pair = 3 entries; cap at 2, which
        // keeps the blend but drops one of the singletons it is made of.
        List<BlendedResult> results = analyzer.analyze(patient, Set.of(anchor1, anchor2), 2);

        assertEquals(2, results.size(), "Result list must respect the results limit");
        // The dropped singleton's counts must still be reported by the blend, so the entries have
        // to be assembled before the limit is applied. Were they assembled afterwards, the counts
        // of the cut singleton would be missing and BlendedResult would reject them.
        BlendedResult blended = results.stream()
                .filter(BlendedResult::isBlended)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected the blend to survive a cap of 2"));
        assertEquals(2, blended.componentCounts().size());
    }

    private static void assertSortedByScoreDescending(List<BlendedResult> results) {
        for (int i = 1; i < results.size(); i++) {
            assertTrue(results.get(i - 1).score() >= results.get(i).score(),
                    "Results should be sorted by score in descending order");
        }
    }

    // Builds the candidate a gene puts forward: its annotated disease with the most observed
    // features (the most discriminative one), as Exomiser would supply it.
    private static TargetDisease anchorOnGene(String geneId) {
        List<String> diseases = diseasesForGene(geneId);
        assumeFalse(diseases.isEmpty(), "Fixture must contain an annotated disease for gene " + geneId);
        String diseaseId = diseases.getFirst();
        return new TargetDisease(diseaseId, diseaseId, Integer.parseInt(geneId.substring("NCBIGene:".length())), geneId, Set.of());
    }

    // The annotated diseases of a gene, most observed features first.
    private static List<String> diseasesForGene(String geneId) {
        return genesByDisease.entrySet().stream()
                .filter(entry -> entry.getValue().contains(geneId))
                .map(Map.Entry::getKey)
                .filter(diseaseData.getDiseaseIds()::contains)
                .sorted(Comparator.comparingInt((String d) -> diseaseData.getObservedDiseaseFeatures(d).size()).reversed())
                .toList();
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
    } */
}
