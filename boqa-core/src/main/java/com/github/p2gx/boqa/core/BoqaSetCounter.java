package com.github.p2gx.boqa.core;

import org.monarchinitiative.phenol.graph.OntologyGraph;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class counts the four quantities needed by the BOQA algorithm.
 *  TODO implement serializable, no need to recompute diseaseLayers each time
 *  Especially important for melded/digenic where combinatorial complexity increases
 */
public class BoqaSetCounter implements Counter, Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(BoqaSetCounter.class);

    private GraphTraversing graphTraverser;
    private Set<TermId> queryLayerInitialized;
    private final Map<TermId, Set<TermId>> diseaseLayers = new HashMap<>();
    private final Set<String> diseaseIds;

    // TODO for each disease in diseaseData compute ancestors OR load from disk (?) [--> add serialize object]
    public BoqaSetCounter(DiseaseData diseaseData, OntologyGraph<TermId> hpoGraph){
        this.graphTraverser = new GraphTraversing(hpoGraph);
        // this.diseaseIds.stream().parallel() -- might be faster than for loop, use forEach method
        this.diseaseIds = diseaseData.getDiseaseIds();
        for (String d : diseaseIds){
            Set<String> observedHpos = diseaseData.getIncludedDiseaseFeatures(d);
            Set<TermId> observedHposTerms = observedHpos.stream()
                        .map(TermId::of)
                        .collect(Collectors.toSet());
            diseaseLayers.put(TermId.of(d), graphTraverser.initLayer(observedHposTerms));
        }
    }

    /**
     *
     * @param diseaseId
     * @param observedHpos
     * @return BoqaCounts object containing for counts.
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
        falseNegatives.removeAll(intersection); // Now iterate over these and count only those with all parents ON
        int betaCounts = 0; // exponent of beta
        for(TermId node : falseNegatives) {
            if (graphTraverser.allParentsActive(node, queryLayerInitialized)) {
                betaCounts += 1;
            }
        }
        int offNodesCount = 0; // exponent of 1-alpha
        for(TermId qobs : queryLayerInitialized){
            Set<TermId> children = new HashSet<>();
            children.addAll( graphTraverser.getHpoGraph().extendWithChildren(qobs, false));
            // Go through all children and find those that are off, increase counter iff *all* parents are ON
            for(TermId child : children){
                if(!queryLayerInitialized.contains(child)){
                    // increase counter iff all parents are ON
                    if (graphTraverser.allParentsActive(child, queryLayerInitialized)){
                        offNodesCount +=1 ;
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
