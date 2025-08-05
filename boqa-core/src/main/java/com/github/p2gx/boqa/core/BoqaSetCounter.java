package com.github.p2gx.boqa.core;

import org.monarchinitiative.phenol.graph.OntologyGraph;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class initializes all disease layers through its constructor, i.e. it computes the full induced HPO graph via
 * disease-phenotype annotations for all diseases. <p>
 * Its method {@link #computeBoqaCounts(String, Set<TermId>) ComputeBoqaCounts} contains the BOQA algorithm which, for
 * a given set of observed HPO terms as TermIds belonging to a patient, counts the four integers needed to compute each
 * disease's probability, see also the record {@link BoqaCounts BoqaCounts}.
 * <p>
 * @author <a href="mailto:leonardo.chimirri@bih-charite.de">Leonardo Chimirri</a>
 * <p>
 * TODO implement serialization via XML or JSON, no need to recompute diseaseLayers each time.
 * Try to avoid Serialization, since it is heavily criticized and deprecated.
 * Especially important for melded/digenic where combinatorial complexity increases.
 */
public class BoqaSetCounter implements Counter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BoqaSetCounter.class);

    private GraphTraversing graphTraverser;
    private final Map<TermId, Set<TermId>> diseaseLayers = new HashMap<>();
    private final Set<String> diseaseIds;

    // TODO for each disease in diseaseData compute ancestors OR load from disk
    public BoqaSetCounter(DiseaseData diseaseData, OntologyGraph<TermId> hpoGraph, boolean fullOntology){
        this.graphTraverser = new GraphTraversing(hpoGraph, fullOntology);
        this.diseaseIds = diseaseData.getDiseaseIds();
        diseaseIds.forEach(
                d -> diseaseLayers.put(
                        TermId.of(d),
                        graphTraverser.initLayer(
                            diseaseData
                                    .getIncludedDiseaseFeatures(d)
                                    .parallelStream()
                                    .map(TermId::of)
                                    .collect(Collectors.toSet() )
                        )
                )
        );
    }

    /**
     * This method computes counts given a disease ID and a patient's observed HPO terms.
     * These counts are related to true/false positives and true/false negatives, and are used to compute the
     * probability that a patient has the input disease. The probability is computed as <p>
     * P = alpha^tpExponent * beta^fpExponent * (1-alpha)^fnExponent * (1-beta)^tpExponent
     * @param diseaseId
     * @param observedHpos
     * @return BoqaCounts record containing four counts associated to a diseases-patient pair.
     */
    @Override
    public BoqaCounts computeBoqaCounts(String diseaseId, Set<TermId> observedHpos){
        Set<TermId> queryLayerInitialized = graphTraverser.initLayer(observedHpos);
        Set<TermId> diseaseLayer = diseaseLayers.get(TermId.of(diseaseId));
        Set<TermId> intersection = new HashSet<>(diseaseLayer); // use copy constructor
        intersection.retainAll(queryLayerInitialized); // TP
        Set<TermId> falsePositives = new HashSet<>(queryLayerInitialized); // FP
        falsePositives.removeAll(diseaseLayer);
        Set<TermId> falseNegatives = new HashSet<>(diseaseLayer); // FN
        //falseNegatives.removeAll(queryLayerInitialized); // TODO is this the right one?
        falseNegatives.removeAll(intersection); // Now iterate over these and count only those with all parents ON
        int betaCounts = 0; // exponent of beta
        for(TermId node : falseNegatives) {
            if (graphTraverser.allParentsActive(node, queryLayerInitialized)) {
                betaCounts += 1;
            }
        }
        //TODO the following is probably too expensive?
        int offNodesCount = 0; // exponent of 1-alpha
        Set<TermId> checkedNodes = new HashSet<>(); // used to avoid overcounting
        for(TermId qobs : queryLayerInitialized){
            Set<TermId> children = new HashSet<>(
                    graphTraverser.getHpoGraph().extendWithChildren(qobs, false));
            // Go through all children of ON terms
            for(TermId child : children){
                // Find those that are off
                if (!queryLayerInitialized.contains(child)) {
                    // Check if they are also off in the disease Layer
                    if(!diseaseLayer.contains(child)) {
                        // Make sure the node has not already been counted
                        if (!checkedNodes.contains(child)) {
                            // increase counter iff all parents are ON
                            if (graphTraverser.allParentsActive(child, queryLayerInitialized)) {
                                offNodesCount += 1;
                                checkedNodes.add(child);
                            }
                        }
                    }
                }
            }
        }
        return new BoqaCounts(diseaseId, intersection.size(), falsePositives.size(), offNodesCount, betaCounts);
    }

    @Override
    public Set<String> getDiseaseIds() {
        return this.diseaseIds;
    }
}
