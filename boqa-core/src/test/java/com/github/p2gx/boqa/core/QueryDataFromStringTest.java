package com.github.p2gx.boqa.core;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class QueryDataFromStringTest {

    @Test
    void testGetIncludedTerms() {
        QueryData queryData = new QueryDataFromString("HP:0040281,HP:0040281,HP:0040282", "HP:0040283,HP:0040283,HP:0040284");
        Set<String> includedTermsExpected = Set.of("HP:0040281","HP:0040282");
        Set<String> includedTermsActual = queryData.getIncludedTerms();
        assertEquals(includedTermsExpected, includedTermsActual);
    }

    @Test
    void testGetExcludedTerms() {
        QueryData queryData = new QueryDataFromString("HP:0040281,HP:0040281,HP:0040282", "HP:0040283,HP:0040283,HP:0040284");
        Set<String> excludedTermsExpected = Set.of("HP:0040283","HP:0040284");
        Set<String> excludedTermsActual = queryData.getExcludedTerms();
        assertEquals(excludedTermsExpected, excludedTermsActual);
    }
}