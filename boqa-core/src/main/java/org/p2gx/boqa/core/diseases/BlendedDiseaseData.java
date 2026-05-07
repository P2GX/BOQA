package org.p2gx.boqa.core.diseases;

import org.p2gx.boqa.core.DiseaseData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(BlendedDiseaseData.class);

    public enum PairingStrategy {
        /** Pairs each anchor disease with every disease not in the anchor set. Requires exactly one anchor gene. */
        ANCHOR_VS_ALL,
        /** Pairs anchor diseases with each other, excluding pairs that share a gene (unordered, no duplicates). */
        ANCHOR_VS_ANCHOR
    }

    private final DiseaseData plainDiseaseData;
    HashMap<String, HashMap<String, Set<String>>> blendedDiseaseFeaturesDict;

    public BlendedDiseaseData(DiseaseData plainDiseaseData, List<String> geneIds) {
        this(plainDiseaseData, geneIds, PairingStrategy.ANCHOR_VS_ALL);
    }

    /**
     * Creates a BlendedDiseaseData object that combines diseases associated with given genes
     * with other diseases according to the chosen pairing strategy.
     *
     * @param plainDiseaseData the underlying disease data source
     * @param geneIds the gene IDs to use for filtering disease associations
     * @param strategy the strategy used to form disease pairs
     */
    public BlendedDiseaseData(DiseaseData plainDiseaseData, List<String> geneIds, PairingStrategy strategy) {
        this.blendedDiseaseFeaturesDict = new HashMap<>();
        this.plainDiseaseData = plainDiseaseData;
        LOGGER.info("Initializing BlendedDiseaseData...");
        LOGGER.info("Number of anchor genes: {}", geneIds.size());
        LOGGER.info("Strategy: {}", strategy);
        if (strategy == PairingStrategy.ANCHOR_VS_ALL && geneIds.size() > 1) {
            throw new IllegalArgumentException(
                "ANCHOR_VS_ALL requires exactly one anchor gene, but " + geneIds.size() + " were provided.");
        }
        Set<String> geneIdAssociatedDiseases = geneIdAssociatedDiseases(geneIds);
        LOGGER.info("Number of diseases associated with input genes: {}", geneIdAssociatedDiseases.size());

        // Add all anchor diseases as singletons
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

        // Create a list of blended disease ID pairs to be added
        Set<String> allDiseases = this.plainDiseaseData.getDiseaseIds();
        List<String> blendedDiseaseIds = new ArrayList<>();
        switch (strategy) {
            case ANCHOR_VS_ALL -> {
                for (String diseaseId1 : geneIdAssociatedDiseases) {
                    for (String diseaseId2 : allDiseases) {
                        if (!geneIdAssociatedDiseases.contains(diseaseId2)) {
                            blendedDiseaseIds.add(diseaseId1 + '-' + diseaseId2);
                        }
                    }
                }
            }
            case ANCHOR_VS_ANCHOR -> {
                List<String> anchorList = new ArrayList<>(geneIdAssociatedDiseases);
                for (int i = 0; i < anchorList.size(); i++) {
                    for (int j = i + 1; j < anchorList.size(); j++) {
                        if (Collections.disjoint(
                                plainDiseaseData.getDiseaseGeneIds(anchorList.get(i)),
                                plainDiseaseData.getDiseaseGeneIds(anchorList.get(j)))) {
                            blendedDiseaseIds.add(anchorList.get(i) + '-' + anchorList.get(j));
                        }
                    }
                }
                if (blendedDiseaseIds.isEmpty()) {
                    LOGGER.warn("ANCHOR_VS_ANCHOR produced no pairs for genes {} — all anchor diseases share a common gene. Analysis will run on singletons only.", geneIds);
                }
            }
        }
        LOGGER.info("Generated {} blended disease pairs.", blendedDiseaseIds.size());

        // Iterate over the list and merge phenotypic features for each pair
        for (String blendedDiseaseId : blendedDiseaseIds) {
            String[] parts = blendedDiseaseId.split("-", 2);
            String diseaseId1 = parts[0];
            String diseaseId2 = parts[1];
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
        LOGGER.info("BlendedDiseaseData ready: {} total entries ({} singletons + {} pairs).",
                blendedDiseaseFeaturesDict.size(), geneIdAssociatedDiseases.size(), blendedDiseaseIds.size());
    }

    Set<String> geneIdAssociatedDiseases(List<String> geneIds) {
        return this.plainDiseaseData.getDiseaseIds().stream()
                .filter(d -> geneIds.stream().anyMatch(geneId -> this.plainDiseaseData.getDiseaseGeneIds(d).contains(geneId)))
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
