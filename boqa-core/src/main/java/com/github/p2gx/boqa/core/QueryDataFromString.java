package com.github.p2gx.boqa.core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that implements the QueryData interface by parsing comma-strings of HPO terms.
 * <p>
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 */
public class QueryDataFromString implements PatientData {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueryDataFromString.class);

    Set<String> includedTerms;
    Set<String> excludedTerms;

    public QueryDataFromString(String includedTermListString, String excludedTermListString) {
        this.includedTerms = Arrays.asList(includedTermListString.split(",")).stream()
                .filter(t -> t.matches("HP:\\d{7}"))
                .collect(Collectors.toSet());
        this.excludedTerms = Arrays.asList(excludedTermListString.split(",")).stream()
                .filter(t -> t.matches("HP:\\d{7}"))
                .collect(Collectors.toSet());
    }

    @Override
    public String getID() {
        return "";
    }

    @Override
    public Set<String> getObservedPhenotypes() {
        return this.includedTerms ;
    }

    @Override
    public Set<String> getExcludedPhenotypes() {
        return this.excludedTerms ;
    }
}
