package com.github.p2gx.diboq.core;

import org.monarchinitiative.phenol.ontology.data.Identified;

/**
 * The information about a gene that we need in BOQA.
 */
public interface Gene extends Identified {

    /**
     * Get HGVS symbol of the gene (e.g. <em>FBN1</em>).
     */
    String symbol();

}
