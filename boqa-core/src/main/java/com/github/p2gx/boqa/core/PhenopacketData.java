package com.github.p2gx.boqa.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.schema.v2.Phenopacket;
import org.phenopackets.schema.v2.core.Disease;
import org.phenopackets.schema.v2.core.OntologyClass;
import org.phenopackets.schema.v2.core.PhenotypicFeature;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This class reads in a Path to a phenopacket file and reads it in as a
 * {@link org.phenopackets.schema.v2.Phenopacket Phenopacket} object.
 * <p>
 * Observed and Excluded phenotypic features,
 * as well as the Phenopacket ID can be queried through {@link #getObservedTerms() getObservedTerms},
 * {@link #getExcludedTerms() getExcludedTerms}, and {@link #getID() getID}.
 * <p>
 * @author <a href="mailto:leonardo.chimirri@bih-charite.de">Leonardo Chimirri</a>
 */
public class PhenopacketData implements PatientData {

    private final Phenopacket ppkt;
    private record DiseaseDTO(String id, String label) {}

    // Primary constructor
    public PhenopacketData(Phenopacket phenopacket) {
        this.ppkt = phenopacket;
    }

    // Convenience constructor (still allow from file)
    public PhenopacketData(Path phenopacketFile) {
        this(PhenopacketReader.readPhenopacket(phenopacketFile));
    }

    @JsonProperty("diagnosis")
    public PhenopacketData.DiseaseDTO getDisease() {
        Disease disease = ppkt.getDiseasesList().getFirst();
        OntologyClass term = disease.getTerm();
        return new PhenopacketData.DiseaseDTO(term.getId(), term.getLabel());
    }

    @Override
    public Set<TermId> getObservedTerms() {
        return ppkt.getPhenotypicFeaturesList().stream()
                .filter(Predicate.not(PhenotypicFeature::getExcluded))
                .map(PhenotypicFeature::getType)
                .map(OntologyClass::getId)
                .map(TermId::of)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<TermId> getExcludedTerms() {
        return ppkt.getPhenotypicFeaturesList().stream()
                .filter(PhenotypicFeature::getExcluded)
                .map(PhenotypicFeature::getType)
                .map(OntologyClass::getId)
                .map(TermId::of)
                .collect(Collectors.toSet());
    }

    @Override
    public String getID() {
        return ppkt.getId();
    }
}
