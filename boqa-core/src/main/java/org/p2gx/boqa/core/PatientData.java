package org.p2gx.boqa.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.boqa.core.patient.DefaultPatientData;

import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Set;

/**
 * This interface provides query data from different input formats in a uniform manner.
 * Different implementations of this interface parse different input formats.
 * <p>
 * Input formats include:
 * <ul>
 *     <li> Phenopackets (PhenopacketReader)
 *     <li> Strings consisting of comma-separated HPO terms (QueryDataFromString)
 *     <li> Maybe other formats
 * </ul>
 * <p>
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 * @author <a href="mailto:leonardo.chimirri@bih-charite.de">Leonardo Chimirri</a>
 *
 */
public interface PatientData {
    String getID();

    // Returns a map of phenopacket ids and corresponding HPO terms
    @JsonIgnore
    Set<TermId> getObservedTerms();
    @JsonIgnore
    default Set<TermId> getExcludedTerms() {
        // If excluded terms are not used or not available
        return Set.of();
    }

    /** The Exomiser provides us with a List of HPO identifiers as Strings. */
    public static PatientData fromObservedHpoTermList(List<String> observed) {
        String randomId = java.util.UUID.randomUUID().toString();
        Set<TermId> observedTidSet = observed.stream()
            .map(TermId::of)
            .collect(toSet());
        return new DefaultPatientData(randomId, observedTidSet);
    }

}
