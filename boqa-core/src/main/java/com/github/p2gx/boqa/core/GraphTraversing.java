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

    public Set<TermId> initLayer(Set<TermId> hpoTerms){
        List<TermId> observed = hpoTerms.stream().toList();
        Set<TermId> observedAncestors = new HashSet<>();
        for (TermId termId : observed) {
            observedAncestors.addAll( hpoGraph.extendWithAncestors(termId, true));
        }
        // We only want phenotypic abnormalities!
        observedAncestors.remove(TermId.of("HP:0000001"));
        return observedAncestors;
    }

    // TODO are these two methods trivial and should be removed? 1/2
    public Collection extendWithParents(TermId termId, boolean includeSource){
        return hpoGraph.extendWithParents(termId, includeSource );
    }

    // TODO are these two methods trivial and should be removed? 2/2
    public Collection extendWithChildren(TermId termId, boolean includeSource){
        return hpoGraph.extendWithChildren(termId, includeSource );
    }

    /**
     * Computes the parents of a node and confronts it with a Set of active nodes.
     * If all parents are in the Set of active nodes, the method returns true.
     *
     * @param node The width of the rectangle.
     * @param activeNodes The height of the rectangle.
     * @return true if all parents are active, false otherwise.
     */
    public boolean allParentsActive(TermId node, Set<TermId> activeNodes){
        Set<TermId> parents = new HashSet<>();
        parents.addAll( extendWithParents(node, false));
        parents.removeAll(activeNodes);
        return parents.isEmpty();
    }
}
