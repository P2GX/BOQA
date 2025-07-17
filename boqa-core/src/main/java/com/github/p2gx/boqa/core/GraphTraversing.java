package com.github.p2gx.boqa.core;

import org.monarchinitiative.phenol.graph.OntologyGraph;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class GraphTraversing {

    private final OntologyGraph<TermId> hpoGraph;

    public OntologyGraph<TermId> getHpoGraph() {
        return hpoGraph;
    }
    public GraphTraversing(OntologyGraph<TermId> hpoGraph) {
        this.hpoGraph = hpoGraph;
    }

    /**
     * // Insert example below:
     * <pre>
     *     Set<TermId> queryWithAncsSet = BoqaSetCounter.initLayer(querySet);
     *
     * </pre>
     * @param hpoTerms
     * @return
     */
    Set<TermId> initLayer(Set<TermId> hpoTerms){
        Set<TermId> observedAncestors = new HashSet<>();
        hpoTerms.forEach(t -> observedAncestors.addAll(hpoGraph.extendWithAncestors(t, true)));
        // We only want phenotypic abnormalities!
        //List<TermId> observed = hpoTerms.stream().toList();
        //Set<TermId> observedAncestors = new HashSet<>();
        //for (TermId termId : observed) {
        //    observedAncestors.addAll( hpoGraph.extendWithAncestors(termId, true));
        //}

        // We only want phenotypic abnormalities!
        observedAncestors.remove(TermId.of("HP:0000001"));
        return observedAncestors;
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
        parents.addAll( hpoGraph.extendWithParents(node, false));
        parents.removeAll(activeNodes);
        return parents.isEmpty();
    }
}
