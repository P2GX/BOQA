package org.p2gx.boqa.core.algorithm;

/**
 * Thrown when a patient's query has no observed term below Phenotypic abnormality (HP:0000118).
 * Every disease would then get the same score.
 */
public class EmptyQueryException extends IllegalArgumentException {

    public EmptyQueryException(String message) {
        super(message);
    }
}
