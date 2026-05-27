package org.p2gx.boqa.core.analysis;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.p2gx.boqa.core.DiseaseData;
import org.p2gx.boqa.core.TestBase;
import org.p2gx.boqa.core.algorithm.AlgorithmParameters;
import org.p2gx.boqa.core.algorithm.BoqaCounts;
import org.p2gx.boqa.core.algorithm.BoqaSetCounter;
import org.p2gx.boqa.core.diseases.DiseaseDataPhenolIngest;
import org.p2gx.boqa.core.diseases.DiseaseMerger;
import org.p2gx.boqa.core.patient.PhenopacketData;
import org.p2gx.boqa.core.patient.PhenopacketReader;
import org.phenopackets.schema.v2.core.Disease;
import org.phenopackets.schema.v2.core.OntologyClass;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

// "Qd" = "Quick and Dirty" — this test exercises the blended/melded BOQA scoring pipeline.
// A "melded" phenopacket represents a patient whose phenotype is a blend of TWO diseases
// (e.g., a digenic patient). The test verifies that scoring against the merged disease
// produces a sensible result (i.e., the blended disease should win).
public class QdTest extends TestBase  {

    // Same numeric value as the default (1/19077), but spelled out explicitly.
    // α = P(feature observed in patient | feature absent in disease) — false positive rate.
    private Double alpha = 5.241914347119568E-05;
    // β = P(feature absent in patient | feature present in disease) — false negative rate.
    // High value (0.9) means disease features are often NOT reported in clinical records.
    private Double beta =.9;

    /**
     * Runs a "melded BOQA" analysis on a single phenopacket that encodes a patient
     * whose phenotype is a blend of exactly TWO diseases (digenic / dual-diagnosis case).
     *
     * <p>Steps:
     * <ol>
     *   <li>Parse the phenopacket and extract the patient's observed HPO terms.</li>
     *   <li>Extract the two known disease IDs from the phenopacket's disease list.</li>
     *   <li>Build a mini disease database with 3 entries:
     *       disease A, disease B, and a merged "A+B" disease whose annotations are
     *       the union of A's and B's HPO terms.</li>
     *   <li>Run BOQA against all 3 diseases and print the raw counts + normalized scores.</li>
     * </ol>
     *
     * <p>This is a sanity check, not a regression test — no score values are asserted.
     * The expectation is that the merged disease ranks first.
     *
     * <p><b>Example intermediate state (hypothetical):</b>
     * <pre>
     * Phenopacket diseases: ["OMIM:123456", "OMIM:789012"]
     *
     * mergedList before DiseaseMerger:
     *   [HpoDisease("OMIM:123456", "Noonan syndrome", [HP:0004322, HP:0001631, ...]),
     *    HpoDisease("OMIM:789012", "Marfan syndrome", [HP:0001519, HP:0004326, ...])]
     *
     * After DiseaseMerger.merge():
     *   merged = HpoDisease("OMIM:123456-OMIM:789012", "Noonan syndrome - Marfan syndrome",
     *                        union of both annotation sets)
     *
     * mergedList after add(merged): size == 3
     *
     * BOQA output (illustrative, not real numbers):
     *   BoqaResult(diseaseId="OMIM:123456-OMIM:789012", score=0.82)   ← merged wins
     *   BoqaResult(diseaseId="OMIM:123456",             score=0.11)
     *   BoqaResult(diseaseId="OMIM:789012",             score=0.07)
     * </pre>
     */
    private void runMeldedBoqa(Path ppktPath) throws IOException {
        // Step 1: parse the phenopacket JSON and wrap it as PatientData.
        // PhenopacketData extracts observed HPO terms and resolves any outdated term IDs.
        var ppkt = PhenopacketReader.readPhenopacket(ppktPath);
        var ppktData = new PhenopacketData(ppkt, hpo());

        // Step 2: pull the disease IDs encoded in the phenopacket (the ground-truth diagnoses).
        // For melded patients these are always exactly 2 — one per component disease.
        Set<String> relevantIds = ppkt.getDiseasesList()
                .stream()
                .map(Disease::getTerm)
                .map(OntologyClass::getId).collect(Collectors.toSet());
        assertEquals(2, relevantIds.size());  // guard: this test only makes sense for dual-disease patients

        // Step 3: look up the two diseases in the full HPOA database (loaded by TestBase).
        List<HpoDisease> diseaseList = hpoDiseases().stream()
                .filter(d -> relevantIds.contains(d.id().getValue()))
                .toList();

        // Step 4: create the merged disease and build a 3-entry list:
        //   [diseaseA, diseaseB, merged(A+B)]
        // DiseaseMerger.merge() unions the HPO annotations; the merged ID is "idA-idB".
        List<HpoDisease> mergedList = new ArrayList<>(diseaseList);
        HpoDisease merged = DiseaseMerger.merge(diseaseList);  // throws if list.size() != 2
        mergedList.add(merged);
        assertEquals(3, mergedList.size());  // sanity: 2 originals + 1 blended

        // Step 5: wrap the 3 diseases in an HpoDiseases container (phenol's collection type)
        // and build a DiseaseData backed by only these 3 entries.
        // This scopes the BOQA search to just the 3 candidates, not all ~10,000 OMIM diseases.
        String version = "melded version";
        HpoDiseases filtered =  HpoDiseases.of(version, mergedList);
        DiseaseData diseaseData = DiseaseDataPhenolIngest.of(hpo(), filtered);

        // Step 6: build the BoqaSetCounter — pre-computes disease layers for all 3 diseases.
        BoqaSetCounter counter = new BoqaSetCounter(diseaseData, hpo());

        // Step 7: print raw BoqaCounts (tp, fp, tn, fn) for each of the 3 diseases before scoring.
        System.out.println("  Raw counts (before scoring):");
        System.out.printf("  %-50s  %4s  %4s  %4s  %4s%n", "Disease", "tp", "fp", "tn", "fn");
        System.out.println("  " + "-".repeat(70));
        for (String did : counter.getDiseaseIds()) {
            BoqaCounts c = counter.computeBoqaCounts(did, ppktData);
            System.out.printf("  %-50s  %4d  %4d  %4d  %4d%n",
                    truncate(c.diseases().getFirst().id() + " " + c.diseases().getFirst().label(), 50),
                    c.tpBoqaCount(), c.fpBoqaCount(), c.tnBoqaCount(), c.fnBoqaCount());
        }

        // Step 8: run the full scoring pipeline — compute raw log scores, normalize with
        // log-sum-exp, sort descending. Limit=100 (more than 3, so all results are returned).
        AlgorithmParameters params = AlgorithmParameters.create(alpha, beta);
        BoqaAnalysisResult result = BoqaPatientAnalyzer.computeBoqaResults(
                ppktData, counter, 100, params);

        // Step 9: print normalized probability scores (sum to 1.0), sorted best-first.
        System.out.println();
        System.out.println("  Normalized scores (sorted best-first):");
        System.out.printf("  %-10s  %-50s%n", "Score", "Disease");
        System.out.println("  " + "-".repeat(62));
        for (var r : result.boqaResults()) {
            System.out.printf("  %-10.6f  %-50s%n",
                    r.boqaScore(),
                    truncate(r.counts().diseases().getFirst().id() + " " + r.counts().diseases().getFirst().label(), 50));
        }
        System.out.println();
    }

    /** Truncates {@code s} to at most {@code maxLen} characters, appending "…" if cut. */
    private static String truncate(String s, int maxLen) {
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 1) + "…";
    }

    @Test
    public void testSanityCheck() throws IOException, URISyntaxException {
        Path meldedPpktDir = Paths.get(
            getClass().getResource("/org/p2gx/boqa/core/melded-phenopackets").toURI());
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(meldedPpktDir, "*.json")) {
            for (Path entry : stream) {
                System.out.println("******* Testing " + entry.getFileName() + " *******");
                runMeldedBoqa(entry);
            }
        }
    }

}
