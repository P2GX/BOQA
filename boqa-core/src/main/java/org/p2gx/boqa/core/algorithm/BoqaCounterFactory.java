package org.p2gx.boqa.core.algorithm;

import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.boqa.core.Counter;

import java.util.Set;

public class BoqaCounterFactory {

    private final Ontology hpo;
    private final OntologyTraverser ontologyTraverser;

    public BoqaCounterFactory(Ontology hpo) {
        this.hpo = hpo;
        this.ontologyTraverser = new OntologyTraverser(hpo);
    }

    public Counter createCounter(Set<TermId> patientHpos) {
        return new SetCounter(
                ontologyTraverser,
                patientHpos);
    }
}