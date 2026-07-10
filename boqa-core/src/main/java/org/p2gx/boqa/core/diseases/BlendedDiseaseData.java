package org.p2gx.boqa.core.diseases;

import org.p2gx.boqa.core.DiseaseData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * This class implements an obvious approach to analyzing blended phenotypes using BOQA.
 *
 * <p>Given a set of anchor diseases, pair them with all other diseases according to a
 * pairing strategy. Create a BlendedDiseaseData object that returns the annotated HPO terms
 * for individual diseases and disease pairs, with the union of the terms from both diseases
 * being returned for disease pairs.
 * Use the created BlendedDiseaseData object for the BOQA analysis.</p>
 *
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 */
public class BlendedDiseaseData implements DiseaseData {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlendedDiseaseData.class);

    public enum PairingStrategy {
        /** Pairs each anchor disease with every disease not in the anchor set. */
        ANCHOR_VS_ALL,
        /** Pairs anchor diseases with each other, according to the supplied blending predicate. */
        ANCHOR_VS_ANCHOR
    }

    private final DiseaseData plainDiseaseData;
    HashMap<String, HashMap<String, Set<String>>> blendedDiseaseFeaturesDict;

    public BlendedDiseaseData(DiseaseData plainDiseaseData, Set<String> anchorDiseaseIds) {
        this(plainDiseaseData, anchorDiseaseIds, PairingStrategy.ANCHOR_VS_ALL, (d1, d2) -> true);
    }

    /**
     * Creates a BlendedDiseaseData object that combines the given anchor diseases
     * with other diseases according to the chosen pairing strategy.
     *
     * @param plainDiseaseData the underlying disease data source
     * @param anchorDiseaseIds the anchor disease IDs to pair
     * @param strategy the strategy used to form disease pairs
     * @param mayBlendAnchors decides whether two anchor diseases may be paired;
     *                        consulted only by {@code ANCHOR_VS_ANCHOR}
     */
    public BlendedDiseaseData(DiseaseData plainDiseaseData, Set<String> anchorDiseaseIds,
                              PairingStrategy strategy, BiPredicate<String, String> mayBlendAnchors) {
        this.blendedDiseaseFeaturesDict = new HashMap<>();
        this.plainDiseaseData = plainDiseaseData;
        LOGGER.info("Initializing BlendedDiseaseData...");
        LOGGER.info("Number of anchor diseases: {}", anchorDiseaseIds.size());
        LOGGER.info("Strategy: {}", strategy);

        // Add all anchor diseases as singletons
        for (String diseaseId : anchorDiseaseIds) {
            this.blendedDiseaseFeaturesDict.putIfAbsent(diseaseId, new HashMap<>());
            this.blendedDiseaseFeaturesDict.get(diseaseId).put("I", new HashSet<>());
            this.blendedDiseaseFeaturesDict.get(diseaseId).get("I").addAll(this.plainDiseaseData.getObservedDiseaseFeatures(diseaseId));
            this.blendedDiseaseFeaturesDict.get(diseaseId).put("E", new HashSet<>());
            this.blendedDiseaseFeaturesDict.get(diseaseId).get("E").addAll(this.plainDiseaseData.getExcludedDiseaseFeatures(diseaseId));
        }

        Set<String> allDiseases = this.plainDiseaseData.getDiseaseIds();
        List<String> blendedDiseaseIds = formDiseasePairs(strategy, anchorDiseaseIds, allDiseases, mayBlendAnchors);
        if (blendedDiseaseIds.isEmpty()) {
            LOGGER.warn("ANCHOR_VS_ANCHOR produced no pairs for anchor diseases {} — no anchor pair passed the blending predicate. Analysis will run on singletons only.", anchorDiseaseIds);
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
        }
        LOGGER.info("BlendedDiseaseData ready: {} total entries ({} singletons + {} pairs).",
                blendedDiseaseFeaturesDict.size(), anchorDiseaseIds.size(), blendedDiseaseIds.size());
    }

    private List<String> formDiseasePairs(PairingStrategy strategy, Set<String> anchorDiseases, Set<String> allDiseases,
                                          BiPredicate<String, String> mayBlendAnchors) {
        List<String> pairs = new ArrayList<>();
        switch (strategy) {
            case ANCHOR_VS_ALL -> {
                for (String diseaseId1 : anchorDiseases) {
                    for (String diseaseId2 : allDiseases) {
                        if (!anchorDiseases.contains(diseaseId2)) {
                            pairs.add(diseaseId1 + '-' + diseaseId2);
                        }
                    }
                }
            }
            case ANCHOR_VS_ANCHOR -> {
                List<String> anchorList = new ArrayList<>(anchorDiseases);
                for (int i = 0; i < anchorList.size(); i++) {
                    for (int j = i + 1; j < anchorList.size(); j++) {
                        if (mayBlendAnchors.test(anchorList.get(i), anchorList.get(j))) {
                            pairs.add(anchorList.get(i) + '-' + anchorList.get(j));
                        }
                    }
                }
            }
        }
        return pairs;
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
}
