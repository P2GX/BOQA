package org.p2gx.boqa.core.diseases;

import org.p2gx.boqa.core.CandidateDiagnosis;
import org.p2gx.boqa.core.DiseaseData;
import org.p2gx.boqa.core.patient.DiseaseDTO;

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
    public int size() {
        return diseaseFeaturesById.size();
    }

    // TODO between here and the next todo we have a quick fix to simply make stuff compile
    @Override
    public Set<Set<DiseaseDTO>> getDiagnosisIds() {
        return Set.of();
    }

    @Override
    public Set<CandidateDiagnosis> getCandidateDiagnosisSet() {
        return Set.of();
    }

//    @Override
//    public Set<String> getDiseaseIds() {
//        return diseaseFeaturesById.keySet();
//    }
//
//    @Override
//    public Set<String> getObservedDiseaseFeatures(String diseaseId) {
//        return getDiseaseFeatures(diseaseId).observedPhenotypes();
//    }
//
//    @Override
//    public Set<String> getExcludedDiseaseFeatures(String diseaseId) {
//        return getDiseaseFeatures(diseaseId).excludedPhenotypes();
//    }
    // TODO end of quick fix to simply make stuff compile
    private DiseaseFeatures getDiseaseFeatures(String diseaseId) {
        DiseaseFeatures diseaseFeatures = diseaseFeaturesById.get(diseaseId);
        if (diseaseFeatures == null) {
            throw new IllegalArgumentException("Disease ID \"" + diseaseId + "\" not found!");
        }
        return diseaseFeatures;
    }
}
