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
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DiseaseDict {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiseaseDict.class);
    String phenotypeAnnotationFile;
    String ontologyFile;
    HpoDiseases diseases;
    HashMap<String, List<String>> diseaseDict;

    public DiseaseDict(String phenotypeAnnotationFile, String ontologyFile) throws IOException{
        this.phenotypeAnnotationFile = phenotypeAnnotationFile;
        this.ontologyFile = ontologyFile;
        Ontology ontology = OntologyLoader.loadOntology(new File(ontologyFile));
        HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(ontology, HpoDiseaseLoaderOptions.defaultOmim());
        this.diseases = loader.load(Paths.get(phenotypeAnnotationFile));

        this.diseaseDict = new HashMap<String, List<String>>();
        for (HpoDisease disease : diseases) {
            List<String> termIdList = disease.annotationTermIdList().stream()
                    .map(object -> Objects.toString(object, null))
                    .collect(Collectors.toList());
            this.diseaseDict.put(disease.id().toString(), termIdList);
        }
    }

    public HpoDiseases getDiseases() {
        return this.diseases;
    }

    public HashMap<String, List<String>> getDiseaseDict(){
        return this.diseaseDict;
    }

    public List<String> get_disease_features(String omimId){
        return this.diseaseDict.get(omimId);
    }
}
