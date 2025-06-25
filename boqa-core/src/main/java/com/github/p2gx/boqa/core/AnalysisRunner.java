package com.github.p2gx.boqa.core;

import java.util.Set;

public class AnalysisRunner implements Analysis {
    private final Set<String> observedHPOs;
    private final String patientID;

    public AnalysisRunner(PatientData phenopacket) {
        observedHPOs = phenopacket.getPhenotypes();
        patientID = phenopacket.getID();
    }
}
