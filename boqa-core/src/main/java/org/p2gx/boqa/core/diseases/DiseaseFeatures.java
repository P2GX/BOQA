package org.p2gx.boqa.core.diseases;

import java.util.HashSet;
import java.util.Set;

/**
 * Container for disease phenotype data (replaces I, E keys).
 *
 * @param id disease identifier (e.g., "OMIM:123456")
 * @param label human-readable disease name
 * @param observedPhenotypes set of HPO term IDs associated with this disease
 * @param excludedPhenotypes set of HPO term IDs explicitly not associated with this disease
 */
record DiseaseFeatures(String id, String label, Set<String> observedPhenotypes, Set<String> excludedPhenotypes) {

    static DiseaseFeatures of(String id, String label) {
        return new DiseaseFeatures(id, label, new HashSet<>(), new HashSet<>());
    }
}