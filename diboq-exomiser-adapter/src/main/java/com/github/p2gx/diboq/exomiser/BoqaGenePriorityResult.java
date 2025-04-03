package com.github.p2gx.diboq.exomiser;

import org.monarchinitiative.exomiser.core.prioritisers.PriorityResult;
import org.monarchinitiative.exomiser.core.prioritisers.PriorityType;

public class BoqaGenePriorityResult implements PriorityResult {
    @Override
    public int getGeneId() {
        return 0;
    }

    @Override
    public String getGeneSymbol() {
        return "";
    }

    @Override
    public double getScore() {
        return 0;
    }

    @Override
    public PriorityType getPriorityType() {
        return null;
    }
}
