package com.github.p2gx.boqa.core;

import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

public class BlendedDiseaseData implements DiseaseData{

    private final DiseaseData plainDiseaseData;
    HashMap<String, HashMap<String, Set<String>>> blendedDiseaseFeaturesDict;
    public BlendedDiseaseData(DiseaseData plainDiseaseData, String geneId) {
        this.plainDiseaseData = plainDiseaseData;
        // Get all diseases associated with the given geneId
        Set<String> geneIdAssociatedDiseases = geneIdAssociatedDiseases(geneId);
        // Create blended diseases by combining the HPO terms for all pairs of these diseases
    }

    Set<String> geneIdAssociatedDiseases(String geneId) {
        return this.plainDiseaseData.getDiseaseIds().stream()
                .filter(d -> this.plainDiseaseData.getDiseaseGeneIds(d).contains(geneId))
                .collect(Collectors.toSet());
    }

    @Override
    public int size() {
        return this.blendedDiseaseFeaturesDict.size();
    }

    @Override
    public Set<String> getDiseaseIds() {
        return this.blendedDiseaseFeaturesDict.keySet();
    }

    @Override
    public Set<String> getIncludedDiseaseFeatures(String diseaseId) {
        return Set.of();
    }

    @Override
    public Set<String> getExcludedDiseaseFeatures(String diseaseId) {
        return Set.of();
    }

    @Override
    public Set<String> getDiseaseGeneIds(String diseaseId) {
        return Set.of();
    }

    @Override
    public Set<String> getDiseaseGeneSymbols(String diseaseId) {
        return Set.of();
    }
}
