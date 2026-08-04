package org.p2gx.boqa.core.diseases;

import org.p2gx.boqa.core.CandidateDiagnosis;
import org.p2gx.boqa.core.DiseaseData;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.boqa.core.patient.SingleDiseaseInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that implements the {@code DiseaseData} interface by parsing disease annotations
 * from HPOA files using Phenol.
 *
 * <p><strong>Rules for defining observed and excluded terms of diseases:</strong></p>
 * <ol>
 *     <li>Only terms below <i>Phenotypic Abnormality</i> ({@code HP:0000118}) are taken into account.</li>
 *     <li>Terms associated with a given disease are considered <i>excluded</i> if they have a
 *         frequency of {@code 0}; otherwise, they are defined as <i>observed</i>.</li>
 * </ol>
 *
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 */
public class DiseaseDataPhenolIngest implements DiseaseData {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiseaseDataPhenolIngest.class);
    private static final int cohortSize = 100; // Imaginary cohort size using phenol to convert HPO frequency terms to ratios
    HpoDiseases diseases; // Temporarily needed to explore Phenols HpoDiseases, as there is no documentation

    List<CandidateDiagnosis> candidateDiagnosisList;
    private static final TermId PHENOTYPIC_ABNORMALITY = TermId.of("HP:0000118");
    private final Ontology hpo;

    public static DiseaseData of(Ontology hpo, HpoDiseases diseases) {
        return new DiseaseDataPhenolIngest(hpo, diseases);
    }

    private DiseaseDataPhenolIngest(Ontology hpo, HpoDiseases diseases){
        this.hpo = hpo;
        this.diseases = diseases;
        // Create dictionary using Phenol
        this.candidateDiagnosisList = phenolIngest();
    }

    /**
     * Loads OMIM disease data from HPOA files using Phenol.
     *
     * @param ontologyStream input stream for the HP ontology (JSON format)
     * @param annotationsStream input stream for the phenotype.hpoa file
     * @throws IOException if reading the streams fails
     */
    public DiseaseDataPhenolIngest(InputStream ontologyStream, InputStream annotationsStream) throws IOException {
        this(annotationsStream, ontologyStream, List.of("OMIM"));
    }

    /**
     * Loads disease data from HPOA files using Phenol, filtered by database sources ("OMIM", "ORPHA", "DECIPHER").
     *
     * @param annotationsStream input stream for the phenotype.hpoa file
     * @param ontologyStream input stream for the HP ontology (JSON format)
     * @param validDatabaseList list of database sources to include (valid sources: "OMIM", "ORPHA", "DECIPHER")
     * @throws IOException if reading the streams fails
     */
    public DiseaseDataPhenolIngest(InputStream annotationsStream,
                                   InputStream ontologyStream,
                                   List<String> validDatabaseList) // Valid databases are "OMIM", "ORPHA", and "DECIPHER"
            throws IOException{

        LOGGER.info("Ingesting HPOA file 'phenotype.hpoa' using Phenol ...");
        this.hpo = OntologyLoader.loadOntology(ontologyStream);
        this.diseases = getPhenolHpoDiseases(hpo, annotationsStream, validDatabaseList);
        // Create dictionary using Phenol
        this.candidateDiagnosisList = phenolIngest();
    }

    private HpoDiseases getPhenolHpoDiseases(Ontology hpo, InputStream phenotypeAnnotations, List<String> validDatabaseList) throws IOException {
        /*
        Code required to get a kind of list of HpoDisease objects in Phenol from the HPOA file phenotype.hpoa
        and the HP ontology in JSON format.
         */
        Set<DiseaseDatabase> DiseaseDatabaseSet = validDatabaseList.stream()
                .map(DiseaseDatabase::fromString)
                .collect(Collectors.toSet());
        HpoDiseaseLoaderOptions options = HpoDiseaseLoaderOptions.of(DiseaseDatabaseSet,false, cohortSize);
        HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(hpo, options);
        return loader.load(phenotypeAnnotations);
    }

    private List<CandidateDiagnosis> phenolIngest() {
        /*
        Use phenol to construct a dictionary that contains, for each disease, associated features and explicitly
        non-associated features.
         */
        List<CandidateDiagnosis> candidateDiagnosisList = new ArrayList<>();

        // Filter for phenotypic abnormality terms
        Set<TermId> phenotypicAbnormalities = Set.copyOf(hpo.graph().getDescendantSet(PHENOTYPIC_ABNORMALITY));

        for (HpoDisease disease : this.diseases) {

            // Observed
            Set<String> observedTerms = disease.annotationTermIdList().stream()
                    .filter(phenotypicAbnormalities::contains)
                    .filter(termId -> disease.getFrequencyOfTermInDisease(termId).orElseThrow().numerator() != 0)
                    .map(TermId::toString)
                    .collect(Collectors.toSet());

            // Excluded
            Set<String> excludedTerms = disease.annotationTermIdList().stream()
                    .filter(phenotypicAbnormalities::contains)
                    .filter(termId -> disease.getFrequencyOfTermInDisease(termId).orElseThrow().numerator() == 0)
                    .map(TermId::toString)
                    .collect(Collectors.toSet());

            candidateDiagnosisList.add(
                    new CandidateDiagnosis(List.of(
                            new SingleDiseaseInfo(disease.id().toString(), disease.diseaseName())),
                            observedTerms,
                            excludedTerms
                    )
            );
        }
        return candidateDiagnosisList;
    }

    /**
     * Temporarily needed to explore Phenols HpoDiseases, as there is no documentation.
     */
    public HpoDiseases getDiseases() {
        return this.diseases;
    }

    /**
     Methods that implement the DiseaseDict interface
     */
    @Override
    public int size() {
        return this.candidateDiagnosisList.size();
    }

    //TODO could it be that in the end DiseaseData is simply a data container? If so:
    // TODO should it be a record? What should it expose?
    @Override
    public List<CandidateDiagnosis> getCandidateDiagnosisList() {
        return this.candidateDiagnosisList;
    }
    @Override
    public List<List<SingleDiseaseInfo>> getDiagnosisIds() {
        return this.candidateDiagnosisList.stream()
                .map(CandidateDiagnosis::diseasesInfo)
                .collect(Collectors.toList());
    }

    //TODO having this tested is not bad, but how does this change?
//    @Override
//    public Set<String> getObservedDiseaseFeatures(DiseaseDTO diseaseId) {
//        if (this.diseaseFeaturesDict.containsKey(diseaseId)) {
//            return this.diseaseFeaturesDict.get(diseaseId).get("I");
//        } else {
//            throw new IllegalArgumentException("Disease ID \"" + diseaseId.id() + "\" not found!");
//        }
//    }
//
//    @Override
//    public Set<String> getExcludedDiseaseFeatures(DiseaseDTO diseaseId){
//        if (this.diseaseFeaturesDict.containsKey(diseaseId)) {
//            return this.diseaseFeaturesDict.get(diseaseId).get("E");
//        } else {
//            throw new IllegalArgumentException("Disease ID \"" + diseaseId.id() + "\" not found!");
//        }
//    }
}
