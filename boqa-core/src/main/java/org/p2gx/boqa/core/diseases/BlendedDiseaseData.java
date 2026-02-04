package org.p2gx.boqa.core.diseases;

import org.p2gx.boqa.core.DiseaseData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class implements an obvious approach to analyzing blended phenotypes using BOQA.
 * 
 * <p>Given phenotypic features of a patient (query) and a disease gene,
 * determine all diseases associated with this gene and pair these diseases with all other diseases.
 * Create a BlendedDiseaseData object that returns the annotated HPO terms for individual diseases and disease pairs,
 * with the union of the terms from both diseases being returned for disease pairs.
 * Use the created BlendedDiseaseData object for the BOQA analysis.</p>
 * 
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 */
public class BlendedDiseaseData implements DiseaseData {

    private final DiseaseData plainDiseaseData;
    HashMap<String, HashMap<String, Set<String>>> blendedDiseaseFeaturesDict;

    /**
     * Creates a BlendedDiseaseData object that combines diseases associated with a given gene
     * with all other annotated HPOA diseases.
     *
     * <p>For each disease associated with the gene ID, creates combined disease entities by
     * pairing it with every other disease in the dataset. The phenotypes of paired diseases
     * are merged (union) to create composite phenotypes.</p>
     *
     * @param plainDiseaseData the underlying disease data source
     * @param geneId the gene ID to use for filtering disease associations
     */
    public BlendedDiseaseData(DiseaseData plainDiseaseData, String geneId) {
        this.blendedDiseaseFeaturesDict = new HashMap<>();
        this.plainDiseaseData = plainDiseaseData;
        // Get a set of all diseases associated with the given geneId and a set of all diseases
        Set<String> geneIdAssociatedDiseases = geneIdAssociatedDiseases(geneId);

        // Add all of these diseases to BlendedDiseaseData
        for (String diseaseId : geneIdAssociatedDiseases) {
            this.blendedDiseaseFeaturesDict.putIfAbsent(diseaseId, new HashMap<>());
            this.blendedDiseaseFeaturesDict.get(diseaseId).put("I", new HashSet<>());
            this.blendedDiseaseFeaturesDict.get(diseaseId).get("I").addAll(this.plainDiseaseData.getObservedDiseaseFeatures(diseaseId));
            this.blendedDiseaseFeaturesDict.get(diseaseId).put("E", new HashSet<>());
            this.blendedDiseaseFeaturesDict.get(diseaseId).get("E").addAll(this.plainDiseaseData.getExcludedDiseaseFeatures(diseaseId));
            this.blendedDiseaseFeaturesDict.get(diseaseId).put("G", new HashSet<>());
            this.blendedDiseaseFeaturesDict.get(diseaseId).get("G").addAll(this.plainDiseaseData.getDiseaseGeneIds(diseaseId));
            this.blendedDiseaseFeaturesDict.get(diseaseId).put("GS", new HashSet<>());
            this.blendedDiseaseFeaturesDict.get(diseaseId).get("GS").addAll(this.plainDiseaseData.getDiseaseGeneSymbols(diseaseId));
        }

        // Create blended diseases by combining the HPO terms for all pairs of these diseases with all diseases
        Set<String> allDiseases = this.plainDiseaseData.getDiseaseIds();
        for (String diseaseId1 : geneIdAssociatedDiseases) {
            for (String diseaseId2 : allDiseases) {
                if (!diseaseId1.equals(diseaseId2)) {
                    String blendedDiseaseId = diseaseId1 + ',' + diseaseId2;
                    this.blendedDiseaseFeaturesDict.putIfAbsent(blendedDiseaseId, new HashMap<>());
                    this.blendedDiseaseFeaturesDict.get(blendedDiseaseId).put("I", new HashSet<>());
                    this.blendedDiseaseFeaturesDict.get(blendedDiseaseId).get("I").addAll(this.plainDiseaseData.getObservedDiseaseFeatures(diseaseId1));
                    this.blendedDiseaseFeaturesDict.get(blendedDiseaseId).get("I").addAll(this.plainDiseaseData.getObservedDiseaseFeatures(diseaseId2));
                    this.blendedDiseaseFeaturesDict.get(blendedDiseaseId).put("E", new HashSet<>());
                    this.blendedDiseaseFeaturesDict.get(blendedDiseaseId).get("E").addAll(this.plainDiseaseData.getExcludedDiseaseFeatures(diseaseId1));
                    this.blendedDiseaseFeaturesDict.get(blendedDiseaseId).get("E").addAll(this.plainDiseaseData.getExcludedDiseaseFeatures(diseaseId2));
                    this.blendedDiseaseFeaturesDict.get(blendedDiseaseId).put("G", new HashSet<>());
                    this.blendedDiseaseFeaturesDict.get(blendedDiseaseId).get("G").addAll(this.plainDiseaseData.getDiseaseGeneIds(diseaseId1));
                    this.blendedDiseaseFeaturesDict.get(blendedDiseaseId).get("G").addAll(this.plainDiseaseData.getDiseaseGeneIds(diseaseId2));
                    this.blendedDiseaseFeaturesDict.get(blendedDiseaseId).put("GS", new HashSet<>());
                    this.blendedDiseaseFeaturesDict.get(blendedDiseaseId).get("GS").addAll(this.plainDiseaseData.getDiseaseGeneSymbols(diseaseId1));
                    this.blendedDiseaseFeaturesDict.get(blendedDiseaseId).get("GS").addAll(this.plainDiseaseData.getDiseaseGeneSymbols(diseaseId2));
                }
            }
        }
    }

    Set<String> geneIdAssociatedDiseases(String geneId) {
        return this.plainDiseaseData.getDiseaseIds().stream()
                .filter(d -> this.plainDiseaseData.getDiseaseGeneIds(d).contains(geneId))
                .collect(Collectors.toSet());
    }

    /**
     Methods that implement the DiseaseDict interface
     */

    @Override
    public int size() {
        return this.blendedDiseaseFeaturesDict.size();
    }

    @Override
    public Set<String> getDiseaseIds() {
        return this.blendedDiseaseFeaturesDict.keySet();
    }

    @Override
    public Set<String> getObservedDiseaseFeatures(String diseaseId) {
        if (this.blendedDiseaseFeaturesDict.containsKey(diseaseId)) {
            return this.blendedDiseaseFeaturesDict.get(diseaseId).get("I");
        } else {
            throw new IllegalArgumentException("Disease ID \"" + diseaseId + "\" not found!");
        }
    }

    @Override
    public Set<String> getExcludedDiseaseFeatures(String diseaseId){
        if (this.blendedDiseaseFeaturesDict.containsKey(diseaseId)) {
            return this.blendedDiseaseFeaturesDict.get(diseaseId).get("E");
        } else {
            throw new IllegalArgumentException("Disease ID \"" + diseaseId + "\" not found!");
        }
    }

    @Override
    public Set<String> getDiseaseGeneIds(String diseaseId) {
        if (this.blendedDiseaseFeaturesDict.containsKey(diseaseId)) {
            if (this.blendedDiseaseFeaturesDict.get(diseaseId).containsKey("G")) {
                return this.blendedDiseaseFeaturesDict.get(diseaseId).get("G");
            } else {
                return new HashSet<>();
            }
        } else {
            throw new IllegalArgumentException("Disease ID \"" + diseaseId + "\" not found!");
        }
    }

    @Override
    public Set<String> getDiseaseGeneSymbols(String diseaseId) {
        if (this.blendedDiseaseFeaturesDict.containsKey(diseaseId)) {
            if (this.blendedDiseaseFeaturesDict.get(diseaseId).containsKey("GS")) {
                return this.blendedDiseaseFeaturesDict.get(diseaseId).get("GS");
            } else {
                return new HashSet<>();
            }
        } else {
            throw new IllegalArgumentException("Disease ID \"" + diseaseId + "\" not found!");
        }
    }
}