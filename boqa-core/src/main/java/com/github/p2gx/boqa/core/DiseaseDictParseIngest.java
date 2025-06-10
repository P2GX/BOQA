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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DiseaseDictParseIngest implements DiseaseDict{
    private static final Logger LOGGER = LoggerFactory.getLogger(DiseaseDictParseIngest.class);
    String phenotypeAnnotationFile;
    List<String> validDatabaseList;
    List<String> hpoFreqTermList;
    List<String> excludedHpoFreqTermList;
    HashMap<String, HashMap<String, Set<String>>> diseaseFeaturesDict;

    public DiseaseDictParseIngest(String phenotypeAnnotationFile) {

        // Source file
        this.phenotypeAnnotationFile = phenotypeAnnotationFile;

        //this.validDatabaseList = List.of("OMIM", "ORPHA", "DECIPHER");
        this.validDatabaseList = List.of("OMIM");

        // HPO frequency terms
        this.hpoFreqTermList = List.of("HP:0040280", "HP:0040281", "HP:0040282", "HP:0040283", "HP:0040284", "HP:0040285");
        this.excludedHpoFreqTermList = List.of("HP:0040285");

        // Create dictionary by parsing phenotype.hpoa
        this.diseaseFeaturesDict = ingest(phenotypeAnnotationFile);
    }

    public HashMap<String, HashMap<String, Set<String>>> ingest(String phenotypeAnnotationFile) {
        HashMap<String, HashMap<String, Set<String>>> diseaseFeaturesDict = new HashMap<String, HashMap<String, Set<String>>>();
        // Open HPOA file phenotype.hpoa
        try {
            File myObj = new File(phenotypeAnnotationFile);
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String line = myReader.nextLine();
                // Skip header lines
                if (line.startsWith("#") | line.startsWith("database_id")) {
                    continue;
                }
                String[] fields = line.split("\t");
                if (fields.length != 12) {
                    System.out.println("ERROR: Row does not have 12 fields!");
                    break;
                }
                String disease_id = fields[0];
                String qualifier = fields[2];
                String hpo_id = fields[3];
                String frequency = fields[7];
                String database_tag = disease_id.split(":")[0];
                if (!validDatabaseList.contains(database_tag)) {
                    // Annotations can be restricted to individual databases from OMIM, ORPHA and DECIPHER.
                    continue;
                }
                String HpoFreqTerm = this.freqStringToHpoTerm(frequency);
                if (qualifier.equals("NOT")) {
                    HpoFreqTerm = "HP:0040285";
                }
                if (!diseaseFeaturesDict.containsKey(disease_id)) {

                    diseaseFeaturesDict.putIfAbsent(disease_id, new HashMap<>());
                    diseaseFeaturesDict.get(disease_id).put("E", new HashSet<>());
                    diseaseFeaturesDict.get(disease_id).put("I", new HashSet<>());
                }
                if (this.excludedHpoFreqTermList.contains(HpoFreqTerm)) {
                    // Term is excluded
                    diseaseFeaturesDict.get(disease_id).get("E").add(hpo_id);
                } else {
                    // Term is included
                    diseaseFeaturesDict.get(disease_id).get("I").add(hpo_id);
                }
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return diseaseFeaturesDict;
    }

    public double freqStringToFloat(String freqString) {
        /*
        Given an HPOA frequency string like "1/4", "20%" or “2.5”, return a float like 0.2, 0.205 or 0.25.
        */
        double freqFloat;
        if (Pattern.matches("\\d{1,}/\\d{1,}", freqString)) {
            // E.g., 1/4
            double numerator = Integer.parseInt(freqString.split("/")[0]);
            double denominator = Integer.parseInt(freqString.split("/")[1]);
            freqFloat = numerator/denominator;
        } else if (Pattern.matches("\\d{1,3}%", freqString)) {
            // E.g., 20%
            double percentage = Double.parseDouble(freqString.split("%")[0]);
            freqFloat = percentage/100;
        } else if (Pattern.matches("\\d{1,3}\\.\\d{1,20}%$", freqString)) {
            // E.g., 2.5%
            double percentage = Double.parseDouble(freqString.split("%")[0]);
            freqFloat = percentage/100;
        } else if (freqString.equals("")) {
            // If no frequency is given, we assume a frequency of 1
            freqFloat = 1.0;
        } else {
            freqFloat = -1.0;
        }
        // Check whether frequency is between 0 and 1
        if (freqFloat < 0 | 1 < freqFloat) {
            freqFloat = -1.0;
        }
        if (freqFloat == -1) {
            System.out.println("ERROR: Invalid frequency string \"" + freqString + "\" from HPOA!");
        }
        return freqFloat;
    }

    public String freqStringToHpoTerm(String freqString) {
        /*
        Given an HPOA frequency string like “1/4”, “20%” or “2.5”, return one of the corresponding HPO frequency terms.
         */

        if (this.hpoFreqTermList.contains(freqString)) {
            // Frequency already specified as HPO term
            return freqString;
        } else if (freqString.equals("")) {
            // If no frequency is given, we assume a frequency of 1
            return "HP:0040280";
        } else {
            double freqFloat = this.freqStringToFloat(freqString);

            if (freqFloat == 1.0) {
                // Obligate
                return "HP:0040280";
            } else if (freqFloat >= 0.8 && freqFloat < 1.0) {
                // Very frequent
               return "HP:0040281";
            } else if (freqFloat >= 0.3 && freqFloat < 0.8) {
                // Frequent
                return "HP:0040282";
            } else if (freqFloat >= 0.05 && freqFloat < 0.3) {
                // Occasional
                return "HP:0040283";
            } else if (freqFloat >= 0.01 && freqFloat < 0.05) {
                // Very rare
                return "HP:0040284";
            } else {
                // Excluded
                return "HP:0040285";
            }
        }
    }

    @Override
    public Set<String> getIncludedDiseaseFeatures(String omimId){
        return this.diseaseFeaturesDict.get(omimId).get("I");
    }

    @Override
    public Set<String> getExcludedDiseaseFeatures(String omimId){
        return this.diseaseFeaturesDict.get(omimId).get("E");
    }
}
