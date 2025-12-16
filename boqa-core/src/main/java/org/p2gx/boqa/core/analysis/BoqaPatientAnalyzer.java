package org.p2gx.boqa.core.analysis;

import org.p2gx.boqa.core.Counter;
import org.p2gx.boqa.core.PatientData;
import org.p2gx.boqa.core.algorithm.AlgorithmParameters;
import org.p2gx.boqa.core.algorithm.BoqaCounts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

import static org.junit.Assert.assertThat;

/**
 * Performs BOQA analysis for a given query set of HPO terms (patient's data).
 * <p>
 * This class evaluates a single patient's phenotypic profile (HPO terms)
 * against all HPOA-annotated diseases and computes probability scores for diagnostic ranking.
 */
public final class BoqaPatientAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BoqaPatientAnalyzer.class);

    /**
     * Computes unnormalized BOQA log scores (log(probabilities))
     * for each HPOA-annotated disease, given a query set of HPO terms (patient's data).
     * This function is also intended to be used in the BoqaPrioritiser of Exomiser.
     *
     * <p>For each HPOA-annotated disease, this method performs the following steps:
     * <ol>
     *   <li>Compute {@link BoqaCounts} using the provided
     *   {@link org.p2gx.boqa.core.algorithm.BoqaSetCounter}</li>
     *   <li>Calculate un-normalized log probability using
     *   {@link #computeUnnormalizedLogProbability(AlgorithmParameters, BoqaCounts)}</li>
     * </ol>
     *
     * @param patientData  Query data (symptoms/features observed in a patient)
     * @param counter      The counter object that computes BoqaCounts for each HPOA-annotated disease
     * @return A {@link BoqaAnalysisResult} containing the patient data along with
     * counts and raw log scores for each HPOA-annotated disease.
     */
    public static BoqaAnalysisResult computeBoqaResultsRawLog(
            PatientData patientData, Counter counter, AlgorithmParameters params) {

        List<BoqaResult> allResults = counter.getDiseaseIds()
                .parallelStream() // fast: computes counts + scores in parallel
                .map(dId -> {
                    BoqaCounts bc = counter.computeBoqaCounts(dId, patientData);
                    double rawScore = computeUnnormalizedLogProbability(params, bc);
                    return new BoqaResult(bc, rawScore);
                })
                .toList();

        return new BoqaAnalysisResult(patientData, allResults);
    }

    /**
     * Computes normalized BOQA scores (probabilities) for each HPOA-annotated disease,
     * given a query set of HPO terms (patient's data),
     * and sorts the diseases by score.
     *
     * <p>This method performs the complete BOQA analysis pipeline:
     * <ol>
     *   <li>Calculate un-normalized probabilities using
     *   {@link #computeBoqaResultsRawLog(PatientData, Counter, AlgorithmParameters)}</li>
     *   <li>Normalize the probabilities so that they sum up to 1.0 across all diseases</li>
     *   <li>Sort results by score (descending) and limit to top results</li>
     * </ol>
     *
     * @param patientData  Query data (symptoms/features observed in a patient)
     * @param counter      The counter object that computes BoqaCounts for each HPOA-annotated disease
     * @param resultsLimit Maximum number of top-scoring diseases to return
     * @return A {@link BoqaAnalysisResult} containing the patient data along with
     * a list of {@link BoqaResult} sorted by score.
     * <p>
     */
    public static BoqaAnalysisResult computeBoqaResults(
            PatientData patientData, Counter counter, int resultsLimit, AlgorithmParameters params) {


        // Get BoqaResults with raw log scores
        List<BoqaResult> rawLogBoqaResults = new ArrayList<>(computeBoqaResultsRawLog(patientData, counter, params).boqaResults());

        // Sort by raw log score
        rawLogBoqaResults.sort(Comparator.comparingDouble(BoqaResult::boqaScore).reversed());

        // Find max log-prob
        double maxLogP = rawLogBoqaResults.stream()
                .mapToDouble(BoqaResult::boqaScore)
                .max()
                .orElse(Double.NEGATIVE_INFINITY);

        double normalizationFactor;
        double epsilon = 0.000001d;

        // Standard BOQA
        if((Math.abs(1.0 - params.getTemperature()) < epsilon)){
            // Compute sum of exp(logP - maxLogP)
            normalizationFactor = rawLogBoqaResults.stream()
                    .mapToDouble(r -> Math.exp(r.boqaScore() - maxLogP))
                    .sum();
        } else {
            // Non-unit temperature normalizes trivially by largest score, which is already present below
            normalizationFactor = 1.0;
        }

        // Normalize
        List<BoqaResult> allResults = new ArrayList<>();
        rawLogBoqaResults.forEach(r -> {
            double normProb = Math.exp(r.boqaScore() - maxLogP) / normalizationFactor;
            allResults.add(new BoqaResult(r.counts(), normProb));
        });

        return new BoqaAnalysisResult(patientData, allResults.stream().limit(resultsLimit).toList());
    }

    /**
     * Computes the un-normalized BOQA log probability for given BoqaCounts and parameters:
     * <p>
     * log(P) = [fp × log(α) + fn × log(β) + tn × log(1-α)  + tp × log(1-β)] / T
     * </p>
     * <p>
     * P = α<sup>fpBoqaCount</sup> × β<sup>fpBoqaCount</sup> ×
     * (1-α)<sup>fnBoqaCount</sup> × (1-β)<sup>tpBoqaCount</sup>
     *  </pre>
     * @param params  alpha, beta, log(alpha), log(beta) etc.
     * @param counts The {@link BoqaCounts} for a query and a disease.
     * @return The un-normalized BOQA log probability score.
     */
    static double computeUnnormalizedLogProbability(AlgorithmParameters params, BoqaCounts counts){
        return (counts.fpBoqaCount() * params.getLogAlpha() +
                counts.fnBoqaCount() * params.getLogBeta() +
                counts.tnBoqaCount() * params.getLogOneMinusAlpha() +
                counts.tpBoqaCount() * params.getLogOneMinusBeta()
        )/params.getTemperature();
    }

    /**
     * Computes the un-normalized BOQA probability P for a given set of BoqaCounts and parameters
     * <p>
     * P = α<sup>fpBoqaCount</sup> × β<sup>fpBoqaCount</sup> ×
     * (1-α)<sup>fnBoqaCount</sup> × (1-β)<sup>tpBoqaCount</sup>
     *  </pre>
     * @param alpha  False positive rate parameter.
     * @param beta   False negative rate parameter.
     * @param temperature   Use to make distributions more robust.
     * @param counts The {@link BoqaCounts} for a disease.
     * @return The un-normalized probability score.
     */
    static double computeUnnormalizedProbability(double alpha, double beta, double temperature, BoqaCounts counts){
        return Math.exp(
                (
                        counts.fpBoqaCount()*Math.log(alpha) +
                                counts.fnBoqaCount()*Math.log(beta) +
                                counts.tnBoqaCount()*Math.log(1-alpha) +
                                counts.tpBoqaCount()*Math.log(1-beta)
                ) / temperature
        );
    }
}
