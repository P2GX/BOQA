package com.github.p2gx.boqa.cli.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import com.github.p2gx.boqa.core.DiseaseDict;

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
    private Path ontologyFile;

    @CommandLine.Option(
            names = {"-p", "--phenopackets"},
            required = true,
            description = "Input phenopacket file in JSON format or text file with list of absolute paths to phenopackets.")
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

        // Prepare data structure for disease-phenotype associations
        //DiseaseDict diseaseDict = new DiseaseDict(phenotypeAnnotationFile, ontologyFile);
        // Prepare data structure for HP ontology
        // Perform BOQA analysis
        // Report results

        return 0;
    }
}
