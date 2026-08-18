package org.p2gx.boqa.core.diseases;

import org.p2gx.boqa.core.algorithm.BoqaCountsNew;

public record DiseaseComponent(
    TargetDisease disease,
    BoqaCountsNew counts,
    double score
) {}