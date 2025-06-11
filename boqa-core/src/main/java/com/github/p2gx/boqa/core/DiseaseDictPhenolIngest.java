package com.github.p2gx.boqa.core;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class DiseaseDictPhenolIngest implements DiseaseDict{
    private static final Logger LOGGER = LoggerFactory.getLogger(DiseaseDictPhenolIngest.class);
    String phenotypeAnnotationFile;
    String ontologyFile;
    List<String> validDatabaseList;
    HpoDiseases diseases;
    HashMap<String, HashMap<String, Set<String>>> diseaseFeaturesDict;

    public DiseaseDictPhenolIngest(String phenotypeAnnotationFile, String ontologyFile) throws IOException{

        // Source files
        this.phenotypeAnnotationFile = phenotypeAnnotationFile;
        this.ontologyFile = ontologyFile;

        // Temporarily needed to explore HpoDiseases in test class because there is no adequate phenol documentation
        this.diseases = getPhenolHpoDiseases(ontologyFile, phenotypeAnnotationFile);

        //this.validDatabaseList = List.of("OMIM", "ORPHA", "DECIPHER");
        this.validDatabaseList = List.of("OMIM");

        // Create dictionary using Phenol
        this.diseaseFeaturesDict = phenolIngest(ontologyFile, phenotypeAnnotationFile);
    }

    private HpoDiseases getPhenolHpoDiseases(String ontologyFile, String phenotypeAnnotationFile) throws IOException {
        /*
        Code required to get a kind of list of HpoDisease objects in Phenol from the HPOA file phenotype.hpoa
        and the HP ontology in JSON format.
         */
        Ontology ontology = OntologyLoader.loadOntology(new File(ontologyFile));
        HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(ontology, HpoDiseaseLoaderOptions.defaultOmim());
        return loader.load(Paths.get(phenotypeAnnotationFile));
    }

    private HashMap<String, HashMap<String, Set<String>>> phenolIngest(String ontologyFile, String phenotypeAnnotationFile) throws IOException {
        /*
        Use phenol to construct a dictionary that contains, for each disease, associated features and explicitly
        non-associated features.
         */
        HashMap<String, HashMap<String, Set<String>>> diseaseFeaturesDict = new HashMap<>();
        diseases = getPhenolHpoDiseases(ontologyFile, phenotypeAnnotationFile);
        for (HpoDisease disease : diseases) {

            // Included
            Set<String> includedTerms = disease.annotationTermIdList().stream()
                    .filter(termId -> disease.getFrequencyOfTermInDisease(termId).get().numerator() != 0)
                    .map(termId -> termId.toString())
                    .collect(Collectors.toSet());
            Set<String> moi_terms = disease.modesOfInheritance().stream() // Include mode of inheritance
                    .map(termId -> termId.toString())
                    .collect(Collectors.toSet());
            includedTerms.addAll(moi_terms);
            HashMap<String, Set<String>> iTerms = new HashMap<>();
            iTerms.put("I", includedTerms);
            diseaseFeaturesDict.putIfAbsent(disease.id().toString(), iTerms);

            // Excluded
            Set<String> excludedTerms = disease.annotationTermIdList().stream()
                    .filter(termId -> disease.getFrequencyOfTermInDisease(termId).get().numerator() == 0)
                    .map(termId -> termId.toString())
                    .collect(Collectors.toSet());
            HashMap<String, Set<String>> eTerms = new HashMap<>();
            iTerms.put("E", excludedTerms);
            diseaseFeaturesDict.putIfAbsent(disease.id().toString(), eTerms);
        }
        return diseaseFeaturesDict;
    }

    public HpoDiseases getDiseases() {
        return this.diseases;
    }

    @Override
    public int size() {
        return this.diseaseFeaturesDict.size();
    }

    @Override
    public Set<String> getIncludedDiseaseFeatures(String omimId){
        return this.diseaseFeaturesDict.get(omimId).get("I");
    }

    @Override
    public Set<String> getExcludedDiseaseFeatures(String omimId){
        return this.diseaseFeaturesDict.get(omimId).get("E");
    }

    @Override
    public Set<String> getDiseaseGeneIds(String omimId) {
        /*
        Not yet implemented.
         */
        return new HashSet<>();
    }
}
