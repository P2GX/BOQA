package org.p2gx.boqa.core.diseases;

import org.p2gx.boqa.core.Gene;

import java.util.HashSet;
import java.util.Set;

// E, I , G, GS
record DiseaseFeatures(String id, String label, Set<String> observedPhenotypes, Set<String> excludedPhenotypes,
                       Set<String> geneIds, Set<String> geneSymbols) {

    static DiseaseFeatures of(String id, String label) {
        return new DiseaseFeatures(id, label, new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());
    }
}

record DiseaseGeneFeatures(String id, String label, Gene gene, Set<String> observedPhenotypes, Set<String> excludedPhenotypes) {}