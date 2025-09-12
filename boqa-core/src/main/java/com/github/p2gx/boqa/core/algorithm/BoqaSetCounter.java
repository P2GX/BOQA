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
 * @implNote  Consider implementing XML or JSON serialization to cache disease layers, avoiding recomputation.
 * Avoid `Serializable` interface, since it is heavily criticized and deprecated.
 * Especially important for melded/digenic where combinatorial complexity increases.
 * @todo should idToLabel live in {@link DiseaseData}
 */
public class BoqaSetCounter implements Counter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BoqaSetCounter.class);

    private final GraphTraversing graphTraverser;
    private final Map<TermId, Set<TermId>> diseaseLayers;
    private final Set<String> diseaseIds;
    private final Map<String, String> idToLabel;

    public BoqaSetCounter(DiseaseData diseaseData,
                          Ontology hpo,
                          boolean fullOntology
    ){
        this.idToLabel = Map.copyOf(diseaseData.getIdToLabel());
        this.graphTraverser = new GraphTraversing(hpo, fullOntology);
        this.diseaseIds = Set.copyOf(diseaseData.getDiseaseIds());
        TermId PHENOTYPIC_ABNORMALITY = TermId.of("HP:0000118");
        LOGGER.info("Initializing disease layers for {} diseases", diseaseIds.size());
        Map<TermId, Set<TermId>> dLayers = diseaseIds.parallelStream()
                .collect(Collectors.toConcurrentMap(
                        d -> TermId.of(d),
                        d -> graphTraverser.initLayer(
                                diseaseData
                                        .getIncludedDiseaseFeatures(d)
                                        .parallelStream()
                                        .map(TermId::of)
                                        .filter(tId -> fullOntology ||
                                                graphTraverser.getHpoGraph().isDescendantOf(tId, PHENOTYPIC_ABNORMALITY))
                                        .collect(Collectors.toSet())
                        )
                ));

        this.diseaseLayers = Map.copyOf(dLayers);
        LOGGER.info("Finished initializing disease layers");
    }

    /**
     * This method computes counts given a disease ID and a patient's observed HPO terms.
     * These counts are related to true/false positives and true/false negatives, and are used later to compute the
     * probability that a patient has the input disease.
     * @param diseaseId the unique OMIM ID of the disease whose counts are computed
     * @param patientData the patient data containing observed HPO terms and patient ID
     * @return a {@link BoqaCounts} record containing the four counts for this disease-patient pair
     * @implNote Consider caching children of all ON nodes to improve offNodesCount calculation.
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
        Set<TermId> falseNegatives = new HashSet<>(diseaseLayer); // FN
        falseNegatives.removeAll(queryLayerInitialized); // equivalent with removeAll(intersection)
        // Now iterate over these and count only those with all parents ON
        int betaCounts = 0; // exponent of beta
        for(TermId node : falseNegatives) {
            if (graphTraverser.allParentsActive(node, queryLayerInitialized)) {
                betaCounts += 1;
            }
        }
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
        LOGGER.debug("True positives: {}, False positives: {}, (BOQA) True negatives: {}, (BOQA) False negatives: {}",
                intersection.size(), falsePositives.size(), offNodesCount, betaCounts);
        LOGGER.debug("BOQA counts computed for disease {} ({})", diseaseId, idToLabel.get(diseaseId));

        return new BoqaCounts(diseaseId,
                idToLabel.get(diseaseId),
                intersection.size(),
                falsePositives.size(),
                offNodesCount,
                betaCounts);
    }

    @Override
    public Set<String> getDiseaseIds() {
        return this.diseaseIds;
    }
}
