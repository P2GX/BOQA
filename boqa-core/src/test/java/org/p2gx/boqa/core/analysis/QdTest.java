package org.p2gx.boqa.core.analysis;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.p2gx.boqa.core.DiseaseData;
import org.p2gx.boqa.core.TestBase;
import org.p2gx.boqa.core.diseases.DiseaseDataPhenolIngest;
import org.p2gx.boqa.core.patient.PhenopacketData;
import org.p2gx.boqa.core.patient.PhenopacketReader;
import org.phenopackets.schema.v2.Phenopacket;
import org.phenopackets.schema.v2.core.Disease;
import org.phenopackets.schema.v2.core.OntologyClass;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class QdTest extends TestBase  {



    public HpoDisease blend(List<HpoDisease> diseaseList) {
        assert diseaseList.size() == 2;

    }

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
        // Make melded HpoDisease
        String version = "melded version";
        HpoDiseases filtered =  HpoDiseases.of(version, diseaseList);
        assertEquals(2, diseaseList.size());
        // We have diseases A and B
        // 1. Create melded disease A+B
        // 2. Thebn we have tree diseases, create DiseaseData with tnem
        DiseaseData diseaseData = DiseaseDataPhenolIngest.of(hpo(), filtered);
        // 3. Then create counter
        // 4. Then compare scores for A vs. A+B and B vs A+B

    }

}
