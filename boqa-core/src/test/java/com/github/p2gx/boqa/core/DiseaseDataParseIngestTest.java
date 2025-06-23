package com.github.p2gx.boqa.core;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Set;

/**
 * Testing of DiseaseDataParseIngest, which implements DiseaseData.
 * <p>
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 */
class DiseaseDataParseIngestTest {

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
        //String ontologyFile = destinationDirectory + "/hp.json";
        String diseaseGeneFile = destinationDirectory + "/genes_to_disease.txt";

        testDiseaseData = new DiseaseDataParseIngest(annotationFile);
        testDiseaseData.addDiseaseGenes(diseaseGeneFile);
    }

    @Test
    void testGetDiseaseGeneIds() {
        String diseaseId = "OMIM:608232";
        Set<String> expectedIncluded = new HashSet<>();
        expectedIncluded.add("NCBIGene:613");
        expectedIncluded.add("NCBIGene:25");
        Set<String> actualIncluded = testDiseaseData.getDiseaseGeneIds(diseaseId);
        assertEquals(expectedIncluded, actualIncluded);
    }

    @Test
    void testGetDiseaseGeneSymbols() {
        String diseaseId = "OMIM:608232";
        Set<String> expectedIncluded = new HashSet<>();
        expectedIncluded.add("ABL1");
        expectedIncluded.add("BCR");
        Set<String> actualIncluded = testDiseaseData.getDiseaseGeneSymbols(diseaseId);
        assertEquals(expectedIncluded, actualIncluded);
    }

    @Test
    void testInclusionAndExclusionOfTerms_1() {
        /*
        Tests inclusion and exclusion of terms based on annotation frequency.

        Relevant rows and columns from phenotype.hpoa:

        OMIM:604091	HDL deficiency, familial, 1		HP:0003233	PMID:30503498	PCS		6/7			P -> included
        OMIM:604091	HDL deficiency, familial, 1		HP:0002155	PMID:30503498	PCS		0/7			P -> excluded
        OMIM:604091	HDL deficiency, familial, 1		HP:0001658	PMID:10431236	PCS					P -> included
        OMIM:604091	HDL deficiency, familial, 1		HP:0005181	PMID:7627690	PCS		2/4			P -> included
        OMIM:604091	HDL deficiency, familial, 1		HP:0000006	PMID:9888879	PCS					I -> included

         */
        String diseaseId = "OMIM:604091";
        System.out.println(diseaseId);

        // Included
        Set<String> actualIncluded = testDiseaseData.getIncludedDiseaseFeatures(diseaseId);
        System.out.println("Included: " + actualIncluded);
        Set<String> expectedIncluded = new HashSet<>();
        expectedIncluded.add("HP:0003233");
        expectedIncluded.add("HP:0000006");
        expectedIncluded.add("HP:0001658");
        expectedIncluded.add("HP:0005181");
        assertEquals(expectedIncluded, actualIncluded);

        // Excluded
        Set<String> actualExcluded = testDiseaseData.getExcludedDiseaseFeatures(diseaseId);
        System.out.println("Excluded: " + actualExcluded);
        Set<String> expectedExcluded = new HashSet<>();
        expectedExcluded.add("HP:0002155");
        assertEquals(expectedExcluded, actualExcluded);
    }

    @Test
    void testInclusionAndExclusionOfTerms_2() {
        /*
        Tests inclusion and exclusion of terms based on annotation frequency.

        Relevant rows and columns from phenotype.hpoa:

        OMIM:165500	Optic atrophy 1		HP:0003587	OMIM:165500	IEA					C -> included
        OMIM:165500	Optic atrophy 1		HP:0000552	OMIM:165500	IEA					P -> included
        OMIM:165500	Optic atrophy 1		HP:0000486	OMIM:165500	PCS		10%			P -> included
        OMIM:165500	Optic atrophy 1		HP:0000650	OMIM:165500	IEA					P -> included
        OMIM:165500	Optic atrophy 1		HP:0000980	OMIM:165500	IEA					P -> included
        OMIM:165500	Optic atrophy 1		HP:0000590	PMID:20157015	PCS		48/104			P -> included
        OMIM:165500	Optic atrophy 1		HP:0001251	PMID:20157015	PCS		31/104			P -> included
        OMIM:165500	Optic atrophy 1		HP:0003829	OMIM:165500	IEA					I -> included
        OMIM:165500	Optic atrophy 1		HP:0007663	OMIM:165500	TAS					P -> included
        OMIM:165500	Optic atrophy 1		HP:0000505	OMIM:165500	IEA					P -> included
        OMIM:165500	Optic atrophy 1		HP:0000648	OMIM:165500	IEA					P -> included
        OMIM:165500	Optic atrophy 1		HP:0000603	OMIM:165500	IEA					P -> included
        OMIM:165500	Optic atrophy 1		HP:0000576	OMIM:165500	IEA					P -> included
        OMIM:165500	Optic atrophy 1		HP:0000642	OMIM:165500	IEA					P -> included
        OMIM:165500	Optic atrophy 1		HP:0003701	PMID:20157015	PCS		37/104			P -> included
        OMIM:165500	Optic atrophy 1		HP:0000006	OMIM:165500	IEA					I -> included
        OMIM:165500	Optic atrophy 1		HP:0000666	OMIM:165500	PCS		5%			P -> included

         */
        String diseaseId = "OMIM:165500";
        System.out.println(diseaseId);

        // Included
        Set<String> actualIncluded = testDiseaseData.getIncludedDiseaseFeatures(diseaseId);
        System.out.println("Included: " + actualIncluded);
        Set<String> expectedIncluded = new HashSet<>();
        expectedIncluded.add("HP:0003587");
        expectedIncluded.add("HP:0000650");
        expectedIncluded.add("HP:0000980");
        expectedIncluded.add("HP:0001251");
        expectedIncluded.add("HP:0000590");
        expectedIncluded.add("HP:0003829");
        expectedIncluded.add("HP:0000505");
        expectedIncluded.add("HP:0000648");
        expectedIncluded.add("HP:0000006");
        expectedIncluded.add("HP:0000666");
        expectedIncluded.add("HP:0007663");
        expectedIncluded.add("HP:0000603");
        expectedIncluded.add("HP:0000552");
        expectedIncluded.add("HP:0000486");
        expectedIncluded.add("HP:0003701");
        expectedIncluded.add("HP:0000576");
        expectedIncluded.add("HP:0000642");
        assertEquals(expectedIncluded, actualIncluded);

        // Excluded
        Set<String> actualExcluded = testDiseaseData.getExcludedDiseaseFeatures(diseaseId);
        System.out.println("Excluded: " + actualExcluded);
        Set<String> expectedExcluded = new HashSet<>();
        assertEquals(expectedExcluded, actualExcluded);
    }

    @Test
    void testFreqStringToFloatPercentages() {

        double expectedFloat = 0.01;
        double actualFloat = testDiseaseData.freqStringToFloat("1%");
        assertEquals(expectedFloat, actualFloat);

        expectedFloat = 0.20;
        actualFloat = testDiseaseData.freqStringToFloat("20%");
        assertEquals(expectedFloat, actualFloat);

        expectedFloat = 1.00;
        actualFloat = testDiseaseData.freqStringToFloat("100%");
        assertEquals(expectedFloat, actualFloat);

        expectedFloat = -1.00;
        actualFloat = testDiseaseData.freqStringToFloat("1000%");
        assertEquals(expectedFloat, actualFloat);

    }

    @Test
    void testFreqStringToFloatPercentagesWithDecimals() {

        double expectedFloat = 0.012;
        double actualFloat = testDiseaseData.freqStringToFloat("1.2%");
        assertEquals(expectedFloat, actualFloat);

        expectedFloat = 0.20123456;
        actualFloat = testDiseaseData.freqStringToFloat("20.123456%");
        assertEquals(expectedFloat, actualFloat);

        expectedFloat = 1.00;
        actualFloat = testDiseaseData.freqStringToFloat("100.00%");
        assertEquals(expectedFloat, actualFloat);

        expectedFloat = -1.00;
        actualFloat = testDiseaseData.freqStringToFloat("100.01%");
        assertEquals(expectedFloat, actualFloat);

    }

    @Test
    void testFreqStringToFloatRatios() {

        double expectedFloat = 1.00;
        double actualFloat = testDiseaseData.freqStringToFloat("1/1");
        assertEquals(expectedFloat, actualFloat);

        expectedFloat = 0.50;
        actualFloat = testDiseaseData.freqStringToFloat("1/2");
        assertEquals(expectedFloat, actualFloat);

        expectedFloat = 0.00;
        actualFloat = testDiseaseData.freqStringToFloat("0/2");
        assertEquals(expectedFloat, actualFloat);

        expectedFloat = -1.00;
        actualFloat = testDiseaseData.freqStringToFloat("2/1");
        assertEquals(expectedFloat, actualFloat);

    }

    @Test
    void testFreqStringToHpoTermObligate() {

        // Percentage
        String expectedTerm = "HP:0040280";
        String actualTerm = testDiseaseData.freqStringToHpoTerm("100%");
        assertEquals(expectedTerm, actualTerm);

        // Percentage with decimals
        expectedTerm = "HP:0040280";
        actualTerm = testDiseaseData.freqStringToHpoTerm("100.00%");
        assertEquals(expectedTerm, actualTerm);

        // Ratio
        expectedTerm = "HP:0040280";
        actualTerm = testDiseaseData.freqStringToHpoTerm("1/1");
        assertEquals(expectedTerm, actualTerm);

        // Empty string
        expectedTerm = "HP:0040280";
        actualTerm = testDiseaseData.freqStringToHpoTerm("");
        assertEquals(expectedTerm, actualTerm);

    }

    @Test
    void testFreqStringToHpoTermVeryFrequent() {

        // Percentage with decimals
        String expectedTerm = "HP:0040281";
        String actualTerm = testDiseaseData.freqStringToHpoTerm("99.999999999%");
        assertEquals(expectedTerm, actualTerm);

        // Percentage
        expectedTerm = "HP:0040281";
        actualTerm = testDiseaseData.freqStringToHpoTerm("80%");
        assertEquals(expectedTerm, actualTerm);

        // Ratio
        expectedTerm = "HP:0040281";
        actualTerm = testDiseaseData.freqStringToHpoTerm("4/5");
        assertEquals(expectedTerm, actualTerm);

    }

    @Test
    void testFreqStringToHpoTermFrequent() {

        // Percentage with decimals
        String expectedTerm = "HP:0040282";
        String actualTerm = testDiseaseData.freqStringToHpoTerm("79.999999999%");
        assertEquals(expectedTerm, actualTerm);

        // Percentage
        expectedTerm = "HP:0040282";
        actualTerm = testDiseaseData.freqStringToHpoTerm("30%");
        assertEquals(expectedTerm, actualTerm);

        // Ratio
        expectedTerm = "HP:0040282";
        actualTerm = testDiseaseData.freqStringToHpoTerm("3/10");
        assertEquals(expectedTerm, actualTerm);

    }

    @Test
    void testFreqStringToHpoTermOccasional() {

        // Percentage with decimals
        String expectedTerm = "HP:0040283";
        String actualTerm = testDiseaseData.freqStringToHpoTerm("29.999999999%");
        assertEquals(expectedTerm, actualTerm);

        // Percentage
        expectedTerm = "HP:0040283";
        actualTerm = testDiseaseData.freqStringToHpoTerm("5%");
        assertEquals(expectedTerm, actualTerm);

        // Ratio
        expectedTerm = "HP:0040283";
        actualTerm = testDiseaseData.freqStringToHpoTerm("1/20");
        assertEquals(expectedTerm, actualTerm);

    }

    @Test
    void testFreqStringToHpoTermVeryRare() {

        // Percentage with decimals
        String expectedTerm = "HP:0040284";
        String actualTerm = testDiseaseData.freqStringToHpoTerm("4.999999999%");
        assertEquals(expectedTerm, actualTerm);

        // Percentage
        expectedTerm = "HP:0040284";
        actualTerm = testDiseaseData.freqStringToHpoTerm("1%");
        assertEquals(expectedTerm, actualTerm);

        // Ratio
        expectedTerm = "HP:0040284";
        actualTerm = testDiseaseData.freqStringToHpoTerm("1/100");
        assertEquals(expectedTerm, actualTerm);

    }

    @Test
    void testFreqStringToHpoTermExcluded() {

        // Percentage with decimals
        String expectedTerm = "HP:0040285";
        String actualTerm = testDiseaseData.freqStringToHpoTerm("0.999999999%");
        assertEquals(expectedTerm, actualTerm);

        // Percentage
        expectedTerm = "HP:0040285";
        actualTerm = testDiseaseData.freqStringToHpoTerm("0.1%");
        assertEquals(expectedTerm, actualTerm);

        // Ratio
        expectedTerm = "HP:0040285";
        actualTerm = testDiseaseData.freqStringToHpoTerm("1/1000");
        assertEquals(expectedTerm, actualTerm);

        expectedTerm = "HP:0040285";
        actualTerm = testDiseaseData.freqStringToHpoTerm("0/10");
        assertEquals(expectedTerm, actualTerm);

    }

    @Test
    void testNonExistentDiseaseId() {
        Throwable exception = assertThrows(
                IllegalArgumentException.class, () ->
                        testDiseaseData.getIncludedDiseaseFeatures("OMIM:INCULDED"));

        assertEquals("Disease ID \"OMIM:INCULDED\" not found!", exception.getMessage());

        exception = assertThrows(
                IllegalArgumentException.class, () ->
                        testDiseaseData.getExcludedDiseaseFeatures("OMIM:EXCLUDED"));
        assertEquals("Disease ID \"OMIM:EXCLUDED\" not found!", exception.getMessage());

        exception = assertThrows(
                IllegalArgumentException.class, () ->
                        testDiseaseData.getDiseaseGeneIds("getDiseaseGeneIds"));
        assertEquals("Disease ID \"getDiseaseGeneIds\" not found!", exception.getMessage());

        exception = assertThrows(
                IllegalArgumentException.class, () ->
                        testDiseaseData.getDiseaseGeneSymbols("getDiseaseGeneSymbols"));
        assertEquals("Disease ID \"getDiseaseGeneSymbols\" not found!", exception.getMessage());
    }
}
