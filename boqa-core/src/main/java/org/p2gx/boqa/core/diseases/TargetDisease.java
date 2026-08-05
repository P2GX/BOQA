package org.p2gx.boqa.core.diseases;
import org.p2gx.boqa.core.analysis.BlendedResult;

import java.util.Objects;

/* Interface with Exomiser that contains all data we need to make an Exomiser result.
We do not want to import the actual Exomiser library, but we want to make it easy to create objects that will
implement the PriorityResult interface from Exomiser. These objects have
1. int geneId();
2. String geneSymbol();
3. double score();
4. PriorityType priorityType();
5. default String getHTMLCode() {return "";}
6. Therefore, we need to get this information from the Exomiser

TODO This should not be used everywhere in BOQA, just at the boundary with the outside world
*/
/**
 * A candidate disease to be analyzed. In Exomiser context, together with the gene that made it a candidate.
 *
 * <p>The caller knows which gene put a disease on the candidate list, so gene identity is supplied
 * with the disease rather than looked up here. The gene fields are carried through to the
 * {@link BlendedResult}, so that the caller can build its own per-gene result objects without
 * boqa-core depending on the caller's types.</p>
 *
 * @param diseaseId    A disease ID as used in the HPO annotations, e.g. {@code OMIM:154700}
 * @param diseaseLabel A disease name such as Marfan syndrome, for display
 * @param geneId       An NCBI gene ID as a plain number, i.e. {@code 2639} for {@code NCBIGene:2639}
 *                     TODO or maybe A gene identifier, such as HGNC:3603, ENSG00000166147, or NCBIGene:2200?
 * @param geneSymbol   A symbol for the gene such as {@code GCDH}
 *
 * @remark We allow for null geneId and geneSymbol, so BOQA can be used as phenotype-only prioritization tool
 * @todo maybe use sealed interface, disease with gene and without it
 */
public record TargetDisease(
    String diseaseId,
    String diseaseLabel,
    String geneId,
    String geneSymbol
) {
    public TargetDisease {
        Objects.requireNonNull(diseaseId, "diseaseId cannot be null");
        Objects.requireNonNull(diseaseLabel, "diseaseLabel cannot be null");
        Objects.requireNonNull(geneId, "geneId cannot be null");
        Objects.requireNonNull(geneSymbol, "geneSymbol cannot be null");
    }
}
