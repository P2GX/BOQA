package org.p2gx.boqa.core.diseases;

import org.p2gx.boqa.core.DiseaseData;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultDiseaseData implements DiseaseData {

    private final Map<String, DiseaseFeatures> diseaseFeaturesById;
    private final Map<String, String> idToLabel;

    DefaultDiseaseData(Map<String, DiseaseFeatures> diseaseFeaturesById) {
        this.diseaseFeaturesById = diseaseFeaturesById;
        this.idToLabel = this.diseaseFeaturesById.values()
                .stream()
                .collect(Collectors.toUnmodifiableMap(DiseaseFeatures::id, DiseaseFeatures::label));
    }

    @Override
    public Map<String, String> getIdToLabel() {
        return idToLabel;
    }

    @Override
    public int size() {
        return diseaseFeaturesById.size();
    }

    @Override
    public Set<String> getDiseaseIds() {
        return diseaseFeaturesById.keySet();
    }

    @Override
    public Set<String> getObservedDiseaseFeatures(String diseaseId) {
        return getDiseaseFeatures(diseaseId).observedPhenotypes();
    }

    @Override
    public Set<String> getExcludedDiseaseFeatures(String diseaseId) {
        return getDiseaseFeatures(diseaseId).excludedPhenotypes();
    }

    @Override
    public Set<String> getDiseaseGeneIds(String diseaseId) {
        return getDiseaseFeatures(diseaseId).geneIds();
    }

    @Override
    public Set<String> getDiseaseGeneSymbols(String diseaseId) {
        return getDiseaseFeatures(diseaseId).geneSymbols();
    }

    private DiseaseFeatures getDiseaseFeatures(String diseaseId) {
        DiseaseFeatures diseaseFeatures = diseaseFeaturesById.get(diseaseId);
        if (diseaseFeatures == null) {
            throw new IllegalArgumentException("Disease ID \"" + diseaseId + "\" not found!");
        }
        return diseaseFeatures;
    }
}
