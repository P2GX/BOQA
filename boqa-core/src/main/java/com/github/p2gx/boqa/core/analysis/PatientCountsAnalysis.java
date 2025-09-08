package com.github.p2gx.boqa.core.analysis;

import com.github.p2gx.boqa.core.Analysis;
import com.github.p2gx.boqa.core.Counter;
import com.github.p2gx.boqa.core.PatientData;
import com.github.p2gx.boqa.core.algorithm.BoqaCounts;
import com.github.p2gx.boqa.core.algorithm.BoqaSetCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * An implementation of the {@link Analysis} interface for a single patient.
 * This class uses a shared {@link Counter} object to compute {@link BoqaCounts}
 * for all diseases and stores the result in an {@link AnalysisResults} instance.
 * <p>
 * Each instance corresponds to one patient. The analysis generates
 * disease-wise {@code BoqaCounts}, through which are diseases probabilities
 * are computed at a later step.
 */
public class PatientCountsAnalysis implements Analysis {

    private static final Logger LOGGER = LoggerFactory.getLogger(PatientCountsAnalysis.class);
    private final Counter counter;
    private final AnalysisResults results;

    public PatientCountsAnalysis(PatientData patientData, Counter counter, int resultsLimit) {
        this.results = new AnalysisResults(patientData, resultsLimit);
        this.counter = counter;
    }

    @Override
    public void run() {
        // Compute BoqaCounts for all diseases
        List<BoqaCounts> countsList = counter.getDiseaseIds()
                .parallelStream() // much faster!
                .map(dId ->  counter.computeBoqaCounts(
                        dId,
                        results.getPatientData()
                ))
                .toList();

        // Compute normalized probabilities and populate results with BoqaResults
        results.computeBoqaListResults(countsList);
    }

    @Override
    public AnalysisResults getResults() {
       return this.results;
    }
}
