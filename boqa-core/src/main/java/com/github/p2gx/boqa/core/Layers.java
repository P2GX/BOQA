package com.github.p2gx.boqa.core;

import java.util.Set;

/**
 * Provides initialized layers for all diseases of a given HPO release.
 * * <p>
 * Could be serialized to improve runtime.
 * <p>
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 * @author <a href="mailto:leonardo.chimirri@bih-charite.de">Leonardo Chimirri</a>
 */
public interface Layers {

    // Returns set of HPO terms associated with disease plus the terms of all ancestors (annotation propagation rule).
    // Layers for all diseases are created when an object of a class that implements this interface is initialized.
    Set<String> getIncludedDiseaseLayer(String diseaseId);

    // Returns set of HPO terms explicitly not associated with disease plus the terms of all descendants.
    // Layers for all diseases are created when an object of a class that implements this interface is initialized.
    Set<String> getExcludedDiseaseLayer(String diseaseId);

    // Returns set of input terms plus the terms of all descendants
    // Is created on the fly (expensive)
    // Required to create query layers
    Set<String> getIncludedLayer(Set<String> termIDs);

    // Returns set of input terms plus the terms of all descendants
    // Is created on the fly (expensive)
    // Required to create query layers
    Set<String> getExcludedLayer(Set<String> termIDs);
}