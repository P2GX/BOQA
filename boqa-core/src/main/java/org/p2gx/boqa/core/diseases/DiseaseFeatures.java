package org.p2gx.boqa.core.diseases;

import org.p2gx.boqa.core.Gene;
import java.util.HashSet;
import java.util.Set;

/**
 * Container for disease phenotype and gene association data (replaces I, E, G, GS keys).
 *
 * @param id disease identifier (e.g., "OMIM:123456")
 * @param label human-readable disease name
 * @param observedPhenotypes set of HPO term IDs associated with this disease
 * @param excludedPhenotypes set of HPO term IDs explicitly not associated with this disease
 * @param geneIds set of NCBI gene IDs associated with this disease
 * @param geneSymbols set of gene symbols associated with this disease
 */
record DiseaseFeatures(String id, String label, Set<String> observedPhenotypes, Set<String> excludedPhenotypes,
                       Set<String> geneIds, Set<String> geneSymbols) {

    static DiseaseFeatures of(String id, String label) {
        return new DiseaseFeatures(id, label, new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());
    }
}

/**
 * Container for disease-gene phenotype associations.
 *
 * @param id disease identifier
 * @param label human-readable disease name
 * @param gene associated gene
 * @param observedPhenotypes set of HPO term IDs observed in this disease-gene context
 * @param excludedPhenotypes set of HPO term IDs excluded in this disease-gene context
 */
record DiseaseGeneFeatures(String id, String label, Gene gene, Set<String> observedPhenotypes, Set<String> excludedPhenotypes) {}