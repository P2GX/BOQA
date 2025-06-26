package com.github.p2gx.boqa.core;

import java.nio.file.Path;
import java.util.Set;

public class CounterDummy implements Counter{

    Set<String> diseaseIdSet;

    public CounterDummy(DiseaseData diseaseData) {
        // Create hidden layers for all diseases and represent them somehow e.g. sets of HPO terms
        this.diseaseIdSet = diseaseData.getDiseaseIds();
    }

    public CounterDummy(Path serializedFile) {
        // Or read CouterDummy object with precalculated hidden layers from a serialized file
    }

    @Override
    public void initQueryLayer(Set<String> queryTerms) {
        // Init Query layer
    }

    @Override
    public BoqaCounts getBoqaCounts(String diseaseId) {
        return new BoqaCounts(diseaseId, 10, 10, 10, 10);
    }

    @Override
    public Set<String> getDiseaseIds() {
        return this.diseaseIdSet;
    }
}
