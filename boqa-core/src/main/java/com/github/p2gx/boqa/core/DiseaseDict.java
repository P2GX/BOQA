package com.github.p2gx.boqa.core;

import java.util.Set;

public interface DiseaseDict {

    // Return number of diseases in dictionary
    int size();

    // Return features associated with a disease
    Set<String> getIncludedDiseaseFeatures(String omimId);

    // Return features that are explicitly not associated with a disease
    Set<String> getExcludedDiseaseFeatures(String omimId);

    Set<String> getDiseaseGeneIds(String omimId);

    Set<String> getDiseaseGeneSymbols(String omimId);
}