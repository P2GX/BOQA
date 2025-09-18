package com.github.p2gx.boqa.cli.cmd;

import picocli.CommandLine;
import java.util.concurrent.Callable;

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
            description ="NCBI ID of a gene that has already been identified as the cause of the disease " +
                    "but does not fully explain the phenotypic features observed. " +
                    "The aim of the analysis is to identify a second gene based on the phenotypic features observed.")
    private String anchorGene;


    public BlendedBenchmarkCommand(){}

    @Override
    public Integer call() throws Exception {

        // Prepare data structure for disease-phenotype associations (blended)
        // Prepare data structure for HP ontology
        // Perform BOQA analysis
        // Report results

        return 0;
    }
}
