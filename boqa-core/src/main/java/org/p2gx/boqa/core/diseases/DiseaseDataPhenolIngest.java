package org.p2gx.boqa.core.diseases;

import org.p2gx.boqa.core.DiseaseData;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that implements the {@code DiseaseData} interface by parsing disease annotations
 * from HPOA files using Phenol.
 *
 * <p><strong>Rules for defining observed and excluded terms of diseases:</strong></p>
 * <ol>
 *     <li>Only terms below <i>Phenotypic Abnormality</i> ({@code HP:0000118}) are taken into account.</li>
 *     <li>Terms associated with a given disease are considered <i>excluded</i> if they have a
 *         frequency of {@code 0}; otherwise, they are defined as <i>observed</i>.</li>
 * </ol>
 *
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 */
public class DiseaseDataPhenolIngest implements DiseaseData {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiseaseDataPhenolIngest.class);
    private static final int cohortSize = 100; // Imaginary cohort size using phenol to convert HPO frequency terms to ratios
    HpoDiseases diseases; // Temporarily needed to explore Phenols HpoDiseases, as there is no documentation
    HashMap<String, HashMap<String, Set<String>>> diseaseFeaturesDict;
    private static final TermId PHENOTYPIC_ABNORMALITY = TermId.of("HP:0000118");
    private final Ontology hpo;

    public static DiseaseDataPhenolIngest fromPaths(Path phenotypeAnnotationFile, Path ontologyFile) throws IOException {
        try (
                BufferedInputStream annotationsStream = new BufferedInputStream(Files.newInputStream(phenotypeAnnotationFile));
                BufferedInputStream ontologyStream = new BufferedInputStream(Files.newInputStream(ontologyFile))
        ) {
            return new DiseaseDataPhenolIngest(ontologyStream, annotationsStream);
        }
    }

    /*
    Constructor call with defaults
    */
    public DiseaseDataPhenolIngest(InputStream ontologyStream, InputStream annotationsStream) throws IOException {
        this(annotationsStream, ontologyStream, List.of("OMIM"));
    }

    public DiseaseDataPhenolIngest(InputStream annotationsStream,
                                   InputStream ontologyStream,
                                   List<String> validDatabaseList) // Valid databases are "OMIM", "ORPHA", and "DECIPHER"
            throws IOException{

        LOGGER.info("Ingesting HPOA file 'phenotype.hpoa' using Phenol ...");

        // Temporarily needed to explore HpoDiseases in test class because there is no adequate phenol documentation
        this.hpo = OntologyLoader.loadOntology(ontologyStream);
        this.diseases = getPhenolHpoDiseases(hpo, annotationsStream, validDatabaseList);


        // Create dictionary using Phenol
        this.diseaseFeaturesDict = phenolIngest();
    }

    private HpoDiseases getPhenolHpoDiseases(Ontology hpo, InputStream phenotypeAnnotations, List<String> validDatabaseList) throws IOException {
        /*
        Code required to get a kind of list of HpoDisease objects in Phenol from the HPOA file phenotype.hpoa
        and the HP ontology in JSON format.
         */
        Set<DiseaseDatabase> DiseaseDatabaseSet = validDatabaseList.stream()
                .map(DiseaseDatabase::fromString)
                .collect(Collectors.toSet());
        HpoDiseaseLoaderOptions options = HpoDiseaseLoaderOptions.of(DiseaseDatabaseSet,false, cohortSize);
        HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(hpo, options);
        return loader.load(phenotypeAnnotations);
    }

    private HashMap<String, HashMap<String, Set<String>>> phenolIngest() {
        /*
        Use phenol to construct a dictionary that contains, for each disease, associated features and explicitly
        non-associated features.
         */
        HashMap<String, HashMap<String, Set<String>>> diseaseFeaturesDict = new HashMap<>();

        System.out.println("*****************");

        // Filter for phenotypic abnormality terms
        Set<TermId> phenotypicAbnormalities = Set.copyOf(hpo.graph().getDescendantSet(PHENOTYPIC_ABNORMALITY));

        for (HpoDisease disease : this.diseases) {

            // Observed
            Set<String> observedTerms = disease.annotationTermIdList().stream()
                    .filter(phenotypicAbnormalities::contains)
                    .filter(termId -> disease.getFrequencyOfTermInDisease(termId).get().numerator() != 0)
                    .map(TermId::toString)
                    .collect(Collectors.toSet());
            HashMap<String, Set<String>> iTerms = new HashMap<>();
            iTerms.put("I", observedTerms);
            diseaseFeaturesDict.putIfAbsent(disease.id().toString(), iTerms);

            // Excluded
            Set<String> excludedTerms = disease.annotationTermIdList().stream()
                    .filter(phenotypicAbnormalities::contains)
                    .filter(termId -> disease.getFrequencyOfTermInDisease(termId).get().numerator() == 0)
                    .map(TermId::toString)
                    .collect(Collectors.toSet());
            HashMap<String, Set<String>> eTerms = new HashMap<>();
            iTerms.put("E", excludedTerms);
            diseaseFeaturesDict.putIfAbsent(disease.id().toString(), eTerms);
        }
        return diseaseFeaturesDict;
    }

    /**
     * Temporarily needed to explore Phenols HpoDiseases, as there is no documentation.
     */
    public HpoDiseases getDiseases() {
        return this.diseases;
    }

    /**
     Methods that implement the DiseaseDict interface
     */

    @Override
    public int size() {
        return this.diseaseFeaturesDict.size();
    }

    @Override
    public Set<String> getDiseaseIds() {
        return this.diseaseFeaturesDict.keySet();
    }

    @Override
    public Set<String> getObservedDiseaseFeatures(String diseaseId) {
        if (this.diseaseFeaturesDict.containsKey(diseaseId)) {
            return this.diseaseFeaturesDict.get(diseaseId).get("I");
        } else {
            throw new IllegalArgumentException("Disease ID \"" + diseaseId + "\" not found!");
        }
    }

    @Override
    public Set<String> getExcludedDiseaseFeatures(String diseaseId){
        if (this.diseaseFeaturesDict.containsKey(diseaseId)) {
            return this.diseaseFeaturesDict.get(diseaseId).get("E");
        } else {
            throw new IllegalArgumentException("Disease ID \"" + diseaseId + "\" not found!");
        }
    }

    @Override
    public Set<String> getDiseaseGeneIds(String diseaseId) {
        /*
        Not yet implemented.
         */
        return new HashSet<>();
    }

    @Override
    public Set<String> getDiseaseGeneSymbols(String diseaseId) {
        /*
        Not yet implemented.
         */
        return new HashSet<>();
    }
}
