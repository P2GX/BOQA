package com.github.p2gx.boqa.core;

import java.util.Set;

public interface DiseaseDict {

    // Return features associated with a disease
    Set<String> getIncludedDiseaseFeatures(String omimId);

    // Return features that are explicitly not associated with a disease
    Set<String> getExcludedDiseaseFeatures(String omimId);
}