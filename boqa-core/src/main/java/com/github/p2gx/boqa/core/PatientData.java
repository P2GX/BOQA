package com.github.p2gx.boqa.core;

import org.monarchinitiative.phenol.ontology.data.TermId;

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
 * TODO: Add parameters alpha and beta
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 * @author <a href="mailto:leonardo.chimirri@bih-charite.de">Leonardo Chimirri</a>
 *
 */
public interface PatientData {
    String getID();
    // Returns a map of phenopacket ids and corresponding HPO terms
    Set<TermId> getObservedTerms();
    default Set<TermId> getExcludedTerms() {
        // If excluded terms are not used or not available
        return Set.of();
    }
}