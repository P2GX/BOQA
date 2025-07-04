package com.github.p2gx.boqa.cli.cmd;

import com.github.p2gx.boqa.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

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
        // Prepare DiseaseData
        DiseaseData diseaseData = DiseaseDataParseIngest.fromPath(phenotypeAnnotationFile);

        // Initialize Counter
        Counter counter = new CounterDummy(diseaseData);

        // Read in list of paths to files
        List<Path> patientFiles = List.of();
        try {
            lines(phenopacketFile).map(Path::of).forEach(p -> {
                patientFiles.add(p);
            });
        } catch (IOException e) {
            LOGGER.warn("File {} does not exist.", e.getMessage()); // TODO make better
        }

        //TODO: make sure Set is appropriate here
        Set<AnalysisResults> analysisResultsSet = Set.of();

        // for item in phenopacketFile
        for(Path singlefile : patientFiles) {
            // Import Patient Data
            PatientData phenopacket = new PhenopacketReader(singlefile);
            counter.initQueryLayer(phenopacket.getObservedTerms());
            // Perform Analysis(phenopacket)
            Analysis analysis = new AnalysisDummy(phenopacket, counter);
            analysis.run();
            analysisResultsSet.add(analysis.getResults());
        }
        return 0;
    }
}
