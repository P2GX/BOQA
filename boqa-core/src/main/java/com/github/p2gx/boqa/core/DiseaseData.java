package com.github.p2gx.boqa.core;

import java.util.Set;

/**
 * Interface that provides disease annotations from HPOA.
 * <p>
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 */
public interface DiseaseData {

    // Return number of diseases in dictionary
    int size();

    // Return HPO terms associated with a disease
    Set<String> getIncludedDiseaseFeatures(String diseaseId);

    // Return HPO terms that are explicitly not associated with a disease
    Set<String> getExcludedDiseaseFeatures(String diseaseId);

    // Return NCBI gene IDs associated with a disease
    Set<String> getDiseaseGeneIds(String diseaseId);

    // Return gene symbols associated with a disease
    Set<String> getDiseaseGeneSymbols(String diseaseId);

    // Filter dictionary for diseases associated with given genes

    // Create blended diseases by combining the phenotypic features for all pairs of diseases
}