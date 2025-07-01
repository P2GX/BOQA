package com.github.p2gx.boqa.core;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.stream.Collectors;

public class CounterSetApproach implements Counter{

    private final String hpo; // not a string
    private Set<TermId> queryLayerInitialized;
    private Map<TermId, Set<TermId>> diseaseLayers = new HashMap<>();

    // for each disease in diseaseData compute ancestors OR load from disk (?) [--> .ser, serialize object]
    public CounterSetApproach(Set<String> diseaseData, String hpo){
        this.hpo = hpo; // ontology file
        //NOTE diseaseData is a type/object
        // for each d in diseaseData
            // observedhpos = diseaseData.getDiseaseHpoTerms(d)
            // diseaseLayers.put(d, initLayer(observedhpos))
            // HashMap of Sets: each disease (key) associated to set of hpo terms (values are explicitly observed
            // hpos AND ancestors)
    }


    public void initQueryLayer(Set<String> queryTerms){
        // TODO should this live in phenol?
        Set<TermId> queryTermIDs = queryTerms.stream()
                .map(TermId::of)
                .collect(Collectors.toSet());
        this.queryLayerInitialized = initLayer(queryTermIDs);
    }

    Set<TermId> initLayer(Set<TermId> hpoterms){
        // TODO implement real function
        return hpoterms;
    }

    //TODO
    @Override
    public BoqaCounts getBoqaCounts(String diseaseId){
        return new BoqaCounts(diseaseId,10,10,10, 10);
    }

    @Override
    public Set<String> getDiseaseIds() {
        return Set.of();
    }

}
