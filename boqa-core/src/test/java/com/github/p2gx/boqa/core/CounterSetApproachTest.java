package com.github.p2gx.boqa.core;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CounterSetApproachTest {

    public static Counter counter;

    @BeforeAll
    static void setUp() throws IOException {
        //Path ppkt = Path.of(PhenopacketReaderTest.class.getResource("PMID_30569521_proband.json").getPath());
        //counter = new CounterSetApproach();
    }

    @Test
    void initQueryLayer() {
        //necessary? Or maybe test this instead of initLayer
    }

    @Test
    void initLayer() {
    }

    @Test //TODO should we try ParametrizedTest here?
    void allParentsActive() {
        // HP:0012718 has two parents: HP:0011024 and HP:0025033, try both off, one on and one off, both on
    }

    @Test
    void computeBoqaCounts() {
    }

    @Test
    void getDiseaseIds() {
    }
}