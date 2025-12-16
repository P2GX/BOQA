package org.p2gx.boqa.core.analysis;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.p2gx.boqa.core.Counter;
import org.p2gx.boqa.core.DiseaseData;
import org.p2gx.boqa.core.PatientData;
import org.p2gx.boqa.core.TestBase;
import org.p2gx.boqa.core.algorithm.AlgorithmParameters;
import org.p2gx.boqa.core.algorithm.BoqaCounts;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.p2gx.boqa.core.algorithm.BoqaSetCounter;
import org.p2gx.boqa.core.diseases.DiseaseDataPhenolIngest;
import org.p2gx.boqa.core.patient.QueryDataFromString;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.p2gx.boqa.core.analysis.BoqaPatientAnalyzer.*;

class BoqaPatientAnalyzerTest extends TestBase {

    private static Counter counter;

    @BeforeAll
    static void setup() throws IOException {
        DiseaseData diseaseData = DiseaseDataPhenolIngest.of(hpo(), hpoDiseases());
        counter = new BoqaSetCounter(diseaseData, hpo());
    }

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
        double actualScore = computeUnnormalizedProbability(alpha, beta, 1.0, counts);

        // Assert with small delta for floating-point comparison
        assertEquals(expectedScore, actualScore, 1e-9);
    }

    /**
     * Test for {@link  BoqaPatientAnalyzer#computeBoqaResults(PatientData, Counter, int, AlgorithmParameters)}.
     * <p>
     * This test validates that the calculation of the normalized BOQA probabilities works as expected.
     * </p>
     *
     * <p>
     * <ol>
     *   <li>First, the BoqaResults for all annotated diseases are calculated for
     *   some valid input query HPO terms and parameters using {@link  BoqaPatientAnalyzer#computeBoqaResults(PatientData, Counter, int, AlgorithmParameters)}.
     *   </li>
     *   <li>Each BoqaResult contains both the normalized score and the underlying BOQA counts.
     *   The counts are used to calculate the normalized probabilities in the conventional way,i.e.,
     *   without the shift-log-exp trick used in computeBoqaResults.</li>
     *   <li>These probabilities are then compared with those from BoqaResults.</li>
     * </ol>
     * </p>
     */
    @Test
    void testComputeBoqaResults(){

        // Prepare arguments for 'computeBoqaResults'
        PatientData patientData = new QueryDataFromString("HP:0000478,HP:0000598", "");
        int limit = counter.getDiseaseIds().size();
        double alpha = 0.01;
        double beta = 0.9;
        AlgorithmParameters params = AlgorithmParameters.create(alpha, beta, 1.0);

        // Run 'computeBoqaResults'
        BoqaAnalysisResult boqaAnalysisResult = BoqaPatientAnalyzer.computeBoqaResults(
                patientData, counter, limit, params);

        // Recompute un-normalized probabilities in the conventional way
        List<Double> rawProbs = boqaAnalysisResult.boqaResults().stream()
                .map(result -> computeUnnormalizedProbability(params.getAlpha(), params.getBeta(),  params.getTemperature(), result.counts()))
                .toList();

        // Get the sum for normalization
        double rawProbsSum = rawProbs.stream().mapToDouble(Double::doubleValue).sum();

        // Compare normalized probabilities from BoqaResults with those recalculated from counts
        boqaAnalysisResult.boqaResults().forEach(br-> {
            double expectedNormProb = computeUnnormalizedProbability(params.getAlpha(), params.getBeta(), params.getTemperature(), br.counts()) / rawProbsSum;
            double actualNormProb = br.boqaScore();
            assertEquals(expectedNormProb, actualNormProb, 1e-9);
        });
    }
}