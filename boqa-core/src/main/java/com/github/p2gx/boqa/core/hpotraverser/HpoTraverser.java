package com.github.p2gx.boqa.core.hpotraverser;

import java.io.File;
import java.nio.file.Path;
import java.phenol;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;

public class HpoTraverser implements Traverser{

    private final Ontology hpoOntology;
    public HpoTraverser(Path hpoPath) {
        File hpoFile = hpoPath.toFile();
        this.hpoOntology = OntologyLoader.loadOntology(hpoFile);
    }

    @Override
    public Set<TermId> getObservedAncestors(Set<TermId> hpos) {
        List<TermId> observed = hpos.stream().toList();
        Set<TermId> observedAncestors = new HashSet<>();
        for (TermId termId : observed) {
            observedAncestors.addAll( hpoOntology.graph().extendWithAncestors(termId, true));
        }
        return observedAncestors;
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
