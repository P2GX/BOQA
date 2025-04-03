package com.github.p2gx.diboq.core;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class GeneTest {

    @Test
    void testGene() {
        Gene gene = null;

        assertThat(gene, is(nullValue()));
    }
}