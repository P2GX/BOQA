package org.p2gx.boqa.cli.cmd;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.p2gx.boqa.core.*;
import org.p2gx.boqa.core.algorithm.AlgorithmParameters;
import org.p2gx.boqa.core.algorithm.BoqaSetCounter;
import org.p2gx.boqa.core.analysis.BoqaAnalysisResult;
import org.p2gx.boqa.core.analysis.BoqaPatientAnalyzer;
import org.p2gx.boqa.core.diseases.DiseaseDataPhenolIngest;
import org.p2gx.boqa.core.output.JsonResultWriter;
import org.p2gx.boqa.core.patient.PhenopacketData;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Command for running BOQA benchmark analysis.
 * <p>
 * This command implements the BOQA algorithm as described in PMID:22843981 to match patient
 * phenotypes against disease-phenotype associations. It loads the HPO ontology, disease annotations (HPOA),
 * and phenopacket data, then computes Bayesian scores for each patient against all annotaed diseases.
 * Input data must be downloaded beforehand using the DownloadCommand.
 * </p>
 * <p>
 * Key features:
 * <ul>
 *   <li>Supports multiple disease databases: OMIM, ORPHA, and DECIPHER (note: OMIM and ORPHA cannot be used together)</li>
 *   <li>Algorithm parameters: alpha (false positive rate) and beta (false negative rate)</li>
 *   <li>Parallel processing of phenopackets for improved performance</li>
 *   <li>JSON output with analysis metadata and algorithm parameters</li>
 * </ul>
 * </p>
 *
 * @see BlendedBenchmarkCommand for blended-phenotype variant (work in progress)
 * @see DownloadCommand for data preparation
 */
@CommandLine.Command(
        name = "plain",
        mixinStandardHelpOptions = true,
        description = "Performs BOQA analysis as described in PMID:22843981, without taking annotation frequencies into account.",
        sortOptions = false)
public class BoqaBenchmarkCommand implements Callable<Integer>  {
    private static final Logger LOGGER = LoggerFactory.getLogger(BoqaBenchmarkCommand.class);

    @Spec
    CommandSpec spec;

    @CommandLine.Option(
            names={"-dp","--disease-phenotype-associations"},
            required = true,
            description ="Big HPO annotation file (phenotype.hpoa).")
    private Path phenotypeAnnotationFile;

    @CommandLine.Option(
            names={"-o","--ontology"},
            required = true,
            description ="HPO in JSON format.")
    private String ontologyFile;

    @CommandLine.Option(
            names = {"-p", "--phenopackets"},
            required = true,
            description = "Input a text file with list of absolute paths to phenopackets.")
    private Path phenopacketFile;

    @CommandLine.Option(
            names={"-n","--num-of-processes"},
            description ="Number of processes that will run in parallel (default: ${DEFAULT-VALUE}).",
            defaultValue = "1")
    private int numOfProcesses;

    @CommandLine.Option(
            names = "--out",
            description = "Output JSON file",
            required = true)
    private Path outPath;

    @CommandLine.Option(
            names = {"-L", "--limit"},
            description = "Limit number of diseases reported in output.")
    private Integer resultsLimit;

    @CommandLine.Option(
            names={"-a","--alpha"},
            description = "Float value such that 0<alpha<1 (default: ${DEFAULT-VALUE}).",
            defaultValue = "5.241914347119568E-05")
    private Double alpha;

    @CommandLine.Option(
            names={"-b","--beta"},
            description = "Float value such that 0<beta<1 (default: ${DEFAULT-VALUE}).",
            defaultValue = "0.9")
    private Double beta;

    @CommandLine.Option(
            names={"-db", "--database"},
            description = "Comma-separated list of databases. Valid databases are OMIM, ORPHA, and DECIPHER (default: ${DEFAULT-VALUE})." +
                    "The databases OMIM and ORPHA must not be used at the same time!",
            defaultValue = "OMIM",
            split = ",")
    private Set<String> diseaseDatabases;

    @Override
    public Integer call() throws Exception {
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

        LOGGER.debug("Disease data parsed from {}", phenotypeAnnotationFile);

        AlgorithmParameters params = AlgorithmParameters.create(alpha, beta);
        LOGGER.info("Using alpha={}, beta={}", params.getAlpha(), params.getBeta());

        // Initialize Counter
        Counter counter = new BoqaSetCounter(diseaseData, hpo);
        LOGGER.debug("Initialized BoqaSetCounter with {} diseases.", diseaseData.size());

        int limit = (resultsLimit != null) ? resultsLimit : Integer.MAX_VALUE;
        List<BoqaAnalysisResult> boqaAnalysisResults = new ArrayList<>();

        AtomicInteger fileCount = new AtomicInteger(0);

        LOGGER.info("Beginning BOQA analysis for phenopackets...");
        LOGGER.info("Results limit set to {}", limit);
        // For each line in the phenopacketFile compute counts (run the analysis) and add them to boqaAnalysisResults
        try (Stream<String> stream = Files.lines(phenopacketFile)) {
            boqaAnalysisResults = stream
                    .map(Path::of)
                    .parallel()
                    .map(singleFile -> {
                        PatientData ppkt = new PhenopacketData(singleFile);
                        BoqaAnalysisResult result = BoqaPatientAnalyzer.computeBoqaResults(
                                ppkt, counter, limit, params);
                        int count = fileCount.incrementAndGet();
                        if (count % 50 == 0) {
                            System.out.println("Processed: " + count);
                        }
                        return result;
                    })
                    .toList();
        } catch (IOException e) {
            LOGGER.warn("Could not read phenopacket list from {}", phenopacketFile, e);
        }
        LOGGER.info("Finished processing {} phenopackets.", fileCount.get());

        LOGGER.info("Writing results to {}", outPath);
        String cliArgs = String.join(" ", spec.commandLine().getParseResult().originalArgs());
        Writer writer = new JsonResultWriter();
        writer.writeResults(
                boqaAnalysisResults,
                Paths.get(ontologyFile),
                phenotypeAnnotationFile,
                cliArgs,
                Map.of("alpha", params.getAlpha(), "beta", params.getBeta()),
                outPath
        );
        LOGGER.info("BOQA analysis completed successfully.");
        return 0;
    }
}
