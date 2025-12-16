package org.p2gx.boqa.core.patient;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.p2gx.boqa.core.PatientData;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.boqa.core.internal.OntologyTraverser;
import org.phenopackets.schema.v2.Phenopacket;
import org.phenopackets.schema.v2.core.OntologyClass;
import org.phenopackets.schema.v2.core.PhenotypicFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This class is constructed with a {@link org.phenopackets.schema.v2.Phenopacket Phenopacket}
 * or a Path to a phenopacket file, read with {@link PhenopacketReader#readPhenopacket(Path)}.
 * <p>
 * Observed and Excluded phenotypic features,
 * as well as the Phenopacket ID can be queried through {@link #getObservedTerms() getObservedTerms},
 * {@link #getExcludedTerms() getExcludedTerms}, and {@link #getID() getID}.
 * <p>
 * {@link #getDiseases()} returns a list of {@link DiseaseDTO} records containing OMIM ID and label.
 * This list can contain no diseases (unknown, unclear), one disease (standard Mendelian disease)
 * or any number of diseases (blended phenotype).
 * <p>
 * @author <a href="mailto:leonardo.chimirri@bih-charite.de">Leonardo Chimirri</a>
 */
public class PhenopacketData implements PatientData {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhenopacketData.class);
    private final String ppktId;
    private final Set<TermId> observedTerms;
    private final Set<TermId> excludedTerms;
    private final List<DiseaseDTO> diseases;

    record DiseaseDTO(String id, String label) {}

    // Primary constructor
    public PhenopacketData(Phenopacket phenopacket) {
        this.ppktId = phenopacket.getId();
        this.observedTerms =  phenopacket.getPhenotypicFeaturesList().stream()
                .filter(Predicate.not(PhenotypicFeature::getExcluded))
                .map(PhenotypicFeature::getType)
                .map(OntologyClass::getId)
                .map(TermId::of)
                .map(OntologyTraverser::getPrimaryTermId)
                .collect(Collectors.toSet());
        if (this.observedTerms.isEmpty()) {
            LOGGER.warn("Phenopacket {} has no observed phenotypic features!", phenopacket.getId());
        }        this.excludedTerms = phenopacket.getPhenotypicFeaturesList().stream()
                .filter(PhenotypicFeature::getExcluded)
                .map(PhenotypicFeature::getType)
                .map(OntologyClass::getId)
                .map(TermId::of)
                .map(OntologyTraverser::getPrimaryTermId)
                .collect(Collectors.toSet());
        this.diseases = phenopacket.getDiseasesList().stream().map(d ->
                new DiseaseDTO(d.getTerm().getId(), d.getTerm().getLabel())).toList();
    }

    // Convenience constructor (allow from file)
    public PhenopacketData(Path phenopacketFile) {
        this(PhenopacketReader.readPhenopacket(phenopacketFile));
    }

    @JsonProperty("diagnosis")
    List<DiseaseDTO> getDiseases() {
        return List.copyOf(diseases);
    }

    @Override
    public Set<TermId> getObservedTerms() {
        return Set.copyOf(observedTerms);
    }

    @Override
    public Set<TermId> getExcludedTerms() {
        return Set.copyOf(excludedTerms);
    }

    @Override
    public String getID() {
        return ppktId;
    }
}
