package org.p2gx.boqa.core.algorithm;

import java.util.HashSet;
import java.util.Set;

import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.boqa.core.Counter;
import org.p2gx.boqa.core.internal.OntologyTraverser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetCounter implements Counter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SetCounter.class);
    private static final TermId PHENOTYPIC_ABNORMALITY = TermId.of("HP:0000118");

    private final OntologyTraverser ontologyTraverser;
    private final Ontology hpo;
    private final Set<TermId> patientLayer;

    public SetCounter(
            Ontology hpo,
            Set<TermId> patientHpos
    ) {
        this.ontologyTraverser = new OntologyTraverser(hpo);
        this.hpo = hpo;
        this.patientLayer =  ontologyTraverser.getObservedWithAncestors(patientHpos);
    }



    private boolean isPhenotypicFeature(TermId tid) {
        return this.hpo.graph().existsPath(tid, PHENOTYPIC_ABNORMALITY);
    }


     /**
     * COPIED FROM BoqaSetCounter. After testing we should make this a default in the interface!
     * This method computes counts given a disease ID and a patient's observed HPO terms.
     * These counts are related to true/false positives and true/false negatives, and are used later to compute the
     * probability that a patient has the input disease.
     *
     * @param diseaseObservedHpoIds
     * @return a {@link BoqaCounts} record containing the four counts for this disease-patient pair
     * @implNote Consider caching children of all ON nodes to improve offNodesCount calculation.
     */
    public BoqaCounts computeBoqaCounts(
             Set<TermId> diseaseObservedHpoIds
     ) {
        Set<TermId> diseaseLayer = ontologyTraverser.getObservedWithAncestors(diseaseObservedHpoIds);

        // TP
        Set<TermId> truePositives = new HashSet<>(diseaseLayer);
        truePositives.retainAll(patientLayer);

        // FP
        Set<TermId> falsePositives = new HashSet<>(patientLayer);
        falsePositives.removeAll(diseaseLayer);

        // FN
        Set<TermId> falseNegatives = new HashSet<>(diseaseLayer);
        falseNegatives.removeAll(patientLayer); // equivalent with removeAll(intersection)
        // Now iterate over these and count only those with all parents ON
        int betaCounts = 0; // exponent of beta
        for (TermId node : falseNegatives) {
            if (ontologyTraverser.allParentsActive(node, patientLayer)) {
                betaCounts += 1;
            }
        }
        int offNodesCount = 0; // exponent of 1-alpha
        Set<TermId> checkedNodes = new HashSet<>(); // used to avoid overcounting
        for (TermId qobs : patientLayer) {
            Set<TermId> children = new HashSet<>(ontologyTraverser.getHpoGraph().extendWithChildren(qobs, false));
            // Go through all children of ON terms
            for (TermId child : children) { // TODO consider a set with children of all of the terms
                // Find those that are off
                if (!patientLayer.contains(child)) {
                    // Check if they are also off in the disease Layer
                    if (!diseaseLayer.contains(child)) {
                        // Make sure the node has not already been counted
                        if (!checkedNodes.contains(child)) {
                            // increase counter iff all parents are ON
                            if (ontologyTraverser.allParentsActive(child, patientLayer)) {
                                offNodesCount += 1;
                                checkedNodes.add(child);
                            }
                        }
                    }
                }
            }
        }
        LOGGER.debug("True positives: {}, False positives: {}, (BOQA) True negatives: {}, (BOQA) False negatives: {}", truePositives.size(), falsePositives.size(), offNodesCount, betaCounts);

        return new BoqaCounts(truePositives.size(), falsePositives.size(), offNodesCount, betaCounts);
    }

    @Override
    public Set<String> getDiseaseIds() {
        return Set.of();
    }
}