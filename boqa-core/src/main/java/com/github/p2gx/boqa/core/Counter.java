package com.github.p2gx.boqa.core;

import com.github.p2gx.boqa.core.algorithm.BoqaCounts;

import java.util.Set;

/**
 * Classes that implement this interface implement different approaches for counting the BoqaCounts
 * (related to false-positives, false-negatives, true-negatives, true-positives).
 * How query and hidden layers are represented is left to the respective implementations.
 * <p>
 * By calling the constructor of an implementing class, hidden layers are created for all diseases.
 * A resulting Counter object is valid for a given HPO release and can be used for all possible analyses.
 * <p>
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 * @author <a href="mailto:leonardo.chimirri@bih-charite.de">Leonardo Chimirri</a>
 */
public interface Counter {

    /**
     * Given a patient's observed HPOs, returns a BoqaCounts object representing
     * the exponents of alpha, beta, 1-alpha, 1-beta
     */
    BoqaCounts computeBoqaCounts(String diseaseId, PatientData patientData);

    /**
     * Return a set with all disease IDs.
     */
    Set<String> getDiseaseIds();
}
