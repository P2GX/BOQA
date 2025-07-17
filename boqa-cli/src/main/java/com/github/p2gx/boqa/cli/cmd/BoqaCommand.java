package com.github.p2gx.boqa.cli.cmd;

import com.github.p2gx.boqa.core.*;
import org.monarchinitiative.phenol.graph.OntologyGraph;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static java.nio.file.Files.lines;

@CommandLine.Command(
        name = "plain",
        mixinStandardHelpOptions = true,
        description = "Performs BOQA analysis as described in PMID:22843981, without taking annotation frequencies into account.",
        sortOptions = false)
public class BoqaCommand extends BaseCommand implements Callable<Integer>  {
    private static final Logger LOGGER = LoggerFactory.getLogger(BoqaCommand.class);

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


    public BoqaCommand(){}

    @Override
    public Integer call() throws Exception {
        //TODO Ielis suggests to only load the ontology once at the beginning, change DiseasesData
        OntologyGraph<TermId> hpoGraph = OntologyLoader.loadOntology(Paths.get(ontologyFile).toFile()).graph();

        // Prepare DiseaseData
        DiseaseData diseaseData = DiseaseDataParseIngest.fromPath(phenotypeAnnotationFile);

        // Initialize Counter
        Counter counter = new BoqaSetCounter(diseaseData, hpoGraph);

        // Read in list of paths to files
        List<Path> patientFiles =  new ArrayList<>();
        try (Stream<String> stream = lines(phenopacketFile)) {
            stream.map(Path::of).forEach(patientFiles::add);
        } catch (IOException e) {
            LOGGER.warn("File {} does not exist.", e.getMessage());  // TODO make better
        }

        Set<AnalysisResults> analysisResults = new HashSet<>();

        // for item in phenopacketFile
        for(Path singlefile : patientFiles) {
            // Import Patient Data
            PatientData phenopacket = new PhenopacketReader(singlefile);
            // Perform Analysis(phenopacket)
            Analysis analysis = new AnalysisDummy(phenopacket, counter);
            analysis.run();
            analysisResults.add(analysis.getResults());
        }

        // TODO This is just a placeholder to print out something to look at
        analysisResults.stream()
                .findFirst()
                .ifPresent(result -> {
                    System.out.println("\n\nPatientData\nPhenopacket ID: " + result.getPatientData().getID());
                    System.out.println("Observed HPOs: " + result.getPatientData().getObservedTerms());
                    System.out.println("Excluded HPOs: " + result.getPatientData().getExcludedTerms());

                    String boqaStr = result.getBoqaCounts().toString();
                    int n = 200; // number of chars to print
                    String shortBoqaStr = boqaStr.length() > n ? boqaStr.substring(0, n) + "..." : boqaStr;
                    System.out.println("\nBoqaCounts (first " + n + " chars): " + shortBoqaStr);
                });
        return 0;
    }
}
