package org.p2gx.boqa.core;

import org.p2gx.boqa.core.patient.DiseaseDTO;

import java.util.Set; /**
 * TODO change the below
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
public record CandidateDiagnosis(Set<DiseaseDTO> diseasesInfo, Set<String> observedPhenotypes, Set<String> excludedPhenotypes) {
}

