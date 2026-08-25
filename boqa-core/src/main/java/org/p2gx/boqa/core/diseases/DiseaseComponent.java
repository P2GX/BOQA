package org.p2gx.boqa.core.diseases;

import org.p2gx.boqa.core.algorithm.BoqaCounts;

final public record DiseaseComponent(
    TargetDisease disease,
    BoqaCounts counts,
    double score
) {}