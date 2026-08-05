package org.p2gx.boqa.core.analysis;

import org.p2gx.boqa.core.algorithm.BoqaCounts;
import org.p2gx.boqa.core.diseases.TargetDisease;

import java.util.List;

/**
 * One scored entry of a BOQA-blended analysis: either a single candidate disease or a blend of two.
 *
 * <p>For a blend, {@code components} holds both diseases with their genes and {@code componentCounts}
 * holds each component's counts in the same order, next to the {@code blendedCounts} of the entry as
 * a whole. A blend always spans two genes, since only diseases with different genes are blended, so
 * both are reported and the caller decides how to map them onto its own per-gene results.</p>
 *
 * <p>For a single disease, {@code components} and {@code componentCounts} hold one element each and
 * {@code blendedCounts} are that disease's own counts.</p>
 *
 * @param components      the disease(s) this entry is made of, in pairing order
 * @param componentCounts the BOQA counts of each component, in the same order as {@code components}
 * @param blendedCounts   the BOQA counts this entry was scored on: the counts of the blend for a
 *                        blended entry, the component's own counts for a single disease
 * @param score           the normalized BOQA probability score of this entry
 */
public record BlendedResult(List<TargetDisease> components, List<BoqaCounts> componentCounts,
                            BoqaCounts blendedCounts, double score) {

    public BlendedResult {
        if (components.isEmpty()) {
            throw new IllegalArgumentException("A result must be made of at least one disease!");
        }
        if (components.size() != componentCounts.size()) {
            throw new IllegalArgumentException("Got " + components.size() + " diseases but "
                    + componentCounts.size() + " counts; each disease needs its own counts!");
        }
        components = List.copyOf(components);
        componentCounts = List.copyOf(componentCounts);
    }

    /** Whether this entry blends two diseases, as opposed to being a single candidate disease. */
    public boolean isBlended() {
        return components.size() > 1;
    }
}
