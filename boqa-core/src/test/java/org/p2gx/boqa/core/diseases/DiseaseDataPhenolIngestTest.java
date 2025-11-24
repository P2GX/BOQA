package org.p2gx.boqa.core.diseases;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.monarchinitiative.phenol.annotations.base.Ratio;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testing of DiseaseDataPhenolIngest, which implements DiseaseData.
 * <p>
 * Apart from that, there are exploratory tests that show some problems when parsing phenotype.hpoa with phenol.
 * These tests are disabled.
 * <p>
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 */
class DiseaseDataPhenolIngestTest {

    private static DiseaseDataPhenolIngest testDiseaseDict;

    @BeforeAll
    static void setup() throws IOException {
        try (
            InputStream ontologyStream = new GZIPInputStream(DiseaseDataCmpParsePhenolIngestTest.class
                    .getResourceAsStream("/org/p2gx/boqa/core/hp.v2025-05-06.json.gz"));
            InputStream annotationStream = new GZIPInputStream(DiseaseDataCmpParsePhenolIngestTest.class
                    .getResourceAsStream("/org/p2gx/boqa/core/phenotype.v2025-05-06.hpoa.gz"));
        ) {
            testDiseaseDict = new DiseaseDataPhenolIngest(ontologyStream, annotationStream);
        }
    }

    @Test
    void testInclusionAndExclusionOfTerms_1() {
        /*
        Tests inclusion and exclusion of terms based on annotation frequency.
        Only terms below phenotypic abnormalities (HP:0000118) should be included.

        Relevant rows and columns from phenotype.hpoa:

        OMIM:604091	HDL deficiency, familial, 1		HP:0003233	PMID:30503498	PCS		6/7			P -> included
        OMIM:604091	HDL deficiency, familial, 1		HP:0002155	PMID:30503498	PCS		0/7			P -> excluded
        OMIM:604091	HDL deficiency, familial, 1		HP:0001658	PMID:10431236	PCS					P -> included
        OMIM:604091	HDL deficiency, familial, 1		HP:0005181	PMID:7627690	PCS		2/4			P -> included
        OMIM:604091	HDL deficiency, familial, 1		HP:0000006	PMID:9888879	PCS					I -> excluded

         */
        String diseaseId = "OMIM:604091";
        //System.out.println(diseaseId);

        // Included
        Set<String> actualIncluded = testDiseaseDict.getObservedDiseaseFeatures(diseaseId);
        //System.out.println("Included: " + actualIncluded);
        Set<String> expectedIncluded = new HashSet<>();
        expectedIncluded.add("HP:0003233");
        expectedIncluded.add("HP:0001658");
        expectedIncluded.add("HP:0005181");
        assertEquals(expectedIncluded, actualIncluded);

        // Excluded
        Set<String> actualExcluded = testDiseaseDict.getExcludedDiseaseFeatures(diseaseId);
        //System.out.println("Excluded: " + actualExcluded);
        Set<String> expectedExcluded = new HashSet<>();
        expectedExcluded.add("HP:0002155");
        assertEquals(expectedExcluded, actualExcluded);
    }

    @Test
    void testInclusionAndExclusionOfTerms_2() {
        /*
        Tests inclusion and exclusion of terms based on annotation frequency.
        Only terms below phenotypic abnormalities (HP:0000118) should be included.

        Relevant rows and columns from phenotype.hpoa:

        OMIM:165500	Optic atrophy 1		HP:0003587	OMIM:165500	IEA					C -> not included
        OMIM:165500	Optic atrophy 1		HP:0000552	OMIM:165500	IEA					P -> included
        OMIM:165500	Optic atrophy 1		HP:0000486	OMIM:165500	PCS		10%			P -> included
        OMIM:165500	Optic atrophy 1		HP:0000650	OMIM:165500	IEA					P -> included
        OMIM:165500	Optic atrophy 1		HP:0000980	OMIM:165500	IEA					P -> included
        OMIM:165500	Optic atrophy 1		HP:0000590	PMID:20157015	PCS		48/104			P -> included
        OMIM:165500	Optic atrophy 1		HP:0001251	PMID:20157015	PCS		31/104			P -> included
        OMIM:165500	Optic atrophy 1		HP:0003829	OMIM:165500	IEA					I -> not included
        OMIM:165500	Optic atrophy 1		HP:0007663	OMIM:165500	TAS					P -> included
        OMIM:165500	Optic atrophy 1		HP:0000505	OMIM:165500	IEA					P -> included
        OMIM:165500	Optic atrophy 1		HP:0000648	OMIM:165500	IEA					P -> included
        OMIM:165500	Optic atrophy 1		HP:0000603	OMIM:165500	IEA					P -> included
        OMIM:165500	Optic atrophy 1		HP:0000576	OMIM:165500	IEA					P -> included
        OMIM:165500	Optic atrophy 1		HP:0000642	OMIM:165500	IEA					P -> included
        OMIM:165500	Optic atrophy 1		HP:0003701	PMID:20157015	PCS		37/104			P -> included
        OMIM:165500	Optic atrophy 1		HP:0000006	OMIM:165500	IEA					I -> not included
        OMIM:165500	Optic atrophy 1		HP:0000666	OMIM:165500	PCS		5%			P -> included

         */
        String diseaseId = "OMIM:165500";
        System.out.println(diseaseId);

        // Included
        Set<String> actualIncluded = testDiseaseDict.getObservedDiseaseFeatures(diseaseId);
        System.out.println("Included: " + actualIncluded);
        Set<String> expectedIncluded = new HashSet<>();
        expectedIncluded.add("HP:0000650");
        expectedIncluded.add("HP:0000980");
        expectedIncluded.add("HP:0001251");
        expectedIncluded.add("HP:0000590");
        expectedIncluded.add("HP:0000505");
        expectedIncluded.add("HP:0000648");
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
        Set<String> actualExcluded = testDiseaseDict.getExcludedDiseaseFeatures(diseaseId);
        System.out.println("Excluded: " + actualExcluded);
        Set<String> expectedExcluded = new HashSet<>();
        //expectedExcluded.add("HP:0000666"); // Bug in Phenol: Aspect is 'P' and frequency 5% - should be included!
        assertEquals(expectedExcluded, actualExcluded);
    }

    /*
    Exploration of the parsing the file phenotype.hpoa using Phenol
    */

    @Disabled
    @Test
    void getDiseases_frequencies_as_percentages() {
        String diseaseId = "OMIM:165500";
        // In the file phenotype.hpoa the frequency of HP:0000486 is given as 10% and of HP:0000666 as 5%.
        System.out.println(diseaseId);
        HpoDisease disease = testDiseaseDict.getDiseases().diseaseById().get(TermId.of(diseaseId));
        for (TermId term : disease.annotationTermIdList()) {
            if (term.toString().equals("HP:0000486") | term.toString().equals("HP:0000666")) {
                Ratio ratio = disease.getFrequencyOfTermInDisease(term).get();
                System.out.println(term + ": " + ratio);
            }
        }
        // The term HP:0000486 with 10% is converted to 1/5, which corresponds to 20% (incorrect).
        // The term HP:0000666 with 5% is converted to 0/5, which corresponds to 0% (incorrect).
        // Works when using default cohort size of 100 (default is 5).
    }

    @Disabled
    @Test
    void getDiseases_frequencies_as_percentages_with_decimals() {
        String diseaseId = "OMIM:609939";
        // In the file phenotype.hpoa the frequency of HP:0000077 is given as 32.3% and of HP:0000992 as 76.3%.
        System.out.println(diseaseId);
        HpoDisease disease = testDiseaseDict.getDiseases().diseaseById().get(TermId.of(diseaseId));
        List<String> relevantTermIds = List.of("HP:0000077", "HP:0000992");
        for (TermId term : disease.annotationTermIdList()) {
            if (relevantTermIds.contains(term.toString())) {
                Ratio ratio = disease.getFrequencyOfTermInDisease(term).get();
                System.out.println(term + ": " + ratio);
            }
        }
        // The term HP:0000077 with 32.3%  is converted to 2/5, which corresponds to 40% (imprecise).
        // The term HP:0000992 with 76.3% is converted to 4/5, which corresponds to 80% (imprecise).
        // More precise with default cohort size of 100 (default is 5).
    }

    @Disabled
    @Test
    void getDiseases_frequencies_as_hpo_frequency_term() {
        String diseaseId = "OMIM:276820";
        // In the file phenotype.hpoa the frequency of "HP:0009815", "HP:0001773", "HP:0002980", "HP:0002827"
        // are given as HP:0040280, HP:0040281, HP:0040282, HP:0040283.
        System.out.println(diseaseId);
        HpoDisease disease = testDiseaseDict.getDiseases().diseaseById().get(TermId.of(diseaseId));
        List<String> relevantTermIds = List.of("HP:0009815", "HP:0001773", "HP:0002980", "HP:0002827");
        for (TermId term : disease.annotationTermIdList()) {
            if (relevantTermIds.contains(term.toString())) {
                Ratio ratio = disease.getFrequencyOfTermInDisease(term).get();
                System.out.println(term + ": " + ratio);
            }
        }
        // The term HP:0009815 with HP:0040280 is converted to 5/5, which corresponds to 100% (within permissible range).
        // The term HP:0001773 with HP:0040281 is converted to 4/5, which corresponds to 80% (within permissible range).
        // The term HP:0002980 with HP:0040282 is converted to 3/5, which corresponds to 60% (within permissible range).
        // The term HP:0002827 with HP:0040283 is converted to 1/5, which corresponds to 20% (within permissible range).
    }

