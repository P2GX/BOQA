package com.github.p2gx.boqa.core.algorithm;

import org.monarchinitiative.phenol.graph.NodeNotPresentInGraphException;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.graph.OntologyGraph;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This utility class holds the ontology graph and methods acting on it to collect necessary sets of terms.
 * <p>
 * @author <a href="mailto:leonardo.chimirri@bih-charite.de">Leonardo Chimirri</a>
 */
class GraphTraversing {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphTraversing.class);
    private static final Set<TermId> LOGGED_REPLACEMENTS = ConcurrentHashMap.newKeySet();

    public static final String HPO_ROOT_TERM = "HP:0000001";
    private final Ontology hpo;
    private final boolean fullOntology;

    public OntologyGraph<TermId> getHpoGraph() {
        return hpo.graph();
    }
    public GraphTraversing(Ontology hpo, boolean fullOntology) {
        this.hpo = hpo;
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
        hpoTerms.forEach(t -> {
            Collection<TermId> traversedHpos;
            try {
                traversedHpos = hpo.graph().extendWithAncestors(t, true);
            } catch (NodeNotPresentInGraphException e){
                TermId primaryTid = hpo.getPrimaryTermId(t);
                traversedHpos = hpo.graph().extendWithAncestors(primaryTid, true);
                // Log once only
                if (LOGGED_REPLACEMENTS.add(t)) {
                    LOGGER.warn("Replacing {} with primary term {}", t, primaryTid);
                }
            }
            initializedLayer.addAll(traversedHpos);
        });
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
        parents.addAll( hpo.graph().extendWithParents(node, false));
        if (parents.isEmpty()){
            return true; // should only happen for root term
        }
        parents.removeAll(activeNodes);
        return parents.isEmpty();
    }
}
