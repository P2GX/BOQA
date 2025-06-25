package com.github.p2gx.boqa.cli.cmd;

import com.github.p2gx.boqa.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        // Example of how to make a log message appear in log file
        //logger.warn("Example log from {}", BoqaCommand.class.getSimpleName());
        
        // Prepare DiseaseData
        DiseaseData diseaseData = DiseaseDataParseIngest.fromPath(phenotypeAnnotationFile);

        // Prepare QueryData
        String includedTerms = "HP:0000006,HP:0005181,HP:0001658,HP:0003233";
        String excludedTerms = "HP:0002155";
        QueryData queryData1 = new QueryDataFromString(includedTerms, excludedTerms);
        QueryData queryData2 = new QueryDataFromString(includedTerms, excludedTerms);
        List<QueryData> QueryDataList = List.of(queryData1, queryData2);

        // Initialize Counter
        Counter counter = new CounterDummy(diseaseData);

        for (QueryData query : QueryDataList) {
            // Perform Analysis(q)
        }

        return 0;
    }
}
