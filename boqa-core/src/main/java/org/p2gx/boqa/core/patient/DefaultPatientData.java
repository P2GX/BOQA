package org.p2gx.boqa.core.patient;

import java.util.Set;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.boqa.core.PatientData;

public class DefaultPatientData implements PatientData {
    private final String id;
    private final Set<TermId> observedHpoIds;

    public DefaultPatientData(String identifier, Set<TermId> observed) {
        this.id = identifier;
        this.observedHpoIds = observed;
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
