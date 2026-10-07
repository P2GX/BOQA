package org.p2gx.boqa.core.diseases;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests which candidates {@link CandidateDisease#createCandidateDiseases(List)} builds from anchor diseases.
 */
class CandidateDiseaseTest {

    private static TargetDisease.PhenotypeAndGene anchor(String diseaseId, String geneId, String geneSymbol,
                                                         String hpoId) {
        return new TargetDisease.PhenotypeAndGene(diseaseId, diseaseId + " label", geneId, geneSymbol,
                Set.of(TermId.of(hpoId)));
    }

    private static long countBlended(List<CandidateDisease> candidates) {
        return candidates.stream().filter(CandidateDisease.BlendedDisease.class::isInstance).count();
    }

    @Test
    void anchorsOnDistinctGenesAreAlsoBlended() {
        // Neurofibromatosis 1 (NF1) and Noonan syndrome 1 (PTPN11)
        List<TargetDisease.PhenotypeAndGene> anchors = List.of(
                anchor("OMIM:162200", "NCBIGene:4763", "NF1", "HP:0000957"),
                anchor("OMIM:163950", "NCBIGene:5781", "PTPN11", "HP:0000316"));

        List<CandidateDisease> candidates = CandidateDisease.createCandidateDiseases(anchors);

        // Both single diseases, plus their blend
        assertEquals(3, candidates.size());
        assertEquals(1, countBlended(candidates));

        // The blend is made of exactly the two anchors
        CandidateDisease blend = candidates.stream()
                .filter(CandidateDisease.BlendedDisease.class::isInstance)
                .findFirst().orElseThrow();
        assertEquals(Set.of("OMIM:162200", "OMIM:163950"), blend.diseaseId());
    }

    @Test
    void anchorsOnTheSameGeneAreNotBlended() {
        // Two diseases of PTPN11: Noonan syndrome 1 and LEOPARD syndrome 1
        List<TargetDisease.PhenotypeAndGene> anchors = List.of(
                anchor("OMIM:163950", "NCBIGene:5781", "PTPN11", "HP:0000316"),
                anchor("OMIM:151100", "NCBIGene:5781", "PTPN11", "HP:0001480"));

        List<CandidateDisease> candidates = CandidateDisease.createCandidateDiseases(anchors);

        // Only the two single diseases
        assertEquals(2, candidates.size());
        assertEquals(0, countBlended(candidates));
    }
}
