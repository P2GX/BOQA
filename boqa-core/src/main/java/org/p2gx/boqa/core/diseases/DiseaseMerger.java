package org.p2gx.boqa.core.diseases;

import org.monarchinitiative.phenol.annotations.base.temporal.TemporalInterval;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseaseAnnotation;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @deprecated not used any more!
 */
public class DiseaseMerger {


    public static HpoDisease merge(HpoDisease diseaseA, HpoDisease diseaseB) {
        TermId mergedId = TermId.of(diseaseA.id().getValue() + "-" + diseaseB.id().getValue());
        String mergedName = String.format("%s - %s", diseaseA.diseaseName(), diseaseB.diseaseName());
        Optional<TemporalInterval> mergedOnset = diseaseA.diseaseOnset()
                .flatMap(a -> diseaseB.diseaseOnset().map(a::intersection))
                .or(diseaseA::diseaseOnset)
                .or(diseaseB::diseaseOnset);
        List<HpoDiseaseAnnotation> annotations = DiseaseMerger.ObservedAnnotations(diseaseA, diseaseB);
        List<TermId> moiList = Stream.concat(
                diseaseA.modesOfInheritance().stream(),
                diseaseB.modesOfInheritance().stream()
        ).distinct().toList();
        return HpoDisease.of(mergedId, mergedName, mergedOnset.get(), annotations, moiList);
    }

    /** Merge observed HPO terms for the disease but avoid duplicates.
     * We are not using frequencies or onset in the Boqa merged aplication,
     * and do not anticipate we ever will, since there is no real way of
     * calculating this from the data for individual diseases. Therefore
     * we just merge all observed HPO annotations*/
    private static List<HpoDiseaseAnnotation> ObservedAnnotations(
            HpoDisease diseaseA,
            HpoDisease diseaseB) {
        return Stream.concat(diseaseA.presentAnnotationsStream(), diseaseB.presentAnnotationsStream())
                .collect(Collectors.toMap(
                        HpoDiseaseAnnotation::id,
                        a -> a,
                        (existing, replacement) -> existing // Keep the first if IDs collide
                ))
                .values()
                .stream()
                .toList();
    }


    public static HpoDisease merge(List<HpoDisease> diseaseList) throws PhenolRuntimeException {
        if (diseaseList.size() != 2) {
            throw new PhenolRuntimeException(String.format("Need to pass list of two HpoDisease objects but we got %d objects.", diseaseList.size()));
        }
        return DiseaseMerger.merge(diseaseList.get(0), diseaseList.get(1));
    }


}
