package org.p2gx.boqa.core.diseases;

import org.p2gx.boqa.core.algorithm.BoqaCounts;

public record DiseaseComponent(
    ExomiserTargetDisease disease,
    BoqaCounts counts,
    double score
) {}