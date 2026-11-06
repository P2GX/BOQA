package org.p2gx.boqa.core.algorithm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class AlgorithmParametersTest {

    @ParameterizedTest
    @CsvSource({"0.0, 0.5", "1.0, 0.5", "-0.1, 0.5"})
    void alphaOutsideRange_throws(double alpha, double beta) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AlgorithmParameters.create(alpha, beta));
        assertTrue(exception.getMessage().startsWith("Alpha must be in the range"));
    }

    @ParameterizedTest
    @CsvSource({"0.5, 0.0", "0.5, 1.0", "0.5, -0.1"})
    void betaOutsideRange_throws(double alpha, double beta) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AlgorithmParameters.create(alpha, beta));
        assertTrue(exception.getMessage().startsWith("Beta must be in the range"));
    }

    @ParameterizedTest
    @CsvSource({"0.5, 0.5", "0.1, 0.9", "0.3, 0.7", "0.5, 0.6", "1.0E-7, 0.9999999"})
    void alphaPlusBetaAtLeastOne_throws(double alpha, double beta) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AlgorithmParameters.create(alpha, beta));
        assertTrue(exception.getMessage().startsWith("Alpha + beta must be less than 1"));
    }

    @ParameterizedTest
    @CsvSource({"0.49, 0.5", "0.01, 0.9", "0.2, 0.3"})
    void alphaPlusBetaBelowOne_isAccepted(double alpha, double beta) {
        AlgorithmParameters params = AlgorithmParameters.create(alpha, beta);
        assertEquals(alpha, params.getAlpha());
        assertEquals(beta, params.getBeta());
    }

    @Test
    void defaultParams_areAccepted() {
        assertDoesNotThrow(AlgorithmParameters::defaultParams);
    }
}
