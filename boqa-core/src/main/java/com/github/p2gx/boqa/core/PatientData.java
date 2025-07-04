package com.github.p2gx.boqa.core;

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
 * TODO: Add parameters alpha and beta
 */
public interface PatientData {
    String getID();
    // Returns a map of phenopacket ids and corresponding HPO terms
    Set<String> getObservedTerms();
    default Set<String> getExcludedTerms() {
        // If excluded terms are not used or not available
        return Set.of();
    }
}