package com.github.p2gx.boqa.core.hpotraverser;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.Ontology;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public interface Traverser {
    // Input should be a set of HPO terms, i.e. Set<TermId>
    // Currently output of DiseasesDict is a Set<String> named terIdList
    // diseaseDict.getIncludedDiseaseFeatures("OMIM:604091");
    // IMHO the class has to ignore whether this is a list of HPO terms coming from a phenopacket or a HPOA file
    // In PR's example it does not

    Set<TermId> getObservedAncestors(Set<TermId> hpos);
    Set<TermId> getObservedParents(Set<TermId> hpos);
    Set<TermId> getChildren(Set<TermId> hpos);
    Set<TermId> getSiblings(Set<TermId> hpos);
}
