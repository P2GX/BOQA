package org.p2gx.boqa.core.diseases;

import org.monarchinitiative.phenol.annotations.base.temporal.TemporalInterval;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseaseAnnotation;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.schema.v1.core.PhenotypicFeature;

import java.util.*;
import java.util.stream.Stream;

public class DiseaseMerger {


    public static HpoDisease merge(HpoDisease diseaseA, HpoDisease diseaseB) {
        TermId mergedId = TermId.of(diseaseA.id().getValue() + "-" + diseaseB.id().getValue());
        String mergedName = String.format("%s - %s", diseaseA.diseaseName(), diseaseB.diseaseName());
        Optional<TemporalInterval> mergedOnset = diseaseA.diseaseOnset()
                .flatMap(a -> diseaseB.diseaseOnset().map(a::intersection))
                .or(diseaseA::diseaseOnset)
                .or(diseaseB::diseaseOnset);
        List<HpoDiseaseAnnotation> annotations = Stream.concat(
                diseaseA.presentAnnotationsStream(),
                diseaseB.presentAnnotationsStream()
        ).toList();
        List<TermId> moiList = Stream.concat(
                diseaseA.modesOfInheritance().stream(),
                diseaseB.modesOfInheritance().stream()
        ).distinct().toList();
        return HpoDisease.of(mergedId, mergedName, mergedOnset.get(), annotations, moiList);
    }


    public static HpoDisease merge(List<HpoDisease> diseaseList) throws PhenolRuntimeException {
        if (diseaseList.size() != 2) {
            throw new PhenolRuntimeException(String.format("Need to pass list of two HpoDisease objects but we got %d objects.", diseaseList.size()));
        }
        return DiseaseMerger.merge(diseaseList.get(0), diseaseList.get(1));
    }


}
