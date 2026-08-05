package org.p2gx.boqa.core;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.p2gx.boqa.core.patient.SingleDiseaseInfo;

import java.util.Set;

/**
 * Interface that provides disease annotations from various sources such as HPOA.
 * Different implementations of this interface ingest annotations from different sources.
 * <p>
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 */
public interface DiagnosisData {
    // Return number of diseases in dictionary
    int size();

    // Get set of all disease IDs
    Set<Set<SingleDiseaseInfo>> getDiagnosisIds();

    // Return HPO terms associated with a disease
    //Set<String> getObservedDiseaseFeatures(DiseaseDTO diseaseId);

    // Return HPO terms that are explicitly not associated with a disease
    //default Set<String> getExcludedDiseaseFeatures(DiseaseDTO diseaseId) {
        // If excluded terms are not used or not available
    //    return Set.of();
    //}

    Set<CandidateDiagnosis> getCandidateDiagnosisSet();

    default HpoDiseases getDiseases() {
        return null;
    }

}