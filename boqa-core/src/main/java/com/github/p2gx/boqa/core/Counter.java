package com.github.p2gx.boqa.core;

import java.util.ArrayList;
import java.util.Set;

/**
 * Describe
 * * <p>
 * Describe
 * <p>
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 * @author <a href="mailto:leonardo.chimirri@bih-charite.de">Leonardo Chimirri</a>
 */
public interface Counter {

    void initQLayer(Set<String> queryTerms);
    /*
     Given the query terms, returns a list of four (or eight) integer values representing
     the counts of false positives, false negatives, true positives, and true negatives.
     */
    ArrayList<Integer> getCounts(String diseaseId);

}
