package com.github.p2gx.boqa.core.analysis;

import com.github.p2gx.boqa.core.algorithm.BoqaCounts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoqaResultTest {
    private static BoqaCounts bc;

    @BeforeAll
    static void initAll() {
        bc = new BoqaCounts("OMIM:123456", "disease", 1, 2, 3, 4);
    }
    @Test
    void testNanHandling() {
        BoqaResult valid = new BoqaResult(bc, 0.5);
        BoqaResult nan = new BoqaResult(bc, Double.NaN);

        List<BoqaResult> results = new ArrayList<>(List.of(nan, valid));
        Collections.sort(results);
        assertEquals(valid, results.get(0));
        assertEquals(nan, results.get(1));
   }

    @Test
    void testOrderingDoubles() {
        BoqaResult hiScore = new BoqaResult(bc, 1.0);
        BoqaResult loScore = new BoqaResult(bc, 0.1);
        List<BoqaResult> allResults = new ArrayList<>(List.of(loScore, hiScore));
        Collections.sort(allResults);
        assertEquals(hiScore, allResults.get(0));
        assertTrue(allResults.getFirst().boqaScore()>allResults.get(1).boqaScore());
    }
}