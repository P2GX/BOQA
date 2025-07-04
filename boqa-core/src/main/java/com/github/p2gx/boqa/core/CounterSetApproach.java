package com.github.p2gx.boqa.core;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class CounterSetApproach implements Counter{
    private static final Logger LOGGER = LoggerFactory.getLogger(CounterSetApproach.class);


    private final Ontology hpoOntology;
    private Set<TermId> queryLayerInitialized;
    private final Map<TermId, Set<TermId>> diseaseLayers = new HashMap<>();
    private final Set<String> diseaseIds;

    // TODO for each disease in diseaseData compute ancestors OR load from disk (?) [--> .ser, serialize object]
    public CounterSetApproach(DiseaseData diseaseData, Path hpoPath){
        File hpoFile = hpoPath.toFile();
        this.hpoOntology = OntologyLoader.loadOntology(hpoFile);
        this.diseaseIds = diseaseData.getDiseaseIds();
        for (String d : diseaseIds){ // for each not applicable to type disease data, fix this
            Set<String> observedHpos = diseaseData.getIncludedDiseaseFeatures(d);
            Set<TermId> observedHposTerms = observedHpos.stream()
                        .map(TermId::of)
                        .collect(Collectors.toSet());
            diseaseLayers.put(TermId.of(d), initLayer(observedHposTerms));
        }
    }

    public void initQueryLayer(Set<String> queryTerms){
        // TODO should the following line live in phenol?
        Set<TermId> queryTermIds = queryTerms.stream()
                .map(TermId::of)
                .collect(Collectors.toSet());
        this.queryLayerInitialized = initLayer(queryTermIds);
    }

    Set<TermId> initLayer(Set<TermId> hpoTerms){
        List<TermId> observed = hpoTerms.stream().toList();
        Set<TermId> observedAncestors = new HashSet<>();
        for (TermId termId : observed) {
            observedAncestors.addAll( hpoOntology.graph().extendWithAncestors(termId, true));
        }
        return observedAncestors;
    }

    boolean allParentsActive(TermId node){
        // TODO this supposes we are in the query layer. Generalize?
        Set<TermId> parents = new HashSet<>();
        parents.addAll( hpoOntology.graph().extendWithParents(node, false));
        // increase counter iff (parents \ queryLayerInitialized) is the empty set
        parents.removeAll(queryLayerInitialized);
        return parents.isEmpty();
    }

    @Override
    public BoqaCounts computeBoqaCounts(String diseaseId){
        Set<TermId> diseaseLayer = diseaseLayers.get(TermId.of(diseaseId));
        Set<TermId> intersection = new HashSet<>(diseaseLayer); // use copy constructor
        intersection.retainAll(queryLayerInitialized); // TP
        Set<TermId> falsePositives = new HashSet<>(queryLayerInitialized); // FP
        falsePositives.removeAll(diseaseLayer);
        Set<TermId> falseNegatives = new HashSet<>(diseaseLayer); // FN
        falseNegatives.removeAll(intersection); // Now iterate over these and count only those with all parents ON
        int betaCounts = 0; // exponent of beta
        for(TermId node : falseNegatives) {
            if (allParentsActive(node)) {
                betaCounts += 1;
            }
        }
        int offNodesCount = 0; // exponent of 1-alpha
        for(TermId qobs : queryLayerInitialized){
            Set<TermId> children = new HashSet<>();
            children.addAll( hpoOntology.graph().extendWithChildren(qobs, false));
            // Go through all children and find those that are off, increase counter iff *all* parents are ON
            for(TermId child : children){
                if(!queryLayerInitialized.contains(child)){
                    // increase counter iff all parents are ON
                    if (allParentsActive(child)){
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
