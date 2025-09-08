package com.github.p2gx.boqa.core.analysis;

import com.github.p2gx.boqa.core.algorithm.BoqaCounts;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static com.github.p2gx.boqa.core.analysis.AnalysisResults.computeUnnormalizedProbability;
import static org.junit.jupiter.api.Assertions.*;

class AnalysisResultsTest {

    /**
     * Parameterized test for {@link  AnalysisResults#computeUnnormalizedProbability(double, double, BoqaCounts)}.
     * <p>
     * This test validates that the Java implementation of the un-normalized probability calculation
     * produces identical results to a set of scores created in Python.
     * The reference scores were generated using the script
     * {@code scripts/generate_scores4testing.py}, which produces thousands of test cases
     * with varying values of {@code alpha}, {@code beta}, and random BoqaCounts values between 0 and 100.
     * </p>
     *
     * <p>
     * Each row in the CSV file {@code boqascores.csv} contains a value for {@code alpha}, {@code beta},
     * {@code count_a}, {@code count_b}, {@code count_1ma}, {@code count_1mb}, {@code score}
     * </p>
     *
     * @param alpha    probability of a false positive annotation (as string from CSV)
     * @param beta     probability of a false negative annotation (as string from CSV)
     * @param count_a  exponent of alpha, related to number of false positives (as string from CSV)
     * @param count_b  exponent of beta, related to number of false negatives (as string from CSV)
     * @param count_1ma exponent of 1-alpha, related to number of true negatives (as string from CSV)
     * @param count_1mb exponent of 1-beta, related to number of true positives (as string from CSV)
     * @param score    expected unnormalized probability (as string from CSV)
     */
    @ParameterizedTest(name = "[{index}] {arguments}")
    @CsvFileSource(resources = "boqascores.csv", numLinesToSkip = 1)
    void testComputeUnnormalizedProbability(
            String alpha,
            String beta,
            String count_a,
            String count_b,
            String count_1ma,
            String count_1mb,
            String score
    ){
        // Set to correct type
        double alphaVal = Double.parseDouble(alpha);
        double betaVal  = Double.parseDouble(beta);
        int countA      = Integer.parseInt(count_a);
        int countB      = Integer.parseInt(count_b);
        int count1ma    = Integer.parseInt(count_1ma);
        int count1mb    = Integer.parseInt(count_1mb);
        double expectedScore = Double.parseDouble(score);

        // Initialize BoqaCounts
        BoqaCounts counts = new BoqaCounts("idIsUnimportant", "labelIsUnimportant", count1mb, countA, count1ma, countB);
        double actualScore = computeUnnormalizedProbability(alphaVal, betaVal, counts);

        // Assert with small delta for floating-point comparison
        assertEquals(expectedScore, actualScore, 1e-9);
    }

}