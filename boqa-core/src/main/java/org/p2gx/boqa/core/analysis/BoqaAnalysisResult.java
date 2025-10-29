package org.p2gx.boqa.core.analysis;

import org.p2gx.boqa.core.PatientData;
import java.util.List;
/**
 * Bookkeeping record that holds the results of a Boqa Analysis for a single patient.
 *
 * <p>This record stores:
 * <ul>
 *   <li>The input {@link PatientData} used for analysis.</li>
 *   <li>A List of computed {@link org.p2gx.boqa.core.analysis.BoqaResult}, one per disease,
 *   each containing a {@link org.p2gx.boqa.core.algorithm.BoqaCounts} and its associated BOQA score.
 * </ul>
 **/
public record BoqaAnalysisResult(PatientData patientData, List<BoqaResult> boqaResults) {
}
