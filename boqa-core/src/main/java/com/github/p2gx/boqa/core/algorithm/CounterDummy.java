package com.github.p2gx.boqa.core.algorithm;

import com.github.p2gx.boqa.core.Counter;
import com.github.p2gx.boqa.core.DiseaseData;
import com.github.p2gx.boqa.core.PatientData;

import java.nio.file.Path;
import java.util.Set;
/**
 * Dummy class with no functionality. For development. Always returns the same BoqaCounts.
 */
public class CounterDummy implements Counter {

    Set<String> diseaseIdSet;

    public CounterDummy(DiseaseData diseaseData) {
        // Create hidden layers for all diseases and represent them somehow e.g. sets of HPO terms
        this.diseaseIdSet = diseaseData.getDiseaseIds();
    }

    public CounterDummy(Path serializedFile) {
        // Or read CouterDummy object with precalculated hidden layers from a serialized file
    }


    @Override
    public BoqaCounts computeBoqaCounts(String diseaseId, PatientData patientData) {
        return new BoqaCounts(diseaseId, "unimportantHere", 10, 10, 10, 10);
    }

    @Override
    public Set<String> getDiseaseIds() {
        return this.diseaseIdSet;
    }
}