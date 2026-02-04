# BOQA

Bayesian Ontology Query Algorithm ([PMID:22843981](https://pubmed.ncbi.nlm.nih.gov/22843981/))

## Run tests

We use Maven Surefire plugin to run tests:

```shell
./mvnw test
```
## Distribute CLI

The compilation of JAR files, generation of sources and Javadoc, as well as packaging into distribution ZIP file
requires activation of the `release` profile:

```shell
./mvnw -Prelease package 
```
## Input data

In BOQA analyses, observed phenotypic features of individuals are compared
with annotated disease-phenotype associations.
We use the following [HPO resources](https://github.com/obophenotype/human-phenotype-ontology):

**1. Disease-phenotype associations: `phenotype.hpoa`**

**2. HPO: `hp.json`**

**3. Disease-gene associations: `genes_to_disease.txt`**

Use the following command to download these files from the latest release:
```shell
java -jar boqa-cli/target/boqa-cli-0.1.0.jar download -d ./data 
```
The files are downloaded to a subdirectory `./data/latest_<time_stamp>`.

To download a specific release, use the following command;
```shell
java -jar boqa-cli/target/boqa-cli-0.1.0.jar download -d ./data -r v2025-05-06 
```
The files are downloaded into a subdirectory of `./data` named after the release.

**4. Phenopackets containing phenotypic features observed in individuals: `phenopacket-store`**

```
wget -O data/all_phenopackets.zip https://github.com/monarch-initiative/phenopacket-store/releases/latest/download/all_phenopackets.zip
unzip data/all_phenopackets.zip -d data
```

## Run CLI

Run the following to run the CLI:

```shell
./mvnw -Prelease package

java -jar boqa-cli/target/boqa-cli-0.1.0.jar --help
```
