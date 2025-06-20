package com.github.p2gx.boqa.core;

import java.util.Map;
import java.util.Set;

/**
 * This interface provides query data from different input formats in a uniform manner.
 * Different implementations of this interface parse different input formats.
 * <p>
 * Input formats include:
 * <ul>
 *     <li> Phenopackets (QueryDataFromPhenopacket)
 *     <li> Strings consisting of comma-separated HPO terms (QueryDataFromString)
 *     <li> Maybe other formats
 * </ul>
 * <p>
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 * @author <a href="mailto:leonardo.chimirri@bih-charite.de">Leonardo Chimirri</a>
 */
public interface PatientData {

    // Returns a map of phenopacket ids and corresponding HPO terms
    Map<String, Set<String>> getPhenotypes();
}