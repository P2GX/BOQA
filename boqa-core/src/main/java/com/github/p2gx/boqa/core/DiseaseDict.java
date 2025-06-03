package com.github.p2gx.boqa.core;

import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.base.PhenolException;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseAnnotationParser;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

import static org.monarchinitiative.phenol.io.OntologyLoader.loadOntology;


public class DiseaseDict {

//    private static final Logger LOGGER = LoggerFactory.getLogger(DiseaseDict.class);
//    private Map<TermId, HpoDisease> diseaseMap;
//
//    public DiseaseDict(Path phenotypeAnnotationFile, Path ontologyFile){
//
//        Ontology hpo = loadOntology((InputStream) ontologyFile);
//        HpoDiseaseLoaderOptions options = HpoDiseaseLoaderOptions.defaultOptions();
//        HpoDiseases diseases = loadHpoDiseases(phenotypeAnnotationFile, hpo, options);
//
//        HpoDiseaseAnnotationParser annotationParser = new HpoDiseaseAnnotationParser(phenotypeAnnotationFile, ontologyFile);
//        try {
//            Map<TermId, HpoDisease> diseaseMap = annotationParser.parse();
//            if (!annotationParser.validParse()) {
//                int n = annotationParser.getErrors().size();
//                LOGGER.warn("Parse problems encountered with the annotation file at {}. Got {} errors",
//                        phenotypeAnnotationFile,n);
//            }
//            this.diseaseMap = diseaseMap; // or do something else with the data
//        } catch (PhenolException e) {
//            e.printStackTrace(); // or do something else
//        }
//}
}
