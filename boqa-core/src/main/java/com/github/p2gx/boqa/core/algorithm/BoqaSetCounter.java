package com.github.p2gx.boqa.core.algorithm;

import com.github.p2gx.boqa.core.Counter;
import com.github.p2gx.boqa.core.DiseaseData;
import com.github.p2gx.boqa.core.PatientData;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class initializes all disease layers through its constructor, i.e. it computes the full induced HPO graph via
 * disease-phenotype annotations for all diseases. <p>
 * Its method {@link #computeBoqaCounts(String, PatientData) ComputeBoqaCounts} contains the BOQA algorithm which, for
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

    private final GraphTraversing graphTraverser;
    private final Map<TermId, Set<TermId>> diseaseLayers;
    private final Set<String> diseaseIds;
    private final Map<String, String> idToLabel; //TODO move to disease dataa

    // TODO for each disease in diseaseData compute ancestors OR load from disk
    public BoqaSetCounter(DiseaseData diseaseData,
                          Ontology hpo,
                          boolean fullOntology
    ){
        this.idToLabel = diseaseData.getIdToLabel(); // TODO make immutable or get rid of this
        this.graphTraverser = new GraphTraversing(hpo, fullOntology); // immutable?
        this.diseaseIds = Set.copyOf(diseaseData.getDiseaseIds()); // immutable
        Map<TermId, Set<TermId>> dLayers = new HashMap<>(); // TODO change to stream ?
        TermId PHENOTYPIC_ABNORMALITY = TermId.of("HP:0000118");
        diseaseIds.forEach(
                d -> dLayers.put(
                        TermId.of(d),
                        graphTraverser.initLayer(
                            diseaseData
                                    .getIncludedDiseaseFeatures(d)
                                    .parallelStream()
                                    .map(TermId::of)
                                    .filter(tId -> fullOntology || graphTraverser // only filter when fullOntology is false
                                            .getHpoGraph()
                                            .isDescendantOf(tId, PHENOTYPIC_ABNORMALITY)) // filter for descendants
                                    .collect(Collectors.toSet() )
                        )
                )
        );
        this.diseaseLayers = Map.copyOf(dLayers);
    }

    /**
     * This method computes counts given a disease ID and a patient's observed HPO terms.
     * These counts are related to true/false positives and true/false negatives, and are used to compute the
     * probability that a patient has the input disease. The probability is computed as <p>
     * P = alpha^tpBoqaCount * beta^fpBoqaCount * (1-alpha)^fnBoqaCount * (1-beta)^tpBoqaCount
     * @param diseaseId
     * @param patientData
     * @return BoqaCounts record containing four counts associated to a diseases-patient pair.
     */
    @Override
    public BoqaCounts computeBoqaCounts(String diseaseId, PatientData patientData){
        Set<TermId> observedHpos = patientData.getObservedTerms();
        Set<TermId> queryLayerInitialized = graphTraverser.initLayer(observedHpos);
        Set<TermId> diseaseLayer = diseaseLayers.get(TermId.of(diseaseId));
        Set<TermId> intersection = new HashSet<>(diseaseLayer); // use copy constructor
        intersection.retainAll(queryLayerInitialized); // TP
        Set<TermId> falsePositives = new HashSet<>(queryLayerInitialized); // FP
        falsePositives.removeAll(diseaseLayer);
        // Do not overcount fps
        int fpcount = 0;
        for(TermId node : falsePositives){
            if (graphTraverser.allChildrenInactive(node, queryLayerInitialized)){
                fpcount += 1;
            }
        }
        Set<TermId> falseNegatives = new HashSet<>(diseaseLayer); // FN
        falseNegatives.removeAll(queryLayerInitialized); // equivalent with removeAll(intersection)
        // Now iterate over these and count only those with all parents ON
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
            for(TermId child : children){ // TODO consider a set with children of all of the terms
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
        return new BoqaCounts(diseaseId, idToLabel.get(diseaseId), intersection.size(), fpcount, offNodesCount, betaCounts);
    }

    @Override
    public Set<String> getDiseaseIds() {
        return this.diseaseIds;
    }
}
