package org.p2gx.boqa.core.patient;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.boqa.core.PatientData;
import org.p2gx.boqa.core.internal.OntologyTraverser;

import static java.util.stream.Collectors.toSet;

public class DefaultPatientData implements PatientData {
    private final String id;
    private final Set<TermId> observedHpoIds;

    public DefaultPatientData(String identifier, Set<TermId> observed, Ontology hpo) {
        this.id = identifier;
        OntologyTraverser traverser = new OntologyTraverser(hpo);
        this.observedHpoIds = observed.stream()
                .map(traverser::getPrimaryTermId)
                .filter(Objects::nonNull) // If old HPO is used without a term, avoids the program crashing
                .collect(Collectors.toSet());
    }

    @Override
    public String getID() {
        return this.id;
    }

    @Override
    public Set<TermId> getObservedTerms() {
        return this.observedHpoIds;
    }

}
