package com.github.p2gx.boqa.core;

import org.monarchinitiative.phenol.base.PhenolRuntimeException;
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
    private final Set<String> observedHPOs;
    private final String ppktID;

    public PhenopacketReader(Path phenopacketFile) throws IOException {
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(String.valueOf(phenopacketFile)));
            JSONObject jsonObject = (JSONObject) obj;
            String phenopacketJsonString = jsonObject.toJSONString();
            Phenopacket.Builder phenoPacketBuilder = Phenopacket.newBuilder();
            JsonFormat.parser().merge(phenopacketJsonString, phenoPacketBuilder);
            this.ppkt = phenoPacketBuilder.build();
        } catch (IOException | ParseException e1) {
            LOGGER.error("Could not ingest phenopacket: {}", e1.getMessage());
            throw new PhenolRuntimeException("Could not load phenopacket at " + phenopacketFile);
        }
        this.ppktID = ppkt.getId();
        this.observedHPOs = ppkt.getPhenotypicFeaturesList().stream()
                .filter(Predicate.not(PhenotypicFeature::getExcluded))
                .map(PhenotypicFeature::getType)
                .map(OntologyClass::getId)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getPhenotypes() {
        return observedHPOs;
    }

    @Override
    public String getID() {
        return ppktID;
    }
}
