package com.github.p2gx.diboq.exomiser;

import org.monarchinitiative.exomiser.core.model.Gene;
import org.monarchinitiative.exomiser.core.prioritisers.Prioritiser;
import org.monarchinitiative.exomiser.core.prioritisers.PriorityType;

import java.util.List;
import java.util.stream.Stream;

/**
 * This would be an example BOQA adapter of Exomiser's Prioritisers interface.
 * <p>
 * The adapter needs the BOQA algorithm plus info how to translate from Exomiser's gene, HPO String into BOQA domain
 * and then how to map the results back into `BoqaGenePriorityResult` objects.
 * <p>
 * Note, this adapter is for demonstration only. Exomiser's Prioritiser interface only works for prioritisation of genes,
 * while BOQA is designed to prioritise diseases (diagnoses?). Therefore, a new interface must be added to Exomiser
 * to support BOQA.
 */
public class BoqaPrioritiserAdapter implements Prioritiser<BoqaGenePriorityResult> {

    public BoqaPrioritiserAdapter() {
        // Ask for the info we need to perform prioritization. Most likely an implementation of the algorithm
    }

    @Override
    public Stream<BoqaGenePriorityResult> prioritise(List<String> hpoIds, List<Gene> list) {
        throw new RuntimeException("Not yet implemented!");
    }

    @Override
    public PriorityType getPriorityType() {
        throw new RuntimeException("Cannot produce a PriorityType value for BoqaPrioritiser");
    }
}
