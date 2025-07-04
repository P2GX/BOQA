package com.github.p2gx.boqa.core;

import org.monarchinitiative.phenol.ontology.data.Term;
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
    private Map<TermId, Set<TermId>> diseaseLayers = new HashMap<>();

    // TODO for each disease in diseaseData compute ancestors OR load from disk (?) [--> .ser, serialize object]
    public CounterSetApproach(DiseaseData diseaseData, Path hpoPath){
        File hpoFile = hpoPath.toFile();
        this.hpoOntology = OntologyLoader.loadOntology(hpoFile);
        Set<String> diseaseIds = diseaseData.getDiseaseIds();
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
    
    @Override
    public BoqaCounts computeBoqaCounts(String diseaseId){
        Set<TermId> diseaseLayer = diseaseLayers.get(TermId.of(diseaseId));
        Set<TermId> intersection = new HashSet<>(diseaseLayer); // use copy constructor
        intersection.retainAll(queryLayerInitialized);
        // TODO this is DUMMY. Implement
        return new BoqaCounts(diseaseId, intersection.size(), intersection.size(), intersection.size(),intersection.size());
    }

    @Override
    public Set<String> getDiseaseIds() {
        return Set.of();
    }

}
