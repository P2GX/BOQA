package com.github.p2gx.boqa.core;

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

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
Class that implements the DiseaseDict interface by parsing disease annotations from HPOA files using Phenol.
 * <p>
 * TODO: Add disease genes, add terms for clinical modifier, find out what the term salvage negated frequencies means.
 * <p>
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 */
public class DiseaseDataPhenolIngest implements DiseaseData {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiseaseDataPhenolIngest.class);
    private static final int cohortSize = 100; // Imaginary cohort size using phenol to convert HPO frequency terms to ratios
    HpoDiseases diseases; // Temporarily needed to explore Phenols HpoDiseases, as there is no documentation
    HashMap<String, HashMap<String, Set<String>>> diseaseFeaturesDict;

    /*
    Constructor call with defaults
    */
    public DiseaseDataPhenolIngest(String phenotypeAnnotationFile, String ontologyFile) throws IOException {
        this(phenotypeAnnotationFile, ontologyFile, List.of("OMIM"));
    }

    public DiseaseDataPhenolIngest(String phenotypeAnnotationFile,
                                   String ontologyFile,
                                   List<String> validDatabaseList) // Valid databases are "OMIM", "ORPHA", and "DECIPHER"
            throws IOException{

        LOGGER.info("Ingesting HPOA file 'phenotype.hpoa' using Phenol ...");

        // Create dictionary using Phenol
        this.diseaseFeaturesDict = phenolIngest(ontologyFile, phenotypeAnnotationFile, validDatabaseList);
    }

    private HpoDiseases getPhenolHpoDiseases(String ontologyFile, String phenotypeAnnotationFile, List<String> validDatabaseList) throws IOException {
        /*
        Code required to get a kind of list of HpoDisease objects in Phenol from the HPOA file phenotype.hpoa
        and the HP ontology in JSON format.
         */
        Ontology ontology = OntologyLoader.loadOntology(new File(ontologyFile));
        Set<DiseaseDatabase> DiseaseDatabaseSet = validDatabaseList.stream()
                .map(DiseaseDatabase::fromString)
                .collect(Collectors.toSet());
        HpoDiseaseLoaderOptions options = HpoDiseaseLoaderOptions.of(DiseaseDatabaseSet,false, cohortSize);
        HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(ontology, options);
        return loader.load(Paths.get(phenotypeAnnotationFile));
    }

    private HashMap<String, HashMap<String, Set<String>>> phenolIngest(String ontologyFile, String phenotypeAnnotationFile, List<String> validDatabaseList) throws IOException {
        /*
        Use phenol to construct a dictionary that contains, for each disease, associated features and explicitly
        non-associated features.
         */
        HashMap<String, HashMap<String, Set<String>>> diseaseFeaturesDict = new HashMap<>();
        this.diseases = getPhenolHpoDiseases(ontologyFile, phenotypeAnnotationFile, validDatabaseList);
        for (HpoDisease disease : this.diseases) {

            // Included
            Set<String> includedTerms = disease.annotationTermIdList().stream()
                    .filter(termId -> disease.getFrequencyOfTermInDisease(termId).get().numerator() != 0)
                    .map(TermId::toString)
                    .collect(Collectors.toSet());
            Set<String> moi_terms = disease.modesOfInheritance().stream() // Include mode of inheritance
                    .map(TermId::toString)
                    .collect(Collectors.toSet());
            includedTerms.addAll(moi_terms);
            HashMap<String, Set<String>> iTerms = new HashMap<>();
            iTerms.put("I", includedTerms);
            diseaseFeaturesDict.putIfAbsent(disease.id().toString(), iTerms);

            // Excluded
            Set<String> excludedTerms = disease.annotationTermIdList().stream()
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
    public Set<String> getIncludedDiseaseFeatures(String diseaseId) {
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