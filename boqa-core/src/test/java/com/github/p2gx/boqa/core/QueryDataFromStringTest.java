package com.github.p2gx.boqa.core;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class QueryDataFromStringTest {

    @Test
    void testGetObservedTerms() {
        PatientData queryData = new QueryDataFromString("HP:0040281,HP:0040281,HP:0040282", "HP:0040283,HP:0040283,HP:0040284");
        Set<TermId> observedTermsExpected = Set.of(TermId.of("HP:0040281"),TermId.of("HP:0040282"));
        Set<TermId> observedTermsActual = queryData.getObservedTerms();
        assertEquals(observedTermsExpected, observedTermsActual);
    }

    @Test
    void testGetExcludedTerms() {
        PatientData queryData = new QueryDataFromString("HP:0040281,HP:0040281,HP:0040282", "HP:0040283,HP:0040283,HP:0040284");
        Set<TermId> excludedTermsExpected = Set.of(TermId.of("HP:0040283"),TermId.of("HP:0040284"));
        Set<TermId> excludedTermsActual = queryData.getExcludedTerms();
        assertEquals(excludedTermsExpected, excludedTermsActual);
    }
}