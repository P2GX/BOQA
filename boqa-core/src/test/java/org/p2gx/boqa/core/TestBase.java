package org.p2gx.boqa.core;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;


import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Set;
import java.util.zip.GZIPInputStream;

public class TestBase {

    private static Ontology hpo = null;
    private static HpoDiseases hpoDiseases = null;


    public static Ontology hpo() throws IOException {
        if (hpo == null) {
            InputStream ontologyStream = new GZIPInputStream(Objects.requireNonNull(TestBase.class
                    .getResourceAsStream("/org/p2gx/boqa/core/hp.v2025-05-06.json.gz")));
            TestBase.hpo = OntologyLoader.loadOntology(ontologyStream);
        }
        return hpo;
    }

    public static HpoDiseases hpoDiseases() throws IOException {
        if (hpoDiseases == null) {
            HpoDiseaseLoaderOptions options = HpoDiseaseLoaderOptions.of(Set.of(DiseaseDatabase.OMIM), false, 100);
            HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(hpo(), options);
            InputStream annotationStream = new GZIPInputStream(Objects.requireNonNull(TestBase.class
                    .getResourceAsStream("/org/p2gx/boqa/core/phenotype.v2025-05-06.hpoa.gz")));
            TestBase.hpoDiseases = loader.load(annotationStream);
        }
        return hpoDiseases;
    }


}
