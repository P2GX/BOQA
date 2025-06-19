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
    private Map<TermId, Set<TermId>> diseaseLayers = new HashMap<>();

    // for each disease in diseaseData compute ancestors OR load from disk (?) [--> .ser, serialize object]
    public CounterSetApproach(DiseaseData diseaseData, Path hpoPath){
        File hpoFile = hpoPath.toFile();
        this.hpoOntology = OntologyLoader.loadOntology(hpoFile);
        for (DiseaseData d : diseaseData){ // for each not applicable to type disease data, fix this
            Set<String> observedHpos = d.getIncludedDiseaseFeatures(d.toString());
            Set<TermId> observedHposTerms = observedHpos.stream().map(TermId::of).collect(Collectors.toSet());
            diseaseLayers.put(TermId.of(d.toString()), initLayer(observedHposTerms));
        }
    }


    public void initQLayer(Set<String> queryTerms){
        // TODO should this live in phenol?
        Set<TermId> queryTermIDs = queryTerms.stream()
                .map(TermId::of)
                .collect(Collectors.toSet());
        this.queryLayerInitialized = initLayer(queryTermIDs);
    }

    Set<TermId> initLayer(Set<TermId> hpoTerms){
        List<TermId> observed = hpoTerms.stream().toList();
        Set<TermId> observedAncestors = new HashSet<>();
        for (TermId termId : observed) {
            observedAncestors.addAll( hpoOntology.graph().extendWithAncestors(termId, true));
        }
        return observedAncestors;
    }

    //TODO
    @Override
    public ArrayList<Integer> getCounts(String diseaseId){
        return new ArrayList<Integer>();
    }

}
