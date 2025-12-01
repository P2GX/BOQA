package org.p2gx.boqa.core.analysis;

import org.p2gx.boqa.core.algorithm.AlgorithmParameters;
import org.p2gx.boqa.core.algorithm.BoqaCounts;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.p2gx.boqa.core.analysis.BoqaPatientAnalyzer.computeUnnormalizedLogProbability;
import static org.p2gx.boqa.core.analysis.BoqaPatientAnalyzer.computeUnnormalizedProbability;
import static org.junit.jupiter.api.Assertions.*;

class BoqaPatientAnalyzerTest {

    /**
     * Parameterized test for {@link  BoqaPatientAnalyzer#computeUnnormalizedProbability(double, double, BoqaCounts)}.
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
     * @param countA  exponent of alpha, related to number of false positives (as string from CSV)
     * @param countB  exponent of beta, related to number of false negatives (as string from CSV)
     * @param count1ma exponent of 1-alpha, related to number of true negatives (as string from CSV)
     * @param count1mb exponent of 1-beta, related to number of true positives (as string from CSV)
     * @param expectedScore    expected unnormalized probability (as string from CSV)
     */
    @ParameterizedTest(name = "[{index}] {arguments}")
    @CsvFileSource(resources = "boqascores.csv", numLinesToSkip = 1)
    void testComputeUnnormalizedProbability(
            double alpha,
            double beta,
            int countA,
            int countB,
            int count1ma,
            int count1mb,
            double expectedScore
    ){
        // Initialize BoqaCounts
        BoqaCounts counts = new BoqaCounts("idIsUnimportant", "labelIsUnimportant", count1mb, countA, count1ma, countB);
        double actualScore = computeUnnormalizedProbability(alpha, beta, counts);

        // Assert with small delta for floating-point comparison
        assertEquals(expectedScore, actualScore, 1e-9);
    }


    /**
     * Parameterized test for {@link  BoqaPatientAnalyzer#computeUnnormalizedLogProbability(AlgorithmParameters, BoqaCounts)}.
     * <p>
     * This test validates that the Java implementation of the un-normalized log probability calculation
     * works as expected.
     * </p>
     *
     * <p>
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
     * @param countA  exponent of alpha, related to number of false positives (as string from CSV)
     * @param countB  exponent of beta, related to number of false negatives (as string from CSV)
     * @param count1ma exponent of 1-alpha, related to number of true negatives (as string from CSV)
     * @param count1mb exponent of 1-beta, related to number of true positives (as string from CSV)
     * @param expectedScore    expected unnormalized probability (as string from CSV)
     */
    @ParameterizedTest(name = "[{index}] {arguments}")
    @CsvFileSource(resources = "boqascores.csv", numLinesToSkip = 1)
    void testComputeUnnormalizedLogProbability(
            double alpha,
            double beta,
            int countA,
            int countB,
            int count1ma,
            int count1mb,
            double expectedScore
    ){
        // Initialize BoqaCounts
        BoqaCounts counts = new BoqaCounts("idIsUnimportant", "labelIsUnimportant", count1mb, countA, count1ma, countB);

        // Compute un-normalized log probability
        AlgorithmParameters params = AlgorithmParameters.create(alpha, beta);
        computeUnnormalizedLogProbability(params, counts);
        double actualScore = Math.exp(computeUnnormalizedLogProbability(params, counts));

        // Assert with small delta for floating-point comparison
        assertEquals(expectedScore, actualScore, 1e-9);
    }
}