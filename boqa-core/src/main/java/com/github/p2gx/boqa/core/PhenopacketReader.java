package com.github.p2gx.boqa.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.nio.file.Files.lines; // ?

public class PhenopacketReader implements PatientData {
    private static final Logger LOGGER = LoggerFactory.getLogger(PhenopacketReader.class);
    HashMap<String, Set<String>> phenopacketData;
    //HashMap<String, HashMap<String, Set<String>>> phenopacketData;

    public PhenopacketReader(Path phenopacketFile) throws IOException {
        // phenopacketFile is a path to a JSON or a text file with list of absolute paths to phenopackets
        try {
            this.phenopacketData = ingest(phenopacketFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public HashMap<String, Set<String>> ingest(Path phenopacketFile) throws IOException {
        HashMap<String, Set<String>> phenopacketData = new HashMap<>();
        if (isJsonFile(phenopacketFile)) {
            phenopacketData.putAll(processPhenopacket(phenopacketFile));
        } else {
            try {
                lines(phenopacketFile).map(Path::of).forEach(p -> {
                    phenopacketData.putAll(processPhenopacket(p));
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return phenopacketData;
    }

    private HashMap<String, Set<String>>  processPhenopacket(Path p) {
        // A JSON ile processor that extracts phenopacket ID and observed HPOs
        return (HashMap<String, Set<String>>) Set.of();
    }

    boolean isJsonFile (Path phenopacketFile){
        return phenopacketFile.toString().toLowerCase().endsWith(".json");
    }
    //
    @Override
    public Map<String, Set<String>> getPhenotypes() {
        // Just a getter!
        return (Map<String, Set<String>>) Set.of();
    }
}
