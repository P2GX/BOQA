package org.p2gx.boqa.core.algorithm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseaseAnnotation;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.boqa.core.Counter;
import org.p2gx.boqa.core.PatientData;
import org.p2gx.boqa.core.diseases.CandidateDisease;
import org.p2gx.boqa.core.diseases.TargetDisease;
import org.p2gx.boqa.core.internal.OntologyTraverser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlendedCounter implements Counter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlendedCounter.class);
    private static final TermId PHENOTYPIC_ABNORMALITY = TermId.of("HP:0000118");

    private final OntologyTraverser ontologyTraverser;
    // OMIM to set of observed HPO and ancestors
    private final Map<TermId, Set<TermId>> diseaseLayers;
    private final Ontology hpo;


    public BlendedCounter(
        Ontology hpo,
        HpoDiseases diseases, 
        List<CandidateDisease> candidateDiseaseList
    ) {
        this.ontologyTraverser = new OntologyTraverser(hpo);
        this.hpo = hpo;
        this.diseaseLayers = new HashMap<>();

        Map<TermId, HpoDisease> hpoDiseaseMap = diseases.diseaseById();
        for (CandidateDisease candidate: candidateDiseaseList) {
             switch (candidate) {
              case CandidateDisease.Single s -> {
                TermId diseaseId = TermId.of(s.diseaseId());
                  HpoDisease hpoDisease = hpoDiseaseMap.get(diseaseId);
                  if (hpoDisease != null) {
                    Set<TermId> observed = new HashSet<>();
                    for (HpoDiseaseAnnotation hda : hpoDisease.presentAnnotations() ){
                        observed.add(hda.id());
                    }
                    diseaseLayers.put(diseaseId, observed);
                  }
              }
              case CandidateDisease.Blended b -> {
                  List<TargetDisease.Gene> list = b.components();
                  Set<TermId> observed = new HashSet<>();
                  for (TargetDisease td: list) {
                    TermId diseaseId = TermId.of(td.diseaseId());
                  HpoDisease hpoDisease = hpoDiseaseMap.get(diseaseId);
                  if (hpoDisease != null) {
                    for (HpoDiseaseAnnotation hda : hpoDisease.presentAnnotations() ){
                        observed.add(hda.id());
                    }
                    
                  }
                  }
                  TermId meldedId = TermId.of(b.diseaseId());
                  diseaseLayers.put(meldedId, observed);
              }
             }
        }
    }

    /**
     * COPIED FROM BoqaSetCounter. After testing we should make this a default in the interface!
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
        Set<TermId> queryLayer = ontologyTraverser.getObservedWithAncestors(observedHpos);
        Set<TermId> diseaseLayer = diseaseLayers.get(TermId.of(diseaseId));

        // TP
        Set<TermId> truePositives = new HashSet<>(diseaseLayer);
        truePositives.retainAll(queryLayer);

        // FP
        Set<TermId> falsePositives = new HashSet<>(queryLayer);
        falsePositives.removeAll(diseaseLayer);

        // FN
        Set<TermId> falseNegatives = new HashSet<>(diseaseLayer);
        falseNegatives.removeAll(queryLayer); // equivalent with removeAll(intersection)
        // Now iterate over these and count only those with all parents ON
        int betaCounts = 0; // exponent of beta
        for (TermId node : falseNegatives) {
            if (ontologyTraverser.allParentsActive(node, queryLayer)) {
                betaCounts += 1;
            }
        }
        int offNodesCount = 0; // exponent of 1-alpha
        Set<TermId> checkedNodes = new HashSet<>(); // used to avoid overcounting
        for (TermId qobs : queryLayer) {
            Set<TermId> children = new HashSet<>(ontologyTraverser.getHpoGraph().extendWithChildren(qobs, false));
            // Go through all children of ON terms
            for (TermId child : children) { // TODO consider a set with children of all of the terms
                // Find those that are off
                if (!queryLayer.contains(child)) {
                    // Check if they are also off in the disease Layer
                    if (!diseaseLayer.contains(child)) {
                        // Make sure the node has not already been counted
                        if (!checkedNodes.contains(child)) {
                            // increase counter iff all parents are ON
                            if (ontologyTraverser.allParentsActive(child, queryLayer)) {
                                offNodesCount += 1;
                                checkedNodes.add(child);
                            }
                        }
                    }
                }
            }
        }
        LOGGER.debug("True positives: {}, False positives: {}, (BOQA) True negatives: {}, (BOQA) False negatives: {}", truePositives.size(), falsePositives.size(), offNodesCount, betaCounts);

        return new BoqaCounts(diseaseId, "doesntmatter", truePositives.size(), falsePositives.size(), offNodesCount, betaCounts);
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
     * @param observedPatientHpoIds
     * @return a {@link BoqaCounts} record containing the four counts for this disease-patient pair
     * @implNote Consider caching children of all ON nodes to improve offNodesCount calculation.
     */
    @Override
    public BoqaCountsNew computeBoqaCountsFromDisease(
            Set<TermId> diseaseObservedHpoIds,
            Set<TermId> observedPatientHpoIds
    ) {
        Set<TermId> patientLayer = ontologyTraverser.getObservedWithAncestors(observedPatientHpoIds);
        Set<TermId> diseaseLayer = ontologyTraverser.getObservedWithAncestors(diseaseObservedHpoIds);
       // diseaseLayers.get(TermId.of(diseaseId));

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

        return new BoqaCountsNew(truePositives.size(), falsePositives.size(), offNodesCount, betaCounts);
    }

    @Override
    public Set<String> getDiseaseIds() {
        return Set.of();
    }

}