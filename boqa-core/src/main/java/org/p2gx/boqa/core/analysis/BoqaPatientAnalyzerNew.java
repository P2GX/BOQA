package org.p2gx.boqa.core.analysis;

import org.p2gx.boqa.core.Counter;
import org.p2gx.boqa.core.PatientData;
import org.p2gx.boqa.core.algorithm.AlgorithmParameters;
import org.p2gx.boqa.core.algorithm.BoqaCountsNew;
import org.p2gx.boqa.core.diseases.CandidateDiseaseNew;
import org.p2gx.boqa.core.diseases.TargetDisease;
import org.p2gx.boqa.core.diseases.DiseaseComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Performs BOQA analysis for a given query set of HPO terms (patient's data).
 * <p>
 * This class evaluates a single patient's phenotypic profile (HPO terms)
 * against all HPOA-annotated diseases and computes probability scores for
 * diagnostic ranking.
 */
public final class BoqaPatientAnalyzerNew {

    private static final Logger LOGGER = LoggerFactory.getLogger(BoqaPatientAnalyzerNew.class);

    /**
     * Computes unnormalized BOQA log scores (log(probabilities))
     * for each HPOA-annotated disease, given a query set of HPO terms (patient's
     * data).
     * This function is also intended to be used in the BoqaPrioritiser of Exomiser.
     *
     * <p>
     * For each HPOA-annotated disease, this method performs the following steps:
     * <ol>
     * <li>Compute {@link BoqaCountsNew} using the provided
     * {@link org.p2gx.boqa.core.algorithm.BoqaSetCounter}</li>
     * <li>Calculate un-normalized log probability using
     * {@link #computeUnnormalizedLogProbability(AlgorithmParameters, BoqaCountsNew)}</li>
     * </ol>
     *
     * @param patientData Query data (symptoms/features observed in a patient)
     * @param counter     The counter object that computes BoqaCounts for each
     *                    HPOA-annotated disease
     * @return A {@link BoqaAnalysisResult} containing the patient data along with
     *         counts and raw log scores for each HPOA-annotated disease.
     */
    public static  List<BoqaResultNew> computeBoqaResultsRawLog(
            PatientData patientData, Counter counter, List<CandidateDiseaseNew> diseaseCandidateList) {
        return computeBoqaResultsRawLog(patientData, counter, diseaseCandidateList, AlgorithmParameters.defaultParams());
    }

    public static List<BoqaResultNew> computeBoqaResultsRawLog(
            PatientData patientData, Counter counter, List<CandidateDiseaseNew> diseaseCandidateList,
            AlgorithmParameters params) {
        return diseaseCandidateList
                .parallelStream() // fast: computes counts + scores in parallel
                .map( dc-> {
                        BoqaCountsNew bc = counter.computeBoqaCountsFromDisease(
                                dc.observedHpoTermids(), patientData.getObservedTerms());
                        double rawScore = computeUnnormalizedLogProbability(params, bc);
                        return new BoqaResultNew(bc,rawScore, dc);
                    })
                .toList();
    }

    /**
     * To do, consider making API a little more convenient and reduced code
     * duplication with above
     * 
     * @param
     * @param
     * @return
     */
    public static List<BoqaResultNew> computeBoqaResultsRescaled(
            List<BoqaResultNew> unscaledResults) {
        return Util.reScaledRawLogBoqaExomiserScoresNew(unscaledResults);
    }

    /**
     * Computes normalized BOQA scores (probabilities) for each HPOA-annotated
     * disease,
     * given a query set of HPO terms (patient's data),
     * and sorts the diseases by score.
     *
     * <p>
     * This method performs the complete BOQA analysis pipeline:
     * <ol>
     * <li>Calculate un-normalized probabilities using
     * {@link #computeBoqaResultsRawLog(PatientData, Counter, List<CandidateDiseaseNew>, AlgorithmParameters)}</li>
     * <li>Normalize the probabilities so that they sum up to 1.0 across all
     * diseases</li>
     * <li>Sort results by score (descending) and limit to top results</li>
     * </ol>
     *
     * @param patientData  Query data (symptoms/features observed in a patient)
     * @param counter      The counter object that computes BoqaCounts for each
     *                     HPOA-annotated disease
     * @param resultsLimit Maximum number of top-scoring diseases to return
     * @return A {@link BoqaAnalysisResult} containing the patient data along with
     *         a list of {@link BoqaResult} sorted by score.
     *         <p>
     */
    public static List<CandidateResult> computeBoqaResults(
            PatientData patientData, 
            Counter counter, 
            int resultsLimit, 
            AlgorithmParameters params,
            List<CandidateDiseaseNew> diseaseCandidateList) {

        // Get BoqaResults (which also contain CandidateDisease now) with raw log scores
        List<BoqaResultNew> rawLogBoqaResults =
                computeBoqaResultsRawLog(
                        patientData, counter, diseaseCandidateList, params);

        // Sort by raw log score
        rawLogBoqaResults.sort(Comparator.comparingDouble(BoqaResultNew::boqaScore).reversed());

        // Find max log-prob
        double maxLogP = rawLogBoqaResults.stream()
                .mapToDouble(BoqaResultNew::boqaScore)
                .max()
                .orElse(Double.NEGATIVE_INFINITY);

        // Compute sum of exp(logP - maxLogP)
        double sum = rawLogBoqaResults.stream()
                .mapToDouble(r -> Math.exp(r.boqaScore() - maxLogP))
                .sum();

        // Normalize
        List<BoqaResultNew> allResults = new ArrayList<>();
        rawLogBoqaResults.forEach(r -> {
            double normProb = Math.exp(r.boqaScore() - maxLogP) / sum;
            allResults.add(new BoqaResultNew(r.counts(), normProb, r.candidate()));
        });

        // Use CandidateResult only now. Filter out most melded
        Map<String, BoqaResultNew> singleResultsById = allResults.stream()
                .filter(r -> r.candidate() instanceof CandidateDiseaseNew.SingleDiseaseNew)
                .collect(Collectors.toMap(
                        r -> ((CandidateDiseaseNew.SingleDiseaseNew) r.candidate())
                                .disease().diseaseId(),
                        Function.identity()
                ));
        List<CandidateResult> candidateResults = allResults.stream()
                .map(r -> toCandidateResult(r, singleResultsById))
                .toList();

        return candidateResults.stream()
                .filter(r ->
                        !(r instanceof CandidateResult.BlendedResult)
                                || r.improvedComparedToBestSingleDisease())
                .sorted()
                .limit(resultsLimit)
                .toList();
    }

    private static DiseaseComponent toDiseaseComponent(
            BoqaResultNew result,
            CandidateDiseaseNew.SingleDiseaseNew candidate) {

        return new DiseaseComponent(
                candidate.disease(),
                result.counts(),
                result.boqaScore()
        );
    }
    // TODO actually make sure BlendedResults have also counts and score of the blended disease
    private static CandidateResult toCandidateResult(
            BoqaResultNew result,
            Map<String, BoqaResultNew> singleResultsById) {

        return switch (result.candidate()) {
            case CandidateDiseaseNew.SingleDiseaseNew singleDiseaseNew ->
                    new CandidateResult.SingleResult(
                            toDiseaseComponent(result, singleDiseaseNew)
                    );
            case CandidateDiseaseNew.BlendedDiseaseNew blended -> {
                List<DiseaseComponent> components = blended.components().stream()
                        .map(TargetDisease.PhenotypeAndGene::diseaseId)
                        .map(singleResultsById::get)
                        .map(r -> {
                            CandidateDiseaseNew.SingleDiseaseNew singleDiseaseNew =
                                    (CandidateDiseaseNew.SingleDiseaseNew) r.candidate();
                            return toDiseaseComponent(r, singleDiseaseNew);
                        })
                        .toList();

                yield new CandidateResult.BlendedResult(
                        components,
                        result.boqaScore()
                );
            }
        };
    }

    /**
     * Computes the un-normalized BOQA log probability for given BoqaCounts and
     * parameters:
     * <p>
     * log(P) = fp × log(α) + fn × log(β) + tn × log(1-α) + tp × log(1-β)
     * </p>
     * 
     * @param params alpha, beta, log(alpha), log(beta) etc.
     * @param counts The {@link BoqaCountsNew} for a query and a disease.
     * @return The un-normalized BOQA log probability score.
     */
    static double computeUnnormalizedLogProbability(AlgorithmParameters params, BoqaCountsNew counts) {
        return counts.fpBoqaCount() * params.getLogAlpha() +
                counts.fnBoqaCount() * params.getLogBeta() +
                counts.tnBoqaCount() * params.getLogOneMinusAlpha() +
                counts.tpBoqaCount() * params.getLogOneMinusBeta();
    }

    /**
     * Computes the un-normalized BOQA probability P for a given set of BoqaCounts
     * and parameters
     * <p>
     * P = α<sup>fpBoqaCount</sup> × β<sup>fpBoqaCount</sup> ×
     * (1-α)<sup>fnBoqaCount</sup> × (1-β)<sup>tpBoqaCount</sup>
     * </pre>
     * 
     * @param alpha  False positive rate parameter.
     * @param beta   False negative rate parameter.
     * @param counts The {@link BoqaCountsNew} for a disease.
     * @return The un-normalized probability score.
     */
    static double computeUnnormalizedProbability(double alpha, double beta, BoqaCountsNew counts) {
        return Math.pow(alpha, counts.fpBoqaCount()) *
                Math.pow(beta, counts.fnBoqaCount()) *
                Math.pow(1 - alpha, counts.tnBoqaCount()) *
                Math.pow(1 - beta, counts.tpBoqaCount());
    }
}

