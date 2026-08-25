package org.p2gx.boqa.core.analysis;

import java.util.Objects;
import java.util.Set;

/**
 * A candidate disease to be analyzed, together with the gene that made it a candidate.
 *
 * <p>The caller knows which gene put a disease on the candidate list, so gene identity is supplied
 * with the disease rather than looked up here. The gene fields are carried through to the
 * {@link BlendedResult}, so that the caller can build its own per-gene result objects without
 * boqa-core depending on the caller's types.</p>
 *
 * @param diseaseId    the disease ID as used in the HPO annotations, e.g. {@code OMIM:154700}
 * @param diseaseLabel the disease name, for display
 * @param geneId       the NCBI gene ID as a plain number, i.e. {@code 2639} for {@code NCBIGene:2639}
 * @param geneSymbol   the gene symbol, e.g. {@code GCDH}
 */
public record TargetDisease(String diseaseId, String diseaseLabel, int geneId, String geneSymbol, Set<String> observedHpoTermIdSet) {

    public TargetDisease {
        Objects.requireNonNull(diseaseId, "Disease ID must not be null!");
    }
}
