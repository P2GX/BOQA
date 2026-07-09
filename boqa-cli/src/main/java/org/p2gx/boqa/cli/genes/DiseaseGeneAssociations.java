package org.p2gx.boqa.cli.genes;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads disease-gene associations from a {@code genes_to_disease.txt} file
 * (tab-separated, with header) and exposes gene ID lookups.
 */
public class DiseaseGeneAssociations {

    private final Map<String, Set<String>> geneIdsByDisease = new HashMap<>();

    private DiseaseGeneAssociations() {
    }

    public static DiseaseGeneAssociations fromFile(Path geneAssociationsFile) throws IOException {
        try (InputStream stream = new BufferedInputStream(Files.newInputStream(geneAssociationsFile))) {
            return fromStream(stream);
        }
    }

    public static DiseaseGeneAssociations fromStream(InputStream geneAssociationsStream) throws IOException {
        DiseaseGeneAssociations associations = new DiseaseGeneAssociations();
        associations.load(geneAssociationsStream);
        return associations;
    }

    private void load(InputStream geneAssociationsStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(geneAssociationsStream))) {
            reader.lines()
                    .skip(1) // Skip header line
                    .map(line -> line.split("\t"))
                    .filter(cols -> cols.length >= 4)
                    .forEach(cols -> {
                        String geneId = cols[0];
                        String diseaseId = cols[3];
                        geneIdsByDisease.computeIfAbsent(diseaseId, k -> new HashSet<>()).add(geneId);
                    });
        }
    }

    // Return gene IDs associated with a disease
    public Set<String> geneIdsForDisease(String diseaseId) {
        return geneIdsByDisease.getOrDefault(diseaseId, Set.of());
    }

    // Return IDs of diseases associated with any of the given genes
    public Set<String> diseaseIdsForGenes(Collection<String> geneIds) {
        return geneIdsByDisease.entrySet().stream()
                .filter(entry -> !Collections.disjoint(entry.getValue(), geneIds))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    // Return all gene IDs associated with any disease
    public Set<String> allGeneIds() {
        return geneIdsByDisease.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }
}
