package org.p2gx.boqa.core.analysis;

import org.junit.jupiter.api.Test;
import org.p2gx.boqa.core.algorithm.BoqaCounts;
import org.p2gx.boqa.core.diseases.TargetDisease;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BlendedResultTest {

    private static final TargetDisease MARFAN =
            new TargetDisease("OMIM:154700", "Marfan syndrome", 2200, "FBN1", Set.of());
    private static final TargetDisease GLUTARIC_ACIDURIA =
            new TargetDisease("OMIM:231670", "Glutaric acidemia I", 2639, "GCDH",Set.of());

    private static final BoqaCounts MARFAN_COUNTS =
            new BoqaCounts("OMIM:154700", "Marfan syndrome", 3, 1, 10, 2);
    private static final BoqaCounts GLUTARIC_ACIDURIA_COUNTS =
            new BoqaCounts("OMIM:231670", "Glutaric acidemia I", 2, 2, 9, 3);
    private static final BoqaCounts BLENDED_COUNTS =
            new BoqaCounts("OMIM:154700-OMIM:231670", "Marfan syndrome + Glutaric acidemia I", 5, 1, 8, 1);

    @Test
    void testSingleDiseaseIsNotBlended() {
        BlendedResult result = new BlendedResult(
                List.of(MARFAN), List.of(MARFAN_COUNTS), MARFAN_COUNTS, 0.75);
        assertFalse(result.isBlended());
    }

    @Test
    void testTwoDiseasesAreBlended() {
        BlendedResult result = new BlendedResult(
                List.of(MARFAN, GLUTARIC_ACIDURIA),
                List.of(MARFAN_COUNTS, GLUTARIC_ACIDURIA_COUNTS),
                BLENDED_COUNTS, 0.9);
        assertTrue(result.isBlended());
        // Both genes stay visible, so the caller can map the blend onto its per-gene results
        assertEquals(List.of(2200, 2639), result.components().stream().map(TargetDisease::geneId).toList());
    }

    @Test
    void testCountsMissingForSomeComponentThrows() {
        assertThrows(IllegalArgumentException.class, () -> new BlendedResult(
                List.of(MARFAN, GLUTARIC_ACIDURIA), List.of(MARFAN_COUNTS), BLENDED_COUNTS, 0.9));
    }

    @Test
    void testResultWithoutComponentsThrows() {
        assertThrows(IllegalArgumentException.class, () -> new BlendedResult(
                List.of(), List.of(), BLENDED_COUNTS, 0.9));
    }

    @Test
    void testComponentsAreDecoupledFromTheCallersLists() {
        List<TargetDisease> components = new ArrayList<>(List.of(MARFAN));
        List<BoqaCounts> componentCounts = new ArrayList<>(List.of(MARFAN_COUNTS));
        BlendedResult result = new BlendedResult(components, componentCounts, MARFAN_COUNTS, 0.75);

        components.add(GLUTARIC_ACIDURIA);
        componentCounts.add(GLUTARIC_ACIDURIA_COUNTS);

        assertFalse(result.isBlended(), "A result must not change when the caller's list changes");
        assertEquals(1, result.componentCounts().size());
    }
}
