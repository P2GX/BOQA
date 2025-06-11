package com.github.p2gx.boqa.core.hpotraverser;

import java.util.Set;

import org.monarchinitiative.phenol.ontology.data.TermId;

public class HpoTraverser implements Traverser{

    @Override
    public Set<TermId> getObservedAncestors(Set<TermId> hpos) {
        return Set.of();
    }

    @Override
    public Set<TermId> getObservedParents(Set<TermId> hpos) {
        return Set.of();
    }

    @Override
    public Set<TermId> getChildren(Set<TermId> hpos) {
        return Set.of();
    }

    @Override
    public Set<TermId> getSiblings(Set<TermId> hpos) {
        return Set.of();
    }
}
