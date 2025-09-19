package com.github.p2gx.boqa.core.diseases;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


import java.io.*;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

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
    static void setup() throws IOException {
        try (InputStream is = new GZIPInputStream(DiseaseDataCmpParsePhenolIngestTest.class
                .getResourceAsStream("/com/github/p2gx/boqa/core/phenotype.v2025-05-06.hpoa.gz"))) {
            testDiseaseDictParse = new DiseaseDataParseIngest(is);
        }

        try (
            InputStream annotationStream = new GZIPInputStream(DiseaseDataCmpParsePhenolIngestTest.class
                    .getResourceAsStream("/com/github/p2gx/boqa/core/phenotype.v2025-05-06.hpoa.gz"));
            InputStream ontologyStream = new GZIPInputStream(DiseaseDataCmpParsePhenolIngestTest.class
                    .getResourceAsStream("/com/github/p2gx/boqa/core/hp.v2025-05-06.json.gz"))
        ) {
            testDiseaseDictPhenol = new DiseaseDataPhenolIngest(ontologyStream, annotationStream);
        }
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
            System.out.println("------------------------------");
            System.out.println(diseaseId);
            Set<String> includedTermsParse = testDiseaseDictParse.getObservedDiseaseFeatures(diseaseId);
            Set<String> includedTermsPhenol = testDiseaseDictPhenol.getObservedDiseaseFeatures(diseaseId);
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