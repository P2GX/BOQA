package com.github.p2gx.boqa.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Class that implements the DiseaseData interface by parsing annotations directly from the HPOA files
 * phenotype.hpoa and genes_to_diseases.txt.
 * <p>
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 */
public class DiseaseDataParseIngest implements DiseaseData {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiseaseDataParseIngest.class);
    List<String> validDatabaseList; // Valid databases are "OMIM", "ORPHA", and "DECIPHER"
    List<String> hpoFreqTermList;
    List<String> hpoExcludedHpoFreqTermList;
    HashMap<String, HashMap<String, Set<String>>> diseaseFeaturesDict;
    HashMap<String, String> geneIdToSymbolDict;

    /**
     * Read disease data from an uncompressed file <code>path</code>.
     */
    public static DiseaseDataParseIngest fromPath(Path phenotypeAnnotationFile) throws IOException {
        try (InputStream annotationStream = Files.newInputStream(phenotypeAnnotationFile)) {
            return new DiseaseDataParseIngest(annotationStream);
        }
    }

    /*
    Constructor call with defaults
     */
    public DiseaseDataParseIngest(InputStream annotationStream) {
        this(annotationStream,
                List.of("OMIM"),
                List.of("HP:0040280", "HP:0040281", "HP:0040282", "HP:0040283", "HP:0040284", "HP:0040285"),
                List.of("HP:0040285")
        );
    }

    /**
     * Read disease data from an input stream. The stream must <em>not</em> be compressed.
     * @param annotationStream the stream to read from.
     */
    public DiseaseDataParseIngest(InputStream annotationStream,
                                  List<String> validDatabaseList,
                                  List<String> hpoFreqTermList,
                                  List<String> hpoExcludedFreqTermList) {
        //this.validDatabaseList = List.of("OMIM", "ORPHA", "DECIPHER");
        this.validDatabaseList = validDatabaseList;

        // HPO frequency terms
        this.hpoFreqTermList = hpoFreqTermList;
        this.hpoExcludedHpoFreqTermList = hpoExcludedFreqTermList;

        // Create dictionary by parsing phenotype.hpoa
        this.diseaseFeaturesDict = ingest(annotationStream);
    }

    private HashMap<String, HashMap<String, Set<String>>> ingest(InputStream annotationStream) {
        HashMap<String, HashMap<String, Set<String>>> diseaseFeaturesDict = new HashMap<>();

        Scanner myReader = new Scanner(annotationStream);
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
                if (this.hpoExcludedHpoFreqTermList.contains(HpoFreqTerm)) {
                    // Term is excluded
                    diseaseFeaturesDict.get(disease_id).get("E").add(hpo_id);
                } else {
                    // Term is included
                    diseaseFeaturesDict.get(disease_id).get("I").add(hpo_id);
                }
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
        } else if (freqString.isEmpty()) {
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
        } else if (freqString.isEmpty()) {
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

    public void addDiseaseGeneAssociations(String diseaseGeneFile) {
        try (InputStream is = Files.newInputStream(Path.of(diseaseGeneFile))) {
            addDiseaseGeneAssociations(is);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public void addDiseaseGeneAssociations(InputStream is) {

        this.geneIdToSymbolDict = new HashMap<>();

        // Open HPOA file genes_to_disease.txt
        Scanner myReader = new Scanner(is);
        while (myReader.hasNextLine()) {
            String line = myReader.nextLine();
            // Skip header line
            if (line.startsWith("ncbi_gene_id")) {
                continue;
            }
            String[] fields = line.split("\t");
            if (fields.length != 5) {
                System.out.println("ERROR: Row does not have 5 fields!");
                break;
            }
            String ncbi_gene_id = fields[0];
            String gene_symbol = fields[1];
            String disease_id = fields[3];

            // Map gene IDs to gene symbols
            this.geneIdToSymbolDict.put(ncbi_gene_id, gene_symbol);

            // Check if disease is in dictionary
            if (this.diseaseFeaturesDict.containsKey(disease_id)) {
                // Check if disease already has genes
                if (!diseaseFeaturesDict.get(disease_id).containsKey("G")) {
                    diseaseFeaturesDict.get(disease_id).put("G", new HashSet<>());
                }
                diseaseFeaturesDict.get(disease_id).get("G").add(ncbi_gene_id);
            }
        }

        // Add gene symbols if available
        for (String disease_id : diseaseFeaturesDict.keySet()) {
            if (diseaseFeaturesDict.get(disease_id).get("G") != null) {
                diseaseFeaturesDict.get(disease_id).put("GS", new HashSet<>());
                Set<String> geneIds = diseaseFeaturesDict.get(disease_id).get("G");
                for (String gene_id : geneIds) {
                    String gene_symbol;
                    if (geneIdToSymbolDict.containsKey(gene_id)) {
                        gene_symbol = geneIdToSymbolDict.get(gene_id);
                    } else {
                        // Use NCBI Gene ID without colon instead of gene symbol
                        gene_symbol = gene_id.split(":")[0] + gene_id.split(":")[1];
                    }
                    diseaseFeaturesDict.get(disease_id).get("GS").add(gene_symbol);
                }
            } else {
                diseaseFeaturesDict.get(disease_id).put("G", new HashSet<>());
                diseaseFeaturesDict.get(disease_id).put("GS", new HashSet<>());
            }
        }
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
        if (this.diseaseFeaturesDict.containsKey(diseaseId)) {
            if (this.diseaseFeaturesDict.get(diseaseId).containsKey("G")) {
                return this.diseaseFeaturesDict.get(diseaseId).get("G");
            } else {
                return new HashSet<>();
            }
        } else {
                throw new IllegalArgumentException("Disease ID \"" + diseaseId + "\" not found!");
            }
    }

    @Override
    public Set<String> getDiseaseGeneSymbols(String diseaseId) {
        if (this.diseaseFeaturesDict.containsKey(diseaseId)) {
            if (this.diseaseFeaturesDict.get(diseaseId).containsKey("GS")) {
                return this.diseaseFeaturesDict.get(diseaseId).get("GS");
            } else {
                return new HashSet<>();
            }
        } else {
            throw new IllegalArgumentException("Disease ID \"" + diseaseId + "\" not found!");
        }
    }
}