package com.github.p2gx.boqa.core;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.annotations.base.Ratio;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.ZipFile;

class DiseaseDictTest {

    private static DiseaseDictPhenolIngest testDiseaseDict;

    @BeforeAll
    static void setup() throws IOException {
        ClassLoader classLoader = DiseaseDictPhenolIngest.class.getClassLoader();
        String annotationFile = classLoader.getResource("data/testDiseaseDict/phenotype_sample.hpoa").getFile();
        String ontologyZipFile = classLoader.getResource("data/testDiseaseDict/hp_v2025-05-06.json.zip").getFile();
        //String ontologyFile = classLoader.getResource("/Users/hansenp/development/BOQA/data/human-phenotype-ontology/v2025-05-06/hp.json").getFile();

        //String ontologyFile = "/Users/hansenp/development/BOQA/data/human-phenotype-ontology/latest_20250603/hp.json";
        testDiseaseDict = new DiseaseDictPhenolIngest(annotationFile, "/Users/hansenp/development/BOQA/data/human-phenotype-ontology/v2025-05-06/hp.json");
    }

    /*
    Exploration of the parsing of the file phenotype.hpoa by Phenol
     */

//    @Test
//    void getDiseases_onset() {
//        String omimId = "OMIM:618117";
//        System.out.println(omimId);
//        // OVARIAN DYSGENESIS 7; ODG7
//        // Juvenile onset (HP:0003621) is an annotated disease feature
//        HpoDisease disease = testDiseaseDict.getDiseases().diseaseById().get(TermId.of(omimId));
//        System.out.println("List of features: " + disease.annotationTermIdList());
//        System.out.println("HP:0003621 included in list: " + disease.annotationTermIdList().toString().contains("HP:0003621"));
//        System.out.println("Content of disease.diseaseOnset(): " + disease.diseaseOnset().toString());
//        // Juvenile onset (HP:0003621) is not in the list of annotated features
//        // Juvenile onset (HP:0003621) is a Clinical Modifier in HPO
//        // Description of Juvenile onset: Onset of signs or symptoms of disease between the age of 5 and 15 years.
//        // Content of disease.diseaseOnset(): 1827 to 5844 days, corresponds to 5 to 16 years.
//    }

//    @Test
//    void getDiseases_inheritance() {
//        String omimId = "OMIM:618117";
//        System.out.println(omimId);
//        // OVARIAN DYSGENESIS 7; ODG7
//        // Autosomal recessive inheritance (HP:0000007) is an annotated disease feature
//        HpoDisease disease = testDiseaseDict.getDiseases().diseaseById().get(TermId.of(omimId));
//        System.out.println("List of features: " + disease.annotationTermIdList());
//        System.out.println("HP:0000007 included in list: " + disease.annotationTermIdList().toString().contains("HP:0000007"));
//        System.out.println("Content of disease.modesOfInheritance(): " + disease.modesOfInheritance());
//        // Autosomal recessive inheritance (HP:0000007) is not in the list of annotated features
//        // Autosomal recessive inheritance (HP:0000007) is a Mode of Inheritance in HPO
//    }

//    @Test
//    void getDiseases_age_of_death() {
//        String omimId = "OMIM:617350";
//        System.out.println(omimId);
//        // DEVELOPMENTAL AND EPILEPTIC ENCEPHALOPATHY 52; DEE52
//        // Death in childhood (HP:0003819) is an annotated disease feature
//        HpoDisease disease = testDiseaseDict.getDiseases().diseaseById().get(TermId.of(omimId));
//        System.out.println("List of features: " + disease.annotationTermIdList());
//        System.out.println("HP:0003819 included in list: " + disease.annotationTermIdList().toString().contains("HP:0003819"));
//        // Death in childhood (HP:0003819) is not in the list of annotated features
//        // Death in childhood (HP:0003819) is a Clinical Modifier in HPO
//    }

//    @Test
//    void getDiseases_frequency_not_available() {
//        String omimId = "OMIM:609153";
//        // Frequencies of all included terms not available
//        // One term (HP:0001878) is excluded with a frequency of 0/2
//        System.out.println(omimId);
//        HpoDisease disease = testDiseaseDict.getDiseases().diseaseById().get(TermId.of(omimId));
//        for (TermId term : disease.annotationTermIdList()) {
//            Ratio ratio = disease.getFrequencyOfTermInDisease(term).get();
//            System.out.println(term.toString() + ": " + ratio);
//        }
//        // For the term with a frequency of 0/2 is converted to 0/2 (correct).
//        // All other terms are converted to 1/1 (correct).
//    }

    @Test
    void getDiseases_frequencies_as_percentages() {
        String omimId = "OMIM:165500";
        // In the file phenotype.hpoa the frequency of HP:0000486 is given as 10% and of HP:0000666 as 5%.
        System.out.println(omimId);
        HpoDisease disease = testDiseaseDict.getDiseases().diseaseById().get(TermId.of(omimId));
        for (TermId term : disease.annotationTermIdList()) {
            if (term.toString().equals("HP:0000486") | term.toString().equals("HP:0000666")) {
                Ratio ratio = disease.getFrequencyOfTermInDisease(term).get();
                System.out.println(term + ": " + ratio);
            }
        }
        // The term HP:0000486 with 10% is converted to 1/5, which corresponds to 20% (incorrect).
        // The term HP:0000486 with 5% is converted to 0/5, which corresponds to 0% (incorrect).
    }

//    @Test
//    void getDiseases_frequencies_as_percentages_with_decimals() {
//        String omimId = "OMIM:609939";
//        // In the file phenotype.hpoa the frequency of HP:0000077 is given as 32.3% and of HP:0000992 as 76.3%.
//        System.out.println(omimId);
//        HpoDisease disease = testDiseaseDict.getDiseases().diseaseById().get(TermId.of(omimId));
//        List<String> relevantTermIds = List.of("HP:0000077", "HP:0000992", "HP:0000707");
//        for (TermId term : disease.annotationTermIdList()) {
//            if (relevantTermIds.contains(term.toString())) {
//                Ratio ratio = disease.getFrequencyOfTermInDisease(term).get();
//                System.out.println(term + ": " + ratio);
//            }
//        }
//        // The term HP:0000077 with 32.3%  is converted to 2/5, which corresponds to 40%.
//        // The term HP:0000992 with 76.3% is converted to 4/5, which corresponds to 80%.
//    }

//    @Test
//    void getDiseases_frequencies_as_hpo_frequency_term() {
//        String omimId = "OMIM:276820";
//        // In the file phenotype.hpoa the frequency of "HP:0009815", "HP:0001773", "HP:0002980", "HP:0002827"
//        // are given as HP:0040280, HP:0040281, HP:0040282, HP:0040283.
//        System.out.println(omimId);
//        HpoDisease disease = testDiseaseDict.getDiseases().diseaseById().get(TermId.of(omimId));
//        List<String> relevantTermIds = List.of("HP:0009815", "HP:0001773", "HP:0002980", "HP:0002827");
//        for (TermId term : disease.annotationTermIdList()) {
//            if (relevantTermIds.contains(term.toString())) {
//                Ratio ratio = disease.getFrequencyOfTermInDisease(term).get();
//                System.out.println(term + ": " + ratio);
//            }
//        }
//        // The term HP:0009815 with HP:0040280 is converted to 5/5, which corresponds to 100% (correct).
//        // The term HP:0001773 with HP:0040281 is converted to 4/5, which corresponds to 80% (correct).
//        // The term HP:0002980 with HP:0040282 is converted to 3/5, which corresponds to 60% (correct).
//        // The term HP:0002827 with HP:0040283 is converted to 1/5, which corresponds to 20% (correct).
//    }

    /*
    Tests for parsing
     */


    @Test
    void getDiseaseDict() {
    }

//    @Test
//    void testGetIncludedDiseaseFeatures() {
//        String omimId = "OMIM:618117";
//        System.out.println(omimId);
//        System.out.println(testDiseaseDict.getIncludedDiseaseFeatures(omimId));
//    }
}