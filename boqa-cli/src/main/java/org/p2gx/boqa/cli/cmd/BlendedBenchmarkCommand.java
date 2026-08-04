package org.p2gx.boqa.cli.cmd;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.p2gx.boqa.core.Counter;
import org.p2gx.boqa.core.DiseaseData;
import org.p2gx.boqa.core.PatientData;
import org.p2gx.boqa.core.Writer;
import org.p2gx.boqa.core.algorithm.AlgorithmParameters;
import org.p2gx.boqa.core.algorithm.BoqaSetCounter;
import org.p2gx.boqa.core.analysis.BoqaAnalysisResult;
import org.p2gx.boqa.core.analysis.BoqaPatientAnalyzer;
import org.p2gx.boqa.core.diseases.BlendedDiseaseData;
import org.p2gx.boqa.core.diseases.DiseaseDataPhenolIngest;
import org.p2gx.boqa.core.output.JsonResultWriter;
import org.p2gx.boqa.core.patient.PhenopacketData;
import org.p2gx.boqa.core.genes.DiseaseGeneAssociations;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * Command for running BOQA analysis with blended scoring.
 * <p>
 * <strong>Work in progress:</strong> This command is not yet implemented and serves as a placeholder
 * for future functionality to handle patients affected by multiple genetic diseases simultaneously.
 * </p>
 *
 * @see BoqaBenchmarkCommand for the plain scoring implementation
 */
@CommandLine.Command(
        name = "blended",
        mixinStandardHelpOptions = true,
        description = "Performs analysis taking into account that patients may be affected by more than one genetic disease.",
        sortOptions = false)
public class BlendedBenchmarkCommand extends BoqaBenchmarkCommand implements Callable<Integer> {

    @CommandLine.Option(
            names={"-dg","--disease-gene-associations"},
            required = true,
            description ="HPOA file with disease-gene associations (genes_to_disease.txt).")
    private String diseaseGeneFile;

    @CommandLine.Option(
            names={"-g","--anchor-gene"},
            required = true,
            split = ",",
            description ="Comma-separated NCBI IDs of genes, e.g. -g NCBIGene:583,NCBIGene:3910.")
    private List<String> anchorGenes;

    @CommandLine.Option(
            names={"-r","--random-genes"},
            defaultValue = "10",
            description ="Number of randomly selected genes added to the anchor gene list when multiple anchors are provided (default: ${DEFAULT-VALUE}).")
    private int randomGeneCount;

    @CommandLine.Option(
            names={"-i","--iterations"},
            defaultValue = "10",
            description ="Number of repetitions of BOQA, each with different random genes (default: ${DEFAULT-VALUE}).")
    private int numberOfIterations;

    @Override
    public Integer call() throws Exception {

        LOGGER.info("Anchor genes: " + anchorGenes);

        LOGGER.info("Starting up BOQA analysis, loading ontology file {} ...", ontologyFile);
        Ontology hpo = OntologyLoader.loadOntology(Paths.get(ontologyFile).toFile());
        LOGGER.debug("Ontology loaded successfully from {}", ontologyFile);

        // Parse disease-HPO associations into DiseaseData object
        LOGGER.info("Importing disease phenotype associations {} from file: {} ...", diseaseDatabases.toString(), phenotypeAnnotationFile);
        if (diseaseDatabases.contains("OMIM") && diseaseDatabases.contains("ORPHA")) {
            throw new CommandLine.ParameterException(
                    new CommandLine(this),
                    "Error: OMIM and ORPHA cannot be used together!"
            );
        }
        if (!Set.of("OMIM", "ORPHA", "DECIPHER").containsAll(diseaseDatabases)) {
            throw new CommandLine.ParameterException(
                    new CommandLine(this),
                    "Error: Invalid database!"
            );
        }
        Set<DiseaseDatabase> DiseaseDatabaseSet = diseaseDatabases.stream()
                .map(DiseaseDatabase::fromString)
                .collect(Collectors.toSet());
        int defaultCohortSize = 100;
        HpoDiseaseLoaderOptions options = HpoDiseaseLoaderOptions.of(DiseaseDatabaseSet,false, defaultCohortSize);
        HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(hpo, options);
        HpoDiseases diseases = loader.load(phenotypeAnnotationFile);
        DiseaseData diseaseData = DiseaseDataPhenolIngest.of(hpo, diseases);
        DiseaseGeneAssociations geneAssociations = DiseaseGeneAssociations.fromFile(Paths.get(diseaseGeneFile));

        LOGGER.debug("Disease data parsed from {}", phenotypeAnnotationFile);

        LOGGER.info("Number of annotated diseases: {}", diseaseData.size());

        AlgorithmParameters params = AlgorithmParameters.create(alpha, beta);
        LOGGER.info("Using alpha={}, beta={}", params.getAlpha(), params.getBeta());

        int limit = (resultsLimit != null) ? resultsLimit : Integer.MAX_VALUE;

        LOGGER.info("Beginning BOQA analysis for phenopackets...");
        LOGGER.info("Results limit set to {}", limit);

        AtomicInteger fileCount = new AtomicInteger(0);
        List<BoqaAnalysisResult> boqaAnalysisResults = new ArrayList<>();

        // Two diseases may blend only if no single gene explains both. The anchors differ per
        // iteration, but the rule does not, so it is built once here.
        BiPredicate<String, String> mayBlendAnchors =
                BlendedDiseaseData.geneDisjointBlend(geneAssociations.geneIdsByDisease());

        for(int i=0; i<numberOfIterations; i++) {
            List<String> finalAnchorGenes;
            if (anchorGenes.size() > 1) {
                // Add randomly selected genes to the anchor gene list
                Set<String> allGeneIds = new HashSet<>(geneAssociations.allGeneIds());
                allGeneIds.removeAll(Set.copyOf(anchorGenes));
                List<String> randomGenes = new ArrayList<>(allGeneIds);
                Collections.shuffle(randomGenes);
                finalAnchorGenes = new ArrayList<>(anchorGenes);
                finalAnchorGenes.addAll(randomGenes.subList(0, Math.min(randomGeneCount, randomGenes.size())));
                LOGGER.info("Extended anchor gene list with {} random genes: {}", randomGeneCount, finalAnchorGenes);
            } else {
                finalAnchorGenes = anchorGenes;
            }

            Set<String> anchorDiseases = new HashSet<>(geneAssociations.diseaseIdsForGenes(finalAnchorGenes));
            anchorDiseases.retainAll(diseaseData.getDiseaseIds());

            BlendedDiseaseData blendedDiseaseData = new BlendedDiseaseData(diseaseData, anchorDiseases,
                    finalAnchorGenes.size() == 1 ? BlendedDiseaseData.PairingStrategy.ANCHOR_VS_ALL
                            : BlendedDiseaseData.PairingStrategy.ANCHOR_VS_ANCHOR,
                    mayBlendAnchors);

            LOGGER.info("Number of diseases diseases in BlendedDiseaseData: " + blendedDiseaseData.size());
            LOGGER.info("Creating BlendedDiseaseData object ...");

            // Initialize Counter
            Counter counter = new BoqaSetCounter(blendedDiseaseData, hpo);
            LOGGER.debug("Initialized BoqaSetCounter with {} diseases.", blendedDiseaseData.size());

            Path jsonFilePath;
            if (phenopacketFile.toString().endsWith(".txt")) {
                String pathString = Files.readAllLines(phenopacketFile).getFirst();
                jsonFilePath = Path.of(pathString);
            }
            else {
                jsonFilePath = phenopacketFile;
            }

            PatientData ppkt = new PhenopacketData(jsonFilePath, hpo);

            boqaAnalysisResults.add(
                    BoqaPatientAnalyzer.computeBoqaResults(
                            ppkt, counter, limit, params)
            );
            int count = fileCount.incrementAndGet();
            if (count % 10 == 0) {
                System.out.println("Iterations processed: " + count);
            }
        }
        LOGGER.info("Finished processing {} iterations.", fileCount.get());

        LOGGER.info("Writing results to {}", outPath);
        String cliArgs = String.join(" ", spec.commandLine().getParseResult().originalArgs());
        Writer writer = new JsonResultWriter();
        writer.writeResults(
                boqaAnalysisResults,
                Paths.get(ontologyFile),
                phenotypeAnnotationFile,
                cliArgs,
                Map.of("alpha", params.getAlpha(), "beta", params.getBeta()),
                outPath,
                compress
        );
        LOGGER.info("BOQA analysis completed successfully.");
        return 0;
    }
}
