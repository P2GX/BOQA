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
        // metadata such as timestamp, algo and version used
        // Prepare data structure for disease-phenotype associations

        // BoqaAnalysis class has what I/O exactly

        // for each ppkt (chunks of n parallelized)
            // Read in phenopacket
            // write ppktID and metadata to result file
            // extract HPOs as Set<termId>
            // Traverser class: create set of ON nodes, Set<TermId> of Q layer --> Initialize counts
            // for each disease (parallelize here, too?)
                // Peter H code: output is Set<termId>, HPOs of that disease H layer
                // Traverser class: create set of ON nodes, Set<TermId> of H layer
                // Counter class' method: Set operations-based counting, return array of four exponents
                // Compute unnormalized probability
                // add  disease1: {normprob: emppty, unnorm prob: val, m001: val, m010...} to results dict
            // Based on above result dict, sum unnorm prob to get normalization factor and insert it into normprob

        return 0;
    }
}
