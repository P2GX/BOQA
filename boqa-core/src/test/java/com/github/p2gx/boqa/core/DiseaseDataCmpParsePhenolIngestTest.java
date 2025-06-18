package com.github.p2gx.boqa.core;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Comparison of the two implementations of the DiseaseData interface,
 * namely DiseaseDataParseIngest and DiseaseDataPhenolIngest.
 * <p>
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 */
class DiseaseDataCmpParsePhenolIngestTest {

    private static DiseaseDataParseIngest testDiseaseDictParse;
    private static DiseaseDataPhenolIngest testDiseaseDictPhenol;

    @BeforeAll
    static void setup() throws IOException, URISyntaxException {
        ClassLoader classLoader = DiseaseDataPhenolIngest.class.getClassLoader();
        URL resourceUrl = classLoader.getResource("data/testDiseaseDict/hpo_v2025-05-06.zip");
        if (resourceUrl == null) {
            throw new IllegalArgumentException("Resource not found");
        }
        String HpoZipArchive = Paths.get(resourceUrl.toURI()).toFile().toString();
        resourceUrl = classLoader.getResource("data/testDiseaseDict");
        if (resourceUrl == null) {
            throw new IllegalArgumentException("Resource not found");
        }
        String destinationDirectory = Paths.get(resourceUrl.toURI()).toFile().toString();
        try {
            new ZipFile(HpoZipArchive).extractAll(destinationDirectory);
        } catch (ZipException e) {
            e.printStackTrace();
        }
        String annotationFile = destinationDirectory + "/phenotype.hpoa";
        String ontologyFile = destinationDirectory + "/hp.json";
        String diseaseGeneFile = destinationDirectory + "/genes_to_disease.txt";
        System.out.println(destinationDirectory);

        testDiseaseDictParse = new DiseaseDataParseIngest(annotationFile);
        testDiseaseDictPhenol = new DiseaseDataPhenolIngest(annotationFile, ontologyFile);
    }

    @Test
    void testCompareNumberOfDiseases() {
        /*
        Compare the number of diseases
         */

        int expectedSize = testDiseaseDictParse.size();
        int actualSize = testDiseaseDictPhenol.size();
        assertEquals(expectedSize, actualSize);
    }

    @Disabled
    @Test
    void testCompareIncludedAndExcludedFeatureTermsOfDiseases() {
        /*
        Compare included and excluded feature terms of diseases
         */

        // Get List of all disease keys
        Set<String> diseaseSet = testDiseaseDictParse.diseaseFeaturesDict.keySet();
        for (String diseaseId: diseaseSet) {
            System.out.println(diseaseId);
            Set<String> includedTermsParse = testDiseaseDictParse.getIncludedDiseaseFeatures(diseaseId);
            Set<String> includedTermsPhenol = testDiseaseDictPhenol.getIncludedDiseaseFeatures(diseaseId);
            Set<String> excludedTermsParse = testDiseaseDictParse.getExcludedDiseaseFeatures(diseaseId);
            Set<String> excludedTermsPhenol = testDiseaseDictPhenol.getExcludedDiseaseFeatures(diseaseId);
            if (!includedTermsParse.equals(includedTermsPhenol) | !excludedTermsParse.equals(excludedTermsPhenol)) {
                System.out.println("Failed!");
                Set<String> differenceSet = includedTermsParse.stream()
                        .filter(val -> !includedTermsPhenol.contains(val))
                        .collect(Collectors.toSet());
                System.out.println("Included Parse Only: " + differenceSet);
                differenceSet = includedTermsPhenol.stream()
                        .filter(val -> !includedTermsParse.contains(val))
                        .collect(Collectors.toSet());
                System.out.println("Included Phenol Only: " + differenceSet);
                differenceSet = excludedTermsParse.stream()
                        .filter(val -> !excludedTermsPhenol.contains(val))
                        .collect(Collectors.toSet());
                System.out.println("Excluded Parse Only: " + differenceSet);
                differenceSet = excludedTermsPhenol.stream()
                        .filter(val -> !excludedTermsParse.contains(val))
                        .collect(Collectors.toSet());
                System.out.println("Excluded Phenol Only: " + differenceSet);

            } else {
                System.out.println("Success!");
            }

        }
    }
}