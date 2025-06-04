package com.github.p2gx.boqa.core;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class DiseaseDictTest {

    private static DiseaseDict testDiseaseDict;

    @BeforeAll
    static void setup() throws IOException {
        String annotationFile = "/Users/hansenp/development/BOQA/data/human-phenotype-ontology/latest_20250603/phenotype.hpoa";
        String ontologyFile = "/Users/hansenp/development/BOQA/data/human-phenotype-ontology/latest_20250603/hp.json";

        testDiseaseDict = new DiseaseDict(annotationFile, ontologyFile);

    }

    @Test
    void getDiseases() {
        int size = testDiseaseDict.getDiseases().size();
        assertEquals(8362, size);

        String omimId = "OMIM:604091";
        System.out.println(omimId);
        for (HpoDisease disease : testDiseaseDict.getDiseases()) {
            if (disease.id().toString().equals(omimId)) {
                System.out.println(disease.modesOfInheritance().toString());
                System.out.println(disease.annotationTermIdList());
                for (TermId termId : disease.annotationTermIdList()){
                    System.out.println(termId);
                    System.out.println(disease.getFrequencyOfTermInDisease(termId).get().numerator());
                }
            }
        }
    }

    @Test
    void getDiseaseDict() {
    }

    @Test
    void get_disease_features() {
    }
}