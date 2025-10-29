package org.p2gx.boqa.core.diseases;

import org.p2gx.boqa.core.DiseaseData;
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
 * @deprecated use the {@link DiseaseDataPhenolIngest} class instead.
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 */
@Deprecated(forRemoval = true)
public class DiseaseDataParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiseaseDataParser.class);

    private static final Pattern FRACTION_PATTERN = Pattern.compile("\\d+/\\d+");
    private static final Pattern INTEGER_PERCENT_PATTERN = Pattern.compile("\\d{1,3}%");
    private static final Pattern DOUBLE_PERCENT_PATTERN = Pattern.compile("\\d{1,3}\\.\\d{1,20}%$");

    // Valid databases are "OMIM", "ORPHA", and "DECIPHER"
    private final Set<String> validDatabases;
    private static final String OBLIGATE = "HP:0040280";
    private static final String VERY_FREQUENT = "HP:0040281";
    private static final String FREQUENT = "HP:0040282";
    private static final String OCCASIONAL = "HP:0040283";
    private static final String VERY_RARE = "HP:0040284";
    private static final String EXCLUDED = "HP:0040285";
    private static final Set<String> HPO_FREQ_TERMS = Set.of(OBLIGATE, VERY_FREQUENT, FREQUENT, OCCASIONAL, VERY_RARE, EXCLUDED);

    private DiseaseDataParser() {
        this.validDatabases = Set.of("OMIM"); // Valid databases are "OMIM", "ORPHA", and "DECIPHER"
    }

    public static DiseaseData parseDiseaseDataFromHpoa(InputStream hpoAnnotationsStream) {
        DiseaseDataParser diseaseDataParser = new DiseaseDataParser();
        Map<String, DiseaseFeatures> diseaseFeaturesById = diseaseDataParser.parseDiseaseAnnotations(hpoAnnotationsStream, Map.of());
        return new DefaultDiseaseData(diseaseFeaturesById);
    }

    public static DiseaseData parseDiseaseDataFromHpoaWithGeneAssociations(InputStream hpoAnnotationsStream, InputStream diseaseGeneSteam) {
        DiseaseDataParser diseaseDataParser = new DiseaseDataParser();
        Map<String, Set<GeneIdSymbol>> diseaseGeneAssociations = diseaseDataParser.addDiseaseGeneAssociations(diseaseGeneSteam);
        Map<String, DiseaseFeatures> diseaseAnnotations = diseaseDataParser.parseDiseaseAnnotations(hpoAnnotationsStream, diseaseGeneAssociations);
        return new DefaultDiseaseData(diseaseAnnotations);
    }

    /**
     * Read disease data from an uncompressed file <code>path</code>.
     */
    public static DiseaseData parseDiseaseDataFromHpoa(Path phenotypeAnnotationFile) throws IOException {
        try (InputStream annotationStream = Files.newInputStream(phenotypeAnnotationFile)) {
            return DiseaseDataParser.parseDiseaseDataFromHpoa(annotationStream);
        }
    }

    private Map<String, DiseaseFeatures> parseDiseaseAnnotations(InputStream annotationStream, Map<String, Set<GeneIdSymbol>> diseaseGeneAssociations) {

        Map<String, DiseaseFeatures> diseaseFeaturesMap = new LinkedHashMap<>(10000);

        Scanner myReader = new Scanner(annotationStream);
        while (myReader.hasNextLine()) {
            String line = myReader.nextLine();
            // Skip header lines
            if (line.startsWith("#") || line.startsWith("database_id")) {
                continue;
            }
            String[] fields = line.split("\t");
            if (fields.length != 12) {
                LOGGER.error("ERROR: Row does not have 12 fields! {}", line);
                break;
            }
            String diseaseId = fields[0];
            String diseaseLabel = fields[1];
//            idToLabel.putIfAbsent(diseaseId, diseaseLabel);
            String qualifier = fields[2];
            String hpoId = fields[3];
            String frequency = fields[7];
            String databaseTag = diseaseId.split(":")[0];
            if (!validDatabases.contains(databaseTag)) {
                // Annotations can be restricted to individual databases from OMIM, ORPHA and DECIPHER.
                continue;
            }
            String hpoFreqTerm = freqStringToHpoTerm(frequency);
            if (qualifier.equals("NOT")) {
                hpoFreqTerm = EXCLUDED;
            }
            DiseaseFeatures diseaseFeatures = diseaseFeaturesMap.computeIfAbsent(diseaseId, k -> DiseaseFeatures.of(diseaseId, diseaseLabel));
            if (EXCLUDED.equals(hpoFreqTerm)) {
                // Term is excluded
                diseaseFeatures.excludedPhenotypes().add(hpoId);
            } else {
                // Term is observed
                diseaseFeatures.observedPhenotypes().add(hpoId);
            }
            Set<GeneIdSymbol> diseaseGeneSymbols = diseaseGeneAssociations.getOrDefault(diseaseId, Set.of());
            diseaseGeneSymbols.forEach(geneIdSymbol -> {
                diseaseFeatures.geneIds().add(geneIdSymbol.geneId());
                diseaseFeatures.geneSymbols().add(geneIdSymbol.geneSymbol());
            });
        }
        return diseaseFeaturesMap;
    }

    public static double freqStringToFloat(String freqString) {
        /*
        Given an HPOA frequency string like "1/4", "20%" or “2.5”, return a float like 0.2, 0.205 or 0.25.
        */
        double freqFloat;
        freqString = freqString.trim();
        if (FRACTION_PATTERN.matcher(freqString).matches()) {
            // E.g., 1/4
            double numerator = Integer.parseInt(freqString.split("/")[0]);
            double denominator = Integer.parseInt(freqString.split("/")[1]);
            freqFloat = numerator/denominator;
        } else if (INTEGER_PERCENT_PATTERN.matcher(freqString).matches()) {
            // E.g., 20%
            double percentage = Double.parseDouble(freqString.split("%")[0]);
            freqFloat = percentage/100;
        } else if (DOUBLE_PERCENT_PATTERN.matcher(freqString).matches()) {
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
        if (freqFloat < 0 || 1 < freqFloat) {
            freqFloat = -1.0;
        }
        if (freqFloat == -1) {
            LOGGER.error("Invalid frequency string \"{}\" from HPOA!", freqString);
        }
        return freqFloat;
    }

    public static String freqStringToHpoTerm(String freqString) {
        /*
        Given an HPOA frequency string like “1/4”, “20%” or “2.5”, return one of the corresponding HPO frequency terms.
         */

        if (HPO_FREQ_TERMS.contains(freqString)) {
            // Frequency already specified as HPO term
            return freqString;
        } else if (freqString.isEmpty()) {
            // If no frequency is given, we assume a frequency of 1
            return OBLIGATE;
        } else {
            double freqFloat = freqStringToFloat(freqString);

            if (freqFloat == 1.0) {
                // Obligate
                return OBLIGATE;
            } else if (freqFloat >= 0.8 && freqFloat < 1.0) {
                // Very frequent
               return VERY_FREQUENT;
            } else if (freqFloat >= 0.3 && freqFloat < 0.8) {
                // Frequent
                return FREQUENT;
            } else if (freqFloat >= 0.05 && freqFloat < 0.3) {
                // Occasional
                return OCCASIONAL;
            } else if (freqFloat >= 0.01 && freqFloat < 0.05) {
                // Very rare
                return VERY_RARE;
            } else {
                // Excluded
                return EXCLUDED;
            }
        }
    }

    record GeneIdSymbol(String geneId, String geneSymbol) {}

    private Map<String, Set<GeneIdSymbol>> addDiseaseGeneAssociations(InputStream is) {

        Map<String, Set<GeneIdSymbol>> diseaseGeneAssociations = new HashMap<>();
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
                LOGGER.error("Row does not have 5 fields! {}", line);
                break;
            }
            String ncbiGeneId = fields[0];
            String geneSymbol = fields[1];
            String diseaseId = fields[3];

            Set<GeneIdSymbol> geneIdSymbols = diseaseGeneAssociations.computeIfAbsent(diseaseId, k -> new HashSet<>());
            geneIdSymbols.add(new GeneIdSymbol(ncbiGeneId, geneSymbol));
//            // Map gene IDs to gene symbols
//            // Check if disease is in dictionary
//            if (diseaseFeaturesById.containsKey(diseaseId)) {
//                DiseaseFeatures diseaseFeatures = diseaseFeaturesById.get(diseaseId);
//                diseaseFeatures.geneIds().add(ncbiGeneId);
//                // Use NCBI Gene ID without colon instead of gene symbol?
//                diseaseFeatures.geneSymbols().add(geneSymbol);
//            }
        }
        return diseaseGeneAssociations;
    }
}