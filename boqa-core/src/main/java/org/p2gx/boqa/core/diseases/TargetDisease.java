package org.p2gx.boqa.core.diseases;
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
*/
final public record TargetDisease(
    /** A database identifier such as OMIM:154700 */
    String diseaseId,
    /** A disease name such as Marfan syndrome */
    String diseaseLabel,
    /** A gene identifier, such as HGNC:3603, ENSG00000166147, or NCBIGene:2200 */
    String geneId,
    /** A symbol for the gene such as FBN1 */
    String geneSymbol 
) {
    public TargetDisease {
        Objects.requireNonNull(diseaseId, "diseaseId cannot be null");
        Objects.requireNonNull(diseaseLabel, "diseaseLabel cannot be null");
        Objects.requireNonNull(geneId, "geneId cannot be null");
        Objects.requireNonNull(geneSymbol, "geneSymbol cannot be null");
    }
}
