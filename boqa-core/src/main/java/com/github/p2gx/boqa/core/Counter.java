package com.github.p2gx.boqa.core;

import java.util.Set;

/**
 * Classes that implement this interface implement different approaches for counting the BoqaCounts
 * (false-positives, false-negatives, true-negatives, true-positives).
 * How query and hidden layers are represented is left to the respective implementations.
 * <p>
 * By calling the constructor of an implementing class, hidden layers are created for all diseases.
 * A resulting Counter object is valid for a given HPO release and can be used for all possible analyses.
 * After initializing the QueryLayer, the method getBoqaCounts returns corresponding BoqaCounts for given diseases.
 * <p>
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 * @author <a href="mailto:leonardo.chimirri@bih-charite.de">Leonardo Chimirri</a>
 */
public interface Counter {

    /*
    Initialization of the query layer involves applying the annotation propagation rule.
     */
    void initQueryLayer(Set<String> queryTerms);

    /*
     Given the initialized query layer, returns a BoqaCounts object representing the counts of
     false-positives, false-negatives, true-negatives, and true-positives.
     */
    BoqaCounts getBoqaCounts(String diseaseId);

    /*
    Return a set with all disease IDs.
     */
    Set<String> getDiseaseIds();
}