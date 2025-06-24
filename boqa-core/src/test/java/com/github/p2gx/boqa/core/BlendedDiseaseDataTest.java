package com.github.p2gx.boqa.core;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BlendedDiseaseDataTest {

    //private static BlendedDiseaseData testBlendedDiseaseData;
    private static DiseaseDataParseIngest testDiseaseData;

    @BeforeAll
    static void setup() {
        ClassLoader classLoader = DiseaseDataParseIngest.class.getClassLoader();
        String HpoZipArchive = classLoader.getResource("data/testDiseaseDict/hpo_v2025-05-06.zip").getFile();
        String destinationDirectory = classLoader.getResource("data/testDiseaseDict").getPath();
        try {
            new ZipFile(HpoZipArchive).extractAll(destinationDirectory);
        } catch (ZipException e) {
            e.printStackTrace();
        }
        String annotationFile = destinationDirectory + "/phenotype.hpoa";
        String diseaseGeneFile = destinationDirectory + "/genes_to_disease.txt";

        testDiseaseData = new DiseaseDataParseIngest(annotationFile);
        testDiseaseData.addDiseaseGenes(diseaseGeneFile);
    }

    @Test
    void testGeneIdAssociatedDiseases() {
        BlendedDiseaseData testBlendedDiseaseData = new BlendedDiseaseData(testDiseaseData, "NCBIGene:392255");
        Set<String> geneIdAssociatedDiseasesExpected = Set.of("OMIM:617898", "OMIM:615360", "OMIM:613094", "OMIM:118100", "OMIM:613703");
        Set<String> geneIdAssociatedDiseasesActual = testBlendedDiseaseData.geneIdAssociatedDiseases("NCBIGene:392255");
        assertEquals(geneIdAssociatedDiseasesExpected, geneIdAssociatedDiseasesActual);
    }
}