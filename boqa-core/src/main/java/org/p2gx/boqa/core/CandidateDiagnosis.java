package org.p2gx.boqa.core;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.p2gx.boqa.core.diseases.BlendedDiseaseData;
import org.p2gx.boqa.core.diseases.CandidateDisease;
import org.p2gx.boqa.core.diseases.TargetDisease;
import org.p2gx.boqa.core.patient.SingleDiseaseInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
/**
 * TODO change and update the description below
 * Diagnosis has a score, name, explanation,
 * one or more diseases, genes and the supporting variants.
 * <p>
 * In some cases , there is a mutation in a single gene that leads to a disease.
 * However, in other more arguably interesting cases,
 * mutations in two or more genes can lead to two or more diseases
 * that present as "melded phenotype".
 * <p>
 * Here we summarize the information to present to the user.
 */
public record CandidateDiagnosis(List<SingleDiseaseInfo> diseasesInfo, Set<String> observedPhenotypes) {
}

