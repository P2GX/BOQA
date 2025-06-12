package com.github.p2gx.boqa.cli.cmd;

import picocli.CommandLine;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CommandLine.Command(
        name = "plain",
        mixinStandardHelpOptions = true,
        description = "Performs BOQA analysis as described in PMID:22843981, without taking annotation frequencies into account.",
        sortOptions = false)
public class BoqaCommand extends BaseCommand implements Callable<Integer>  {

    private static final Logger logger = LoggerFactory.getLogger(BoqaCommand.class);

    @CommandLine.Option(
            names={"-dp","--disease-phenotype-associations"},
            required = true,
            description ="Big HPO annotation file (phenotype.hpoa).")
    private String phenotypeAnnotationFile;

    @CommandLine.Option(
            names={"-o","--ontology"},
            required = true,
            description ="HPO in JSON format.")
    private String ontologyFile;

    @CommandLine.Option(
            names = {"-p", "--phenopackets"},
            required = true,
            description = "Input phenopacket file in JSON format or text file with list of absolute paths to phenopackets.")
    private String phenopacketFile;

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
        // Example of how to make a log message appear in log file
        //logger.warn("Example log from {}", BoqaCommand.class.getSimpleName());

        // results_dict = {id is phenopacket
        // metadata such as timestamp, algo and version used, probability is main result, also save the four exponents}
        // Prepare data structure for disease-phenotype associations

        // MAYBE run on one ppkt only, then we have a benchmark command for running it many times, grabbing many results
        // and making comparison tables and plots?
        // NO for each phenopacket

        // Read in phenopacket
        // write ppktID and metadata to result file
        // extract HPOs as Set<termId>
        // Traverser: create set of ON nodes, Set<termId> of Q layer
        // for each disease
            // Peter H code: output is Set<termId>, HPOs of that disease
            // Traverser: create set of ON nodes, Set<termId> of H layer
            // Set operations-based counting, return array of four numbers (the exponents of alfa, beta...)
            // Compute unnormalized probability
            // add  disease1: {normprob: emppty, unnorm prob: val, m001: val, m010...} to results dict
        // Based on above result dict, sum unnorm prob to get normalization factor and insert it into normprob

        return 0;
    }
}
