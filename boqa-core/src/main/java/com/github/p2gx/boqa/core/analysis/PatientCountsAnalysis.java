package com.github.p2gx.boqa.core.analysis;

import com.github.p2gx.boqa.core.Analysis;
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
 * An implementation of the {@link Analysis} interface for a single patient.
 * This class uses a shared {@link Counter} object to compute {@link BoqaCounts}
 * for all diseases and stores the result in an {@link AnalysisResults} instance.
 * <p>
 * Each instance corresponds to one patient. The analysis generates
 * disease-wise {@code BoqaCounts}, through which are diseases probabilities
 * are computed at a later step.
 */
public class PatientCountsAnalysis {

    private static final Logger LOGGER = LoggerFactory.getLogger(PatientCountsAnalysis.class);
    /**
     * Extra record with wrapping around {@link BoqaCounts} and adding the score.
     *
     * <p> Useful to ease development phase without changing future code.
     *
     * @param counts    The BOQA counts for a disease.
     * @param boqaScore The normalized BOQA score for that disease.
     *
     * <p>
     * TODO add check/handling for boqaScore = NaN
     */
    public record BoqaResult(BoqaCounts counts, Double boqaScore) implements Comparable<BoqaResult>{
        @Override
        public int compareTo(BoqaResult other) {
            return other.boqaScore.compareTo(this.boqaScore);
        }
    }

    public static List<BoqaResult> computeBoqaResults(PatientData patientData, Counter counter, int resultsLimit) {
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
        return allResults.stream()
                .limit(resultsLimit)
                .toList();
    }

    /**
     * Computes the un-normalized BOQA probability for a given set of BoqaCounts and parameters.
     *
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
