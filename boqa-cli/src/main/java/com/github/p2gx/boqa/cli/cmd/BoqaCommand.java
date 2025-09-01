package com.github.p2gx.boqa.cli.cmd;

import com.github.p2gx.boqa.core.*;
import com.github.p2gx.boqa.core.algorithm.AlgorithmParameters;
import com.github.p2gx.boqa.core.algorithm.BoqaSetCounter;
import com.github.p2gx.boqa.core.analysis.AnalysisResults;
import com.github.p2gx.boqa.core.analysis.PatientCountsAnalysis;
import com.github.p2gx.boqa.core.diseases.DiseaseDataParseIngest;
import com.github.p2gx.boqa.core.output.JsonResultWriter;
import com.github.p2gx.boqa.core.patient.PhenopacketData;
import org.monarchinitiative.phenol.graph.OntologyGraph;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;


@CommandLine.Command(
        name = "plain",
        mixinStandardHelpOptions = true,
        description = "Performs BOQA analysis as described in PMID:22843981, without taking annotation frequencies into account.",
        sortOptions = false)
public class BoqaCommand extends BaseCommand implements Callable<Integer>  {
    private static final Logger LOGGER = LoggerFactory.getLogger(BoqaCommand.class);

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
            description = "Input a text file with list of absolute paths to patient files.")
    private Path phenopacketFile;

    @CommandLine.Option(
            names={"-a","--a-param"},
            description ="Float value between 0 and 5 used to define parameter alpha (default: ${DEFAULT-VALUE}).",
            defaultValue = "1.0")
    private float aParam;

    @CommandLine.Option(
            names={"-b","--b-param"},
            description ="Float value between 0 and 9 used to define parameter beta (default: ${DEFAULT-VALUE}).",
            defaultValue = "1.0")
    private float bParam;

    @CommandLine.Option(
            names={"-n","--num-of-processes"},
            description ="Number of processes that will run in parallel (default: ${DEFAULT-VALUE}).",
            defaultValue = "1")
    private int numOfProcesses;

    @CommandLine.Option(
            names = "--out",
            description = "Output JSON file",
            required = true)
    Path outPath;

    @CommandLine.Option(
            names = {"-L", "--limit"},
            description = "Limit number of diseases reported in output.",
            required = false)
    Integer resultsLimit;

    public BoqaCommand(){}

    @Override
    public Integer call() throws Exception {

        //TODO Ielis suggests to only load the ontology once at the beginning, change DiseasesData
        OntologyGraph<TermId> hpoGraph = OntologyLoader.loadOntology(Paths.get(ontologyFile).toFile()).graph();

        // Prepare DiseaseData
        DiseaseData diseaseData = DiseaseDataParseIngest.fromPath(phenotypeAnnotationFile);

        // Initialize Counter
        Counter counter = new BoqaSetCounter(diseaseData, hpoGraph, false);

        int limit = (resultsLimit != null) ? resultsLimit : Integer.MAX_VALUE;
        Set<AnalysisResults> analysisResults = new HashSet<>();
        AtomicInteger fileCount = new AtomicInteger(0);

        // For each line in the phenopacketFile compute counts (run the analysis) and add them to analysisResults
        try (Stream<String> stream = Files.lines(phenopacketFile)) {
            stream.map(Path::of).forEach(singleFile -> {
                Analysis analysis = new PatientCountsAnalysis(new PhenopacketData(singleFile), counter, limit);
                analysis.run();
                analysisResults.add(analysis.getResults());

                int count = fileCount.incrementAndGet();
                if (count % 10 == 0) {
                    System.out.println("Processed: " + count);
                }
            });
        } catch (IOException e) {
            LOGGER.warn("Could not read patient file list from {}", phenopacketFile, e);
        }

        String cliArgs = String.join(" ", spec.commandLine().getParseResult().originalArgs());
        Writer writer = new JsonResultWriter();
        writer.writeResults(
                analysisResults,
                Paths.get(ontologyFile),
                phenotypeAnnotationFile,
                cliArgs,
                Map.of("alpha", AlgorithmParameters.ALPHA, "beta", AlgorithmParameters.BETA),
                outPath
        );

        return 0;
    }
}
