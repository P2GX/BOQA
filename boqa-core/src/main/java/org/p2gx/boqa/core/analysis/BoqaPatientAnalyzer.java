package org.p2gx.boqa.core.analysis;

import org.p2gx.boqa.core.Counter;
import org.p2gx.boqa.core.PatientData;
import org.p2gx.boqa.core.algorithm.AlgorithmParameters;
import org.p2gx.boqa.core.algorithm.BoqaCounts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

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
        List<BoqaResult> rawLogBoqaResults = new ArrayList<>(computeBoqaResultsRawLog(patientData, counter, params).boqaResults());;

        // Sort by raw log score
        Collections.sort(
                rawLogBoqaResults,
                Comparator.comparingDouble(BoqaResult::boqaScore).reversed()
        );

        // Find max log-prob
        double maxLogP = rawLogBoqaResults.stream()
                .mapToDouble(BoqaResult::boqaScore)
                .max()
                .orElse(Double.NEGATIVE_INFINITY);

        // Compute sum of exp(logP - maxLogP)
        double sum = rawLogBoqaResults.stream()
                .mapToDouble(r -> Math.exp(r.boqaScore() - maxLogP))
                .sum();

        // Normalize
        List<BoqaResult> allResults = new ArrayList<>();
        rawLogBoqaResults.forEach(r -> {
            double normProb = Math.exp(r.boqaScore() - maxLogP) / sum;
            allResults.add(new BoqaResult(r.counts(), normProb));
        });

        return new BoqaAnalysisResult(patientData, allResults.stream().limit(resultsLimit).toList());
    }

    /**
     * Computes the un-normalized BOQA log probability for given BoqaCounts and parameters:
     * <p>
     * log(P) = fp × log(α) + fn × log(β) + tn × log(1-α)  + tp × log(1-β)
     * </p>
     * @param params  alpha, beta, log(alpha), log(beta) etc.
     * @param counts The {@link BoqaCounts} for a query and a disease.
     * @return The un-normalized BOQA log probability score.
     */
    static double computeUnnormalizedLogProbability(AlgorithmParameters params, BoqaCounts counts){
        return counts.fpBoqaCount() * params.getLogAlpha() +
                counts.fnBoqaCount() * params.getLogBeta() +
                counts.tnBoqaCount() * params.getLogOneMinusAlpha() +
                counts.tpBoqaCount() * params.getLogOneMinusBeta();
    }

    /**
     * Computes the un-normalized BOQA probability P for a given set of BoqaCounts and parameters
     * <p>
     * P = α<sup>fpBoqaCount</sup> × β<sup>fpBoqaCount</sup> ×
     * (1-α)<sup>fnBoqaCount</sup> × (1-β)<sup>tpBoqaCount</sup>
     *  </pre>
     * @param alpha  False positive rate parameter.
     * @param beta   False negative rate parameter.
     * @param counts The {@link BoqaCounts} for a disease.
     * @return The un-normalized probability score.
     */
    @Deprecated
    static double computeUnnormalizedProbability(double alpha, double beta, BoqaCounts counts){
        return Math.pow(alpha, counts.fpBoqaCount())*
                Math.pow(beta, counts.fnBoqaCount())*
                Math.pow(1-alpha, counts.tnBoqaCount())*
                Math.pow(1-beta, counts.tpBoqaCount());
    }
}
