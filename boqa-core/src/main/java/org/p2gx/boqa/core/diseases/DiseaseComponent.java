package org.p2gx.boqa.core.diseases;

import org.p2gx.boqa.core.PatientData;
import org.p2gx.boqa.core.algorithm.BoqaCountsNew;

import java.util.List;

public record DiseaseComponent(
    TargetDisease disease,
    BoqaCountsNew counts,
    double score
) {}
