package com.github.p2gx.boqa.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.phenopackets.schema.v2.Phenopacket;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import com.google.protobuf.util.JsonFormat;
import org.json.simple.parser.ParseException;
import org.phenopackets.schema.v2.core.*;

public class PhenopacketReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhenopacketReader.class);

    /**
     *
     * @param phenopacketFile
     */
    public static Phenopacket readPhenopacket(Path phenopacketFile) {
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new FileReader(String.valueOf(phenopacketFile)));
            JSONObject jsonObject = (JSONObject) obj;
            String phenopacketJsonString = jsonObject.toJSONString();
            Phenopacket.Builder phenoPacketBuilder = Phenopacket.newBuilder();
            JsonFormat.parser().merge(phenopacketJsonString, phenoPacketBuilder);
            return phenoPacketBuilder.build();
        } catch (IOException e) {
            LOGGER.error("I/O error while loading phenopacket: {}", e.getMessage(), e);
            throw new PhenolRuntimeException("I/O failure", e);
        } catch (ParseException e) {
            LOGGER.error("Could not ingest phenopacket ({}): {}", e.getClass().getSimpleName(), e.getMessage());
            throw new PhenolRuntimeException("Phenopacket parsing failure at " + phenopacketFile, e);
        }
    }
}
