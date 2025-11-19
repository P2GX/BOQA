package org.p2gx.boqa.core.algorithm;

import org.monarchinitiative.phenol.graph.OntologyGraph;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.boqa.core.Counter;
import org.p2gx.boqa.core.DiseaseData;
import org.p2gx.boqa.core.PatientData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class initializes all disease layers through its constructor, i.e. it computes the full induced HPO graph via
 * disease-phenotype annotations for all diseases. <p>
 * Its method {@link #computeBoqaCounts(String, PatientData) ComputeBoqaCounts} contains the BOQA algorithm which, for
 * a given set of observed HPO terms as TermIds belonging to a patient, counts the four integers needed to compute each
 * disease's probability, see also the record {@link BoqaCounts BoqaCounts}.
 * <p>
 *
 * @author <a href="mailto:leonardo.chimirri@bih-charite.de">Leonardo Chimirri</a>
 * <p>
 * @implNote Consider implementing XML or JSON serialization to cache disease layers, avoiding recomputation.
 * Avoid `Serializable` interface, since it is heavily criticized and deprecated.
 * Especially important for melded/digenic where combinatorial complexity increases.
 * @todo should idToLabel live in {@link DiseaseData}?
 */
public class BoqaSetCounter implements Counter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BoqaSetCounter.class);
    private static final TermId PHENOTYPIC_ABNORMALITY = TermId.of("HP:0000118");

    private final GraphTraverser graphTraverser;
    private final Map<TermId, Set<TermId>> diseaseLayers;
    private final Set<String> diseaseIds;
    private final Map<String, String> idToLabel;

    /**
     * Constructs a BoqaSetCounter using the default behavior (fullOntology = false).
     * <p>
     * This is a convenience constructor that carries out expected behavior, namely it initializes
     * disease layers by only considering descendants of the "Phenotypic Abnormality" term.
     *
     * @param diseaseData the disease data containing disease IDs, labels, and observed phenotypes
     * @param hpo         the HPO ontology used to traverse and expand phenotype terms
     */
    public BoqaSetCounter(DiseaseData diseaseData, Ontology hpo) {
        this(diseaseData, hpo, false);
    }

    /**
     * Constructs a BoqaSetCounter and initializes all disease layers.
     * <p>
     * Precomputes the induced HPO graph for each disease based on observed phenotypes from {@link DiseaseData}.
     * The computation considers only descendants of the "Phenotypic Abnormality" term unless {@code useFullOntology}
     * is set to true.
     *
     * @param diseaseData     the disease data containing disease IDs, labels, and observed phenotypes
     * @param hpo             the HPO ontology used to traverse and expand phenotype terms
     * @param useFullOntology if true, include all terms in the ontology without filtering by Phenotypic Abnormality;
     *                        primarily for legacy testing purposes
     */
    BoqaSetCounter(DiseaseData diseaseData, Ontology hpo, boolean useFullOntology) {
        this.idToLabel = Map.copyOf(diseaseData.getIdToLabel());
        this.graphTraverser = new GraphTraverser(hpo, useFullOntology);
        this.diseaseIds = Set.copyOf(diseaseData.getDiseaseIds());
        LOGGER.info("Initializing disease layers for {} diseases", diseaseIds.size());
        OntologyGraph<TermId> hpoGraph = graphTraverser.getHpoGraph();
        Set<TermId> phenotypicAbnormalities = Set.copyOf(hpoGraph.getDescendantSet(useFullOntology ? hpoGraph.root() : PHENOTYPIC_ABNORMALITY));
        this.diseaseLayers = diseaseIds.parallelStream()
                .collect(Collectors.toUnmodifiableMap(TermId::of, diseaseId -> {
                    Set<TermId> diseasePhenotypes = diseaseData.getObservedDiseaseFeatures(diseaseId).stream()
                            .map(TermId::of)
                            .filter(phenotypicAbnormalities::contains)
                            .collect(Collectors.toSet());
                    return graphTraverser.initLayer(diseasePhenotypes);
                }));
        LOGGER.info("Finished initializing disease layers");
    }

    /**
     * This method computes counts given a disease ID and a patient's observed HPO terms.
     * These counts are related to true/false positives and true/false negatives, and are used later to compute the
     * probability that a patient has the input disease.
     *
     * @param diseaseId   the unique OMIM ID of the disease whose counts are computed
     * @param patientData the patient data containing observed HPO terms and patient ID
     * @return a {@link BoqaCounts} record containing the four counts for this disease-patient pair
     * @implNote Consider caching children of all ON nodes to improve offNodesCount calculation.
     */
    @Override
    public BoqaCounts computeBoqaCounts(String diseaseId, PatientData patientData) {
        Set<TermId> observedHpos = patientData.getObservedTerms();
        Set<TermId> queryLayer = graphTraverser.initLayer(observedHpos);
        Set<TermId> diseaseLayer = diseaseLayers.get(TermId.of(diseaseId));

        boolean fpFullAnnotPropRule = true;
        boolean tpFullAnnotPropRule = true;
        boolean fnFullAnnotPropRule = true;
        boolean tnDiscard = true;


        // TP
        Set<TermId> intersection = new HashSet<>(diseaseLayer);
        intersection.retainAll(queryLayer);
        int tpCounts=0;
        if (tpFullAnnotPropRule) {
            for (TermId node : intersection) {
                if (graphTraverser.allChildrenInactive(node, queryLayer)) {
                    tpCounts += 1;
                }
            }
        }
        else tpCounts = intersection.size();

        // FP
        Set<TermId> falsePositives = new HashSet<>(queryLayer);
        falsePositives.removeAll(diseaseLayer);
        int fpCounts=0;
        if(fpFullAnnotPropRule){
            for (TermId node : falsePositives){
                if(graphTraverser.allChildrenInactive(node, queryLayer)){
                    fpCounts += 1 ;
                }
            }
        }
        else {
            fpCounts=falsePositives.size();
        }

        // FN
        Set<TermId> falseNegatives = new HashSet<>(diseaseLayer);
        falseNegatives.removeAll(queryLayer); // equivalent with removeAll(intersection)
        // Now iterate over these and count only those with all parents ON
        int betaCounts = 0; // exponent of beta
        if(fnFullAnnotPropRule) {
            for (TermId node : falseNegatives) {
                if (graphTraverser.allParentsActive(node, queryLayer)) {
                    betaCounts += 1;
                }
            }
        }
        else betaCounts = falseNegatives.size();

        int offNodesCount=0;
        if(!tnDiscard) {
            Set<TermId> checkedNodes = new HashSet<>(); // used to avoid overcounting
            // TODO  actually I think we always need a third condition for TNs...
            for (TermId qobs : queryLayer) {
                Set<TermId> children = new HashSet<>(graphTraverser.getHpoGraph().extendWithChildren(qobs, false));
                // Go through all children of ON terms
                for (TermId child : children) { // TODO consider a set with children of all of the terms
                    // Find those that are off
                    if (!queryLayer.contains(child)) {
                        // Check if they are also off in the disease Layer
                        if (!diseaseLayer.contains(child)) {
                            // Make sure the node has not already been counted
                            if (!checkedNodes.contains(child)) {
                                // increase counter iff all parents are ON
                                if (graphTraverser.allParentsActive(child, queryLayer)) {
                                    offNodesCount += 1;
                                    checkedNodes.add(child);
                                }
                            }
                        }
                    }
                }
            }
        }
        LOGGER.debug("True positives: {}, False positives: {}, (BOQA) True negatives: {}, (BOQA) False negatives: {}", tpCounts, falsePositives.size(), offNodesCount, betaCounts);
        LOGGER.debug("BOQA counts computed for disease {} ({})", diseaseId, idToLabel.get(diseaseId));

        return new BoqaCounts(diseaseId, idToLabel.get(diseaseId), tpCounts, fpCounts, offNodesCount, betaCounts);
    }

    @Override
    public Set<String> getDiseaseIds() {
        return this.diseaseIds;
    }
}
