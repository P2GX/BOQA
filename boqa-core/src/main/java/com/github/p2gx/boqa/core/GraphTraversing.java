package com.github.p2gx.boqa.core;

import org.monarchinitiative.phenol.graph.OntologyGraph;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GraphTraversing {

    private final OntologyGraph<TermId> hpoGraph;

    public GraphTraversing(OntologyGraph<TermId> hpoGraph) {
        this.hpoGraph = hpoGraph;
    }

    Set<TermId> initLayer(Set<TermId> hpoTerms){
        List<TermId> observed = hpoTerms.stream().toList();
        Set<TermId> observedAncestors = new HashSet<>();
        for (TermId termId : observed) {
            observedAncestors.addAll( hpoGraph.extendWithAncestors(termId, true));
        }
        return observedAncestors;
    }

    public Collection extendWithAncestors(TermId termId, boolean includeSource){
        return hpoGraph.extendWithAncestors(termId, includeSource );
    }


    public Collection extendWithParents(TermId termId, boolean includeSource){
        return hpoGraph.extendWithParents(termId, includeSource );
    }


    public Collection extendWithChildren(TermId termId, boolean includeSource){
        return hpoGraph.extendWithChildren(termId, includeSource );
    }

    public boolean allParentsActive(TermId node, Set<TermId> initializedLayer){
        // TODO this supposes we are in the query layer. Generalize?
        Set<TermId> parents = new HashSet<>();
        parents.addAll( extendWithParents(node, false));
        // increase counter iff (parents \ queryLayerInitialized) is the empty set
        parents.removeAll(initializedLayer);
        return parents.isEmpty();
    }
}
