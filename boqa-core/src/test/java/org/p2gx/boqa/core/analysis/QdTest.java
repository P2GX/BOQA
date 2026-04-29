package org.p2gx.boqa.core.analysis;

import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.p2gx.boqa.core.DiseaseData;
import org.p2gx.boqa.core.TestBase;
import org.p2gx.boqa.core.algorithm.AlgorithmParameters;
import org.p2gx.boqa.core.algorithm.BoqaCounts;
import org.p2gx.boqa.core.algorithm.BoqaSetCounter;
import org.p2gx.boqa.core.diseases.DiseaseDataPhenolIngest;
import org.p2gx.boqa.core.diseases.DiseaseMerger;
import org.p2gx.boqa.core.patient.PhenopacketData;
import org.p2gx.boqa.core.patient.PhenopacketReader;
import org.phenopackets.schema.v2.Phenopacket;
import org.phenopackets.schema.v2.core.Disease;
import org.phenopackets.schema.v2.core.OntologyClass;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class QdTest extends TestBase  {

    private Double alpha = 5.241914347119568E-05;
    private Double beta =.9;

    @Test
    public void ingest() throws IOException {
        var ppktPath = Path.of("/Users/robin/GIT/mgd-ppkt/phenopackets/PMID_37501760_proband.json");
        var ppkt = PhenopacketReader.readPhenopacket(ppktPath);
        var ppktData = new PhenopacketData(ppkt, hpo());
        Set<String> relevantIds = ppkt.getDiseasesList()
                .stream()
                .map(Disease::getTerm)
                .map(OntologyClass::getId).collect(Collectors.toSet());
        assertEquals(2, relevantIds.size());
        List<HpoDisease> diseaseList = hpoDiseases().stream().filter(d -> relevantIds.contains(d.id().getValue())).toList();
        List<HpoDisease> mergedList = new ArrayList<>(diseaseList);
        HpoDisease merged = DiseaseMerger.merge(diseaseList);
        mergedList.add(merged);
        String version = "melded version";
        HpoDiseases filtered =  HpoDiseases.of(version, mergedList);
        assertEquals(3, mergedList.size());
        DiseaseData diseaseData = DiseaseDataPhenolIngest.of(hpo(), filtered);
        BoqaSetCounter counter = new BoqaSetCounter(diseaseData, hpo());
        for (String did : counter.getDiseaseIds()) {
            BoqaCounts boqaCounts = counter.computeBoqaCounts(did, ppktData);
            System.out.println(boqaCounts);
        }
        AlgorithmParameters params = AlgorithmParameters.create(alpha, beta);
        BoqaAnalysisResult result = BoqaPatientAnalyzer.computeBoqaResults(
                ppktData, counter, 100, params);
        System.out.println(result);
        for (var x : result.boqaResults()) {
            System.out.println(x);
        }
    }

}
