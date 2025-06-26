package com.github.p2gx.boqa.core;

/**
 * Minimal interface for analysis so that different approaches can be implemented for this interface.
 */
public interface Analysis {

    void run();

    AnalysisResults getResults();
}
