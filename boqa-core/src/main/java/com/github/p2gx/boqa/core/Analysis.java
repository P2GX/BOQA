package com.github.p2gx.boqa.core;

/**
 * Initializes one query layer (one patient) and orchestrates the computation of the score for each disease.
 * Should allow for parallel execution of diseases.
 */
public interface Analysis {

    void run();

    AnalysisResults getResults();
}
