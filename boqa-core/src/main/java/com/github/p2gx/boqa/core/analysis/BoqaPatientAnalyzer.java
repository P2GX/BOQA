package com.github.p2gx.boqa.core.analysis;

import com.github.p2gx.boqa.core.Counter;
import com.github.p2gx.boqa.core.PatientData;
import com.github.p2gx.boqa.core.algorithm.AlgorithmParameters;
import com.github.p2gx.boqa.core.algorithm.BoqaCounts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Performs BOQA Analysis for individual patients.
 * <p>This class analyzes a single patient's phenotypic profile against all known
 * diseases to compute probability scores for diagnostic ranking.
 */
public final class BoqaPatientAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BoqaPatientAnalyzer.class);

    /**
     * Computes normalized BOQA scores (probabilities) for all diseases given a patient's data.
     *
     * <p>This method performs the complete BOQA analysis pipeline:
     * <ol>
     *   <li>Computes {@link BoqaCounts} for each disease using the provided
     *   {@link com.github.p2gx.boqa.core.algorithm.BoqaSetCounter}</li>
     *   <li>Calculates un-normalized probabilities using
     *   {@link #computeUnnormalizedProbability(double, double, BoqaCounts)}</li>
     *   <li>Normalizes probabilities so they sum to 1.0 across all diseases</li>
     *   <li>Sorts results by score (highest first) and limits to top results</li>
     * </ol>
     *
     * @param patientData  The patient's phenotypic data (observed symptoms/features)
     * @param counter      The counter object that computes BoqaCounts for diseases
     * @param resultsLimit Maximum number of top-scoring diseases to return
     * @return An {@link BoqaAnalysisResult} containing the patient data and sorted disease scores
     * <p>
     * TODO consider using again pyboqa scores results, but this is trivial at this point
     */
    public static BoqaAnalysisResult computeBoqaResults(PatientData patientData, Counter counter, int resultsLimit) {
        List<BoqaCounts> countsList = counter.getDiseaseIds()
                .parallelStream() // much faster!
                .map(dId ->  counter.computeBoqaCounts(
                        dId,
                        patientData
                ))
                .toList();

        // Compute normalized probabilities and populate results with BoqaResults
        Map<String, Double> rawScores = countsList.stream()
                .collect(Collectors.toMap(
                        BoqaCounts::diseaseId,
                        bc -> computeUnnormalizedProbability(AlgorithmParameters.ALPHA, AlgorithmParameters.BETA, bc)
                ));
        double sum = rawScores.values().stream().mapToDouble(Double::doubleValue).sum();
        List<BoqaResult> allResults = new ArrayList<>();
        countsList.forEach(bc-> {
            double normalizedScore = rawScores.get(bc.diseaseId()) / sum;
            allResults.add(new BoqaResult(bc, normalizedScore));
        });

        Collections.sort(allResults);
        return new BoqaAnalysisResult(patientData, allResults.stream().limit(resultsLimit).toList());
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
    static double computeUnnormalizedProbability(double alpha, double beta, BoqaCounts counts){
        return Math.pow(alpha, counts.fpBoqaCount())*
                Math.pow(beta, counts.fnBoqaCount())*
                Math.pow(1-alpha, counts.tnBoqaCount())*
                Math.pow(1-beta, counts.tpBoqaCount());
    }


}
