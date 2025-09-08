package com.github.p2gx.boqa.core.patient;
import com.github.p2gx.boqa.core.PatientData;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that implements the QueryData interface by parsing comma-separated strings of HPO terms.
 * <p>
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 */
public class QueryDataFromString implements PatientData {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueryDataFromString.class);

    Set<TermId> observedTerms;
    Set<TermId> excludedTerms;

    public QueryDataFromString(String includedTermListString, String excludedTermListString) {
        this.observedTerms = Arrays.asList(includedTermListString.split(",")).stream()
                .filter(t -> t.matches("HP:\\d{7}"))
                .map(TermId::of)
                .collect(Collectors.toSet());
        this.excludedTerms = Arrays.asList(excludedTermListString.split(",")).stream()
                .filter(t -> t.matches("HP:\\d{7}"))
                .map(TermId::of)
                .collect(Collectors.toSet());
    }

    @Override
    public String getID() {
        return "";
    }

    @Override
    public Set<TermId> getObservedTerms() {
        return this.observedTerms ;
    }

    @Override
    public Set<TermId> getExcludedTerms() {
        return this.excludedTerms ;
    }
}
