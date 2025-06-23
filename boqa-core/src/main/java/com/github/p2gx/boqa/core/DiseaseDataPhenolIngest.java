package com.github.p2gx.boqa.core;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
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
Class that implements the DiseaseDict interface by parsing disease annotations from HPOA files using Phenol.
 * <p>
 * TODO: Add disease genes, add terms for clinical modifier, increase default cohort size in Phenol too 100.
 * <p>
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 */
public class DiseaseDataPhenolIngest implements DiseaseData {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiseaseDataPhenolIngest.class);
    List<String> validDatabaseList;
    HpoDiseases diseases;
    HashMap<String, HashMap<String, Set<String>>> diseaseFeaturesDict;

    public static DiseaseDataPhenolIngest fromPaths(Path phenotypeAnnotationFile, Path ontologyFile) throws IOException {
        try (
                BufferedInputStream annotationsStream = new BufferedInputStream(Files.newInputStream(phenotypeAnnotationFile));
                BufferedInputStream ontologyStream = new BufferedInputStream(Files.newInputStream(ontologyFile))
        ) {
            return new DiseaseDataPhenolIngest(ontologyStream, annotationsStream);
        }
    }

    public DiseaseDataPhenolIngest(InputStream ontologyStream, InputStream annotationsStream) throws IOException{
        // Temporarily needed to explore HpoDiseases in test class because there is no adequate phenol documentation
        this.diseases = getPhenolHpoDiseases(ontologyStream, annotationsStream);

        //this.validDatabaseList = List.of("OMIM", "ORPHA", "DECIPHER");
        this.validDatabaseList = List.of("OMIM");

        // Create dictionary using Phenol
        this.diseaseFeaturesDict = phenolIngest();
    }

    private HpoDiseases getPhenolHpoDiseases(InputStream ontologyStream, InputStream phenotypeAnnotations) throws IOException {
        /*
        Code required to get a kind of list of HpoDisease objects in Phenol from the HPOA file phenotype.hpoa
        and the HP ontology in JSON format.
         */
        Ontology ontology = OntologyLoader.loadOntology(ontologyStream);
        HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(ontology, HpoDiseaseLoaderOptions.defaultOmim());
        return loader.load(phenotypeAnnotations);
    }

    private HashMap<String, HashMap<String, Set<String>>> phenolIngest() {
        /*
        Use phenol to construct a dictionary that contains, for each disease, associated features and explicitly
        non-associated features.
         */
        HashMap<String, HashMap<String, Set<String>>> diseaseFeaturesDict = new HashMap<>();
        for (HpoDisease disease : diseases) {

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
    public Set<String> getIncludedDiseaseFeatures(String diseaseId){
        return this.diseaseFeaturesDict.get(diseaseId).get("I");
    }

    @Override
    public Set<String> getExcludedDiseaseFeatures(String diseaseId){
        return this.diseaseFeaturesDict.get(diseaseId).get("E");
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