package org.p2gx.boqa.core.analysis;

import java.util.Collections;
import java.util.List;

public class Util {
     /**
     * Transforms a list of BOQA results by rescaling their raw log scores into the range [0, 1].
     *
     * <p>The transformation is done as follows:</p>
     * <pre>
     * boqaExomiserScore_i =
     *     (boqaRawLogScore_i + abs(min(boqaRawLogScore)))
     *     / (max(boqaRawLogScore) + abs(min(boqaRawLogScore)))
     * </pre>
     *
     * <p>This ensures that the minimum raw score maps to 0, and the maximum maps to 1.</p>
     *
     * @param boqaResults the list of BOQA results to rescale
     * @TODO move to an exomiser adapter module, as originally decided?
     * @return a list of BOQA results with rescaled scores
     */
    public static List<BoqaResult> reScaledRawLogBoqaExomiserScores(List<BoqaResult> boqaResults) {

        // Extract raw BOQA log scores
        List<Double> rawLogBoqaScores =
                boqaResults.stream()
                        .map(BoqaResult::boqaScore)
                        .toList();

        // Compute offset and normalization factor
        double offset = Math.abs(Collections.min(rawLogBoqaScores));
        double scale = Collections.max(rawLogBoqaScores) + offset;

        // Rescale
        return boqaResults.stream()
                .map(br -> {
                    double boqaExomiserScore = (br.boqaScore() + offset) / scale;
                    return new BoqaResult(br.counts(), boqaExomiserScore);
                })
                .toList();
    }

}
