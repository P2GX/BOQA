package org.p2gx.boqa.core.genes;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhenotypeGeneAssociationsTest {

    private static DiseaseGeneAssociations geneAssociations;

    @BeforeAll
    static void setup() throws IOException {
        try (InputStream stream = new GZIPInputStream(Objects.requireNonNull(PhenotypeGeneAssociationsTest.class
                .getResourceAsStream("/org/p2gx/boqa/core/genes_to_disease.v2025-05-06.txt.gz")))) {
            geneAssociations = DiseaseGeneAssociations.fromStream(stream);
        }
    }

    @Test
    void geneIdsForDisease_returnsSingleGene() {
        assertEquals(Set.of("NCBIGene:64170"), geneAssociations.geneIdsForDisease("OMIM:212050"));
    }

    @Test
    void geneIdsForDisease_returnsSingleGene_BBS2() {
        assertEquals(Set.of("NCBIGene:583"), geneAssociations.geneIdsForDisease("OMIM:616562"));
    }

    @Test
    void geneIdsForDisease_returnsGeneForOPA1() {
        assertEquals(Set.of("NCBIGene:4976"), geneAssociations.geneIdsForDisease("OMIM:165500"));
    }

    @Test
    void geneIdsForDisease_returnsEmptySet_forDiseaseAbsentFromGeneFile() {
        assertTrue(geneAssociations.geneIdsForDisease("OMIM:100070").isEmpty());
    }

    @Test
    void allGeneIds_containsGenesFromMultipleDiseases() {
        Set<String> allGeneIds = geneAssociations.allGeneIds();
        assertTrue(allGeneIds.containsAll(Set.of("NCBIGene:64170", "NCBIGene:583", "NCBIGene:4976")));
    }

    @Test
    void diseaseIdsForGenes_returnsAssociatedDiseases() {
        // NCBIGene:392255 (GDF6) is associated with both OMIM and ORPHA diseases in the gene file
        Set<String> expectedDiseases = Set.of(
                "OMIM:617898", "OMIM:615360", "OMIM:613094", "OMIM:118100", "OMIM:613703",
                "ORPHA:65", "ORPHA:98938", "ORPHA:2345");
        assertEquals(expectedDiseases, geneAssociations.diseaseIdsForGenes(List.of("NCBIGene:392255")));
    }
}
