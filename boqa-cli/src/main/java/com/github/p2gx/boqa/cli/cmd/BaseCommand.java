package com.github.p2gx.boqa.cli.cmd;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;


public class BaseCommand {
    private final static Logger LOGGER = LoggerFactory.getLogger(BaseCommand.class);

    @CommandLine.Option(
            names = {"-x", "--out-prefix"},
            description = "Common prefix for all generated files, which can also contain the path."
    )
    protected String outPrefix = "JABOQA";

    @CommandLine.Option(names={"-d","--data"}, description ="directory to download data (default: ${DEFAULT-VALUE})" )
    public String datadir="data";

/*

    HpoDiseaseLoaderOptions options = HpoDiseaseLoaderOptions.of(Set.of(DiseaseDatabase.OMIM), false, 5);
        LOGGER.info("HPOA loader options: {}", options);
    HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(ontology, options);
    Path annotpath = annotFile.toPath();
    HpoDiseases diseases = loader.load(annotpath);

 */

    protected Ontology getHpOntology() {
        File f = new File(datadir + File.separator + "hp.json");
        return OntologyLoader.loadOntology(f);
    }

    protected HpoDiseases getDiseases(Ontology ontology) throws IOException {
        File annotFile = new File(datadir + File.separator + "phenotype.hpoa");
        //HpoDiseaseLoaderOptions options = HpoDiseaseLoaderOptions.of(Set.of(DiseaseDatabase.OMIM), false, 5);
        HpoDiseaseLoaderOptions options = HpoDiseaseLoaderOptions.defaultOptions();
        LOGGER.info("HPOA loader options: {}", options);
        //HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(ontology, options);
        HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(ontology, HpoDiseaseLoaderOptions.defaultOmim());
        Path annotpath = annotFile.toPath();
        HpoDiseases diseases = loader.load(annotpath);
        return diseases;
    }

}