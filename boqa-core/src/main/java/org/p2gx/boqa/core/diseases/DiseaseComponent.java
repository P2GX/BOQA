package org.p2gx.boqa.core.diseases;

import org.p2gx.boqa.core.algorithm.BoqaCounts;

/**
 * @deprecated, almost equivalent to BoqaResult
 * @param disease
 * @param counts
 * @param score
 */
public record DiseaseComponent(
    TargetDisease disease,
    BoqaCounts counts,
    double score
) {}