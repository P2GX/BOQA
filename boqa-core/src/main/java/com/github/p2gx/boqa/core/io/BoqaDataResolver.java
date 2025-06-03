package com.github.p2gx.boqa.core.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class BoqaDataResolver {


    private static final Logger LOGGER = LoggerFactory.getLogger(BoqaDataResolver.class);

    private final Path dataDirectory;

    public static BoqaDataResolver of(Path dataDirectory) throws BoqaDataException {
        return new BoqaDataResolver(dataDirectory);
    }

    public BoqaDataResolver(Path dataDirectory) throws BoqaDataException {
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "Data directory must not be null!");
        checkV1Resources();
    }

    private void checkV1Resources() throws BoqaDataException {
        boolean error = false;
        List<Path> requiredFiles = List.of(hpoJsonFile(), phenotypeAnnotationFile(), geneAnnotationFile());
        for (Path file : requiredFiles) {
            if (!Files.isRegularFile(file)) {
                LOGGER.error("Missing required file `{}` in `{}`.", file.toFile().getName(), dataDirectory.toAbsolutePath());
                error = true;
            }
        }
        if (error) {
            throw new BoqaDataException("Missing one or more resource files in boqa data directory!");
        }
    }

    public Path dataDirectory() {
        return dataDirectory;
    }

    public Path hpoJsonFile() {
        return dataDirectory.resolve("hp.json");
    }

    public Path phenotypeAnnotationFile() {
        return dataDirectory.resolve("phenotype.hpoa");
    }

    public Path geneAnnotationFile() {
        return dataDirectory.resolve("genes_to_disease.txt");
    }

}