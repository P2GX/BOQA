package com.github.p2gx.boqa.core.patient;

import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;

import org.phenopackets.schema.v2.Phenopacket;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import com.google.protobuf.util.JsonFormat;
import org.json.simple.parser.ParseException;

public class PhenopacketReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhenopacketReader.class);

    /**
     * Reads a Phenopacket from the given JSON file.
     *
     * @param phenopacketFile the path to the JSON file containing the phenopacket
     * @return a {@link Phenopacket} object representing the patient data
     * @throws PhenolRuntimeException if there is an I/O error or the JSON cannot be parsed
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
