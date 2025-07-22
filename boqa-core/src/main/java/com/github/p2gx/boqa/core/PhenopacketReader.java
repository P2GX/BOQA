package com.github.p2gx.boqa.core;

import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.phenopackets.schema.v2.Phenopacket;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import com.google.protobuf.util.JsonFormat;
import org.json.simple.parser.ParseException;
import org.phenopackets.schema.v2.core.*;

public class PhenopacketReader implements PatientData {

    private final Logger LOGGER = LoggerFactory.getLogger(PhenopacketReader.class);
    private final Phenopacket ppkt;
    private final Set<TermId> observedHPOs;
    private final Set<TermId> excludedHPOs;
    private final String ppktID;

    /**
     * This class reads in a Path to a phenopacket file and reads it in as a
     * {@link org.phenopackets.schema.v2.Phenopacket Phenopacket} object.
     * <p>
     * Observed and Excluded phenotypic features,
     * as well as the Phenopacket ID can be queried through {@link #getObservedTerms() getObservedTerms},
     * {@link #getExcludedTerms() getExcludedTerms}, and {@link #getID() getID}.
     * @param phenopacketFile
     * <p>
     * @author <a href="mailto:leonardo.chimirri@bih-charite.de">Leonardo Chimirri</a>
     */
    public PhenopacketReader(Path phenopacketFile) {
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(String.valueOf(phenopacketFile)));
            JSONObject jsonObject = (JSONObject) obj;
            String phenopacketJsonString = jsonObject.toJSONString();
            Phenopacket.Builder phenoPacketBuilder = Phenopacket.newBuilder();
            JsonFormat.parser().merge(phenopacketJsonString, phenoPacketBuilder);
            this.ppkt = phenoPacketBuilder.build();
        } catch (IOException e) {
            LOGGER.error("I/O error while loading phenopacket: {}", e.getMessage(), e);
            throw new PhenolRuntimeException("I/O failure", e);
        } catch (ParseException e) {
            LOGGER.error("Could not ingest phenopacket ({}): {}", e.getClass().getSimpleName(), e.getMessage());
            throw new PhenolRuntimeException("Phenopacket parsing failure at " + phenopacketFile, e);
        }

        this.ppktID = ppkt.getId();
        this.observedHPOs = ppkt.getPhenotypicFeaturesList().stream()
                .filter(Predicate.not(PhenotypicFeature::getExcluded))
                .map(PhenotypicFeature::getType)
                .map(OntologyClass::getId)
                .map(TermId::of)
                .collect(Collectors.toSet());
        this.excludedHPOs = ppkt.getPhenotypicFeaturesList().stream()
                .filter(PhenotypicFeature::getExcluded)
                .map(PhenotypicFeature::getType)
                .map(OntologyClass::getId)
                .map(TermId::of)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<TermId> getObservedTerms() {
        return observedHPOs;
    }

    @Override
    public Set<TermId> getExcludedTerms() {
        return excludedHPOs;
    }

    @Override
    public String getID() {
        return ppktID;
    }
}
