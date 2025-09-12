package com.github.p2gx.boqa.core.algorithm;

import org.monarchinitiative.phenol.graph.NodeNotPresentInGraphException;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.graph.OntologyGraph;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Utility class for traversing and querying an HPO {@link OntologyGraph}.
 * <p>
 * This class centralizes operations needed in the BOQA algorithm to:
 * <ul>
 *   <li>Initialize a "layer" of ontology terms by expanding observed HPO terms
 *   with their ancestors ({@link #initLayer(Set)}).</li>
 *   <li>Check whether all parents of a given term are active
 *   ({@link #allParentsActive(TermId, Set)}).</li>
 * </ul>
 *
 * <h3>Key behaviors</h3>
 * <ul>
 *   <li>If an outdated {@link TermId} is encountered (raising
 *   {@link NodeNotPresentInGraphException}), the primary replacement is resolved
 *   via {@link Ontology#getPrimaryTermId(TermId)} and logged (once only, through {@code LOGGED_REPLACEMENTS}).</li>
 *   <li>If {@code fullOntology} is set to {@code false}, the initialized layer excludes
 *   the root HPO term ({@code HP:0000001}), thereby retaining only phenotypic
 *   abnormalities.</li>
 * </ul>
 *
 * <h3>Thread safety</h3>
 * <p>
 * This class is effectively thread-safe for read operations, since the underlying
 * {@link Ontology} and {@link OntologyGraph} are immutable, and replacement logging
 * is guarded by a concurrent set.
 *
 * <h3>Usage example</h3>
 * <pre>{@code
 * Ontology hpo = ...;
 * GraphTraversing traverser = new GraphTraversing(hpo, false);
 *
 * Set<TermId> observed = Set.of(TermId.of("HP:0004322"));
 * Set<TermId> initialized = traverser.initLayer(observed);
 *
 * boolean parentsActive = traverser.allParentsActive(TermId.of("HP:0004322"), initialized);
 * }</pre>
 *
 * @author
 *   <a href="mailto:leonardo.chimirri@bih-charite.de">Leonardo Chimirri</a>
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
     * <p>
     * If an outdated HPO TermId is encountered through {@link NodeNotPresentInGraphException}, the method of
     * the HPO ontology {@link Ontology#getPrimaryTermId(TermId)} is used to retrieve the new one.
     * <pre>
     *      Set&lt;TermId&gt; someLayerInitialized = graphTraverser.initLayer(observedHpos);
     * </pre>
     * @param hpoTerms the set of observed HPO terms to initialize from
     * @return the initialized layer of terms including ancestors
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
     * Computes the parents of a {@code node} and confronts it with a Set of {@code activeNodes}.
     * If all parents are in the Set of activeNodes, the method returns true.
     *
     * @param node The node we check.
     * @param activeNodes The reference against which node is checked.
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

    public boolean allChildrenInactive(TermId node, Set<TermId> activeNodes){
        Set<TermId> children = new HashSet<>();
        children.addAll( hpo.graph().extendWithChildren(node, false));
        // if any children are in active nodes, return false, if not, return true
        if (children.retainAll(activeNodes)){
            return false;
        }
        else return true;
    }
}
