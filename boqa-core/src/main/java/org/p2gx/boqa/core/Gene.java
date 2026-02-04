package org.p2gx.boqa.core;

import org.monarchinitiative.phenol.ontology.data.TermId;

/**
 * The information about a gene that we need in BOQA.
 */
public interface Gene {

    /**
     * Get gene's identifier (e.g. <code>HGNC:3603</code> for <em>FBN1</em>).
     */
    TermId id();

    /**
     * Get HGVS symbol of the gene (e.g. <em>FBN1</em>).
     */
    String symbol();

}