    @Disabled
    @Test
    void getDiseases_frequencies_as_hpo_frequency_term_2() {
        String diseaseId = "OMIM:615286";
        // In the file phenotype.hpoa the frequency of "HP:0000718" and "HP:0000752"
        // are given as HP:0040284.
        System.out.println(diseaseId);
        HpoDisease disease = testDiseaseDict.getDiseases().diseaseById().get(TermId.of(diseaseId));
        List<String> relevantTermIds = List.of("HP:0000718", "HP:0000752");
        for (TermId term : disease.annotationTermIdList()) {
            if (relevantTermIds.contains(term.toString())) {
                Ratio ratio = disease.getFrequencyOfTermInDisease(term).get();
                System.out.println(term + ": " + ratio);
            }
        }
        // The term HP:0000718 with HP:0040284 is converted to 0/5, which corresponds to excluded (incorrect).
        // The term HP:0000752 with HP:0040284 is converted to 0/5, which corresponds to excluded (incorrect).
        // Works when using default cohort size of 100 (default is 5).
    }

    @Disabled
    @Test
    void getDiseases_onset() {
        String diseaseId = "OMIM:618117";
        System.out.println(diseaseId);
        // OVARIAN DYSGENESIS 7; ODG7
        // Juvenile onset (HP:0003621) is an annotated disease feature
        HpoDisease disease = testDiseaseDict.getDiseases().diseaseById().get(TermId.of(diseaseId));
        System.out.println("List of features: " + disease.annotationTermIdList());
        System.out.println("HP:0003621 included in list: " + disease.annotationTermIdList().toString().contains("HP:0003621"));
        System.out.println("Content of disease.diseaseOnset(): " + disease.diseaseOnset().toString());
        // Juvenile onset (HP:0003621) is not in the list of annotated features
        // Juvenile onset (HP:0003621) is a Clinical Modifier in HPO
        // Description of Juvenile onset: Onset of signs or symptoms of disease between the age of 5 and 15 years.
        // Content of disease.diseaseOnset(): 1827 to 5844 days, corresponds to 5 to 16 years.
        // What happens in Phenol with the Clinical Modifier Terms from phenotype.hpoa?
    }

    @Disabled
    @Test
    void getDiseases_age_of_death() {
        String diseaseId = "OMIM:617350";
        System.out.println(diseaseId);
        // DEVELOPMENTAL AND EPILEPTIC ENCEPHALOPATHY 52; DEE52
        // Death in childhood (HP:0003819) is an annotated disease feature
        HpoDisease disease = testDiseaseDict.getDiseases().diseaseById().get(TermId.of(diseaseId));
        System.out.println("List of features: " + disease.annotationTermIdList());
        System.out.println("HP:0003819 included in list: " + disease.annotationTermIdList().toString().contains("HP:0003819"));
        // Death in childhood (HP:0003819) is not in the list of annotated features
        // Death in childhood (HP:0003819) is a Clinical Modifier in HPO
        // What happens in Phenol with the Clinical Modifier Terms from phenotype.hpoa?
    }

    @Disabled
    @Test
    void getDiseases_ventricular_septal_hypertrophy() {
        String diseaseId = "OMIM:615248";
        System.out.println(diseaseId);
        // DEVELOPMENTAL AND EPILEPTIC ENCEPHALOPATHY 52; DEE52
        // Ventricular septal hypertrophy (HP:0005144) is an annotated disease feature
        HpoDisease disease = testDiseaseDict.getDiseases().diseaseById().get(TermId.of(diseaseId));
        System.out.println("List of features: " + disease.annotationTermIdList());
        System.out.println("HP:0005144 included in list: " + disease.annotationTermIdList().toString().contains("HP:0005144"));
        // Death in childhood (HP:0005144) is not in the list of annotated features
        // Death in childhood (HP:0005144) is a Phenotypic abnormality in HPO
    }
}