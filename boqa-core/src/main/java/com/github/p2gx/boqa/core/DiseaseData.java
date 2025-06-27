package com.github.p2gx.boqa.core;

import java.util.Set;

/**
 * Interface that provides disease annotations from various sources such as HPOA.
 * Different implementations of this interface ingest annotations from different sources.
 * <p>
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 */
public interface DiseaseData {

    // Return number of diseases in dictionary
    int size();

    // Get set of all disease IDs
    Set<String> getDiseaseIds();

    // Return HPO terms associated with a disease
    Set<String> getIncludedDiseaseFeatures(String diseaseId);

    // Return HPO terms that are explicitly not associated with a disease
    default Set<String> getExcludedDiseaseFeatures(String diseaseId) {
        // If excluded terms are not used or not available
        return Set.of();
    }

    // Return NCBI gene IDs associated with a disease
    Set<String> getDiseaseGeneIds(String diseaseId);

    // Return gene symbols associated with a disease
    Set<String> getDiseaseGeneSymbols(String diseaseId);
}