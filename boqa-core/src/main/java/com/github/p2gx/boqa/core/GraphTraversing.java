package com.github.p2gx.boqa.core;

import org.monarchinitiative.phenol.graph.OntologyGraph;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This utility class holds the ontology graph and methods acting on it to collect necessary sets of terms.
 * <p>
 * @author <a href="mailto:leonardo.chimirri@bih-charite.de">Leonardo Chimirri</a>
 */
class GraphTraversing {

    public static final String HPO_ROOT_TERM = "HP:0000001";
    private final OntologyGraph<TermId> hpoGraph;
    private final boolean fullOntology;

    public OntologyGraph<TermId> getHpoGraph() {
        return hpoGraph;
    }
    public GraphTraversing(OntologyGraph<TermId> hpoGraph, boolean fullOntology) {
        this.hpoGraph = hpoGraph;
        this.fullOntology = fullOntology;
    }

    /**
     * Given a set of HPO terms, computes and returns the set of all implied terms, included those passed as input.
     * In BOQA language, the layer (query layer for patients and hidden layer for diseases) is
     * <i>initialized</i>.
     *
     * <pre>
     *      Set &ltTermId&gt someLayerInitialized = graphTraverser.initLayer(observedHpos);
     * </pre>
     * @param hpoTerms
     * @return initializedLayer
     */
    Set<TermId> initLayer(Set<TermId> hpoTerms){
        Set<TermId> initializedLayer = new HashSet<>();
        hpoTerms.forEach(t -> initializedLayer.addAll(hpoGraph.extendWithAncestors(t, true)));
        if(!fullOntology) {
            // We only want phenotypic abnormalities!
            initializedLayer.remove(TermId.of(HPO_ROOT_TERM));
        }
        return initializedLayer;
    }

    /**
     * Computes the parents of a node and confronts it with a Set of active nodes.
     * If all parents are in the Set of active nodes, the method returns true.
     *
     * @param node The node which we want to check.
     * @param activeNodes The reference against which we want to check.
     * @return true if all parents are active, false otherwise.
     */
    public boolean allParentsActive(TermId node, Set<TermId> activeNodes){
        Set<TermId> parents = new HashSet<>();
        parents.addAll( hpoGraph.extendWithParents(node, false));
        if (parents.isEmpty()){
            return true; // should only happen for root term
        }
        parents.removeAll(activeNodes);
        return parents.isEmpty();
    }
}
