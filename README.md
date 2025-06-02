# BOQA

Bayesian Ontology Query Analysis

## Local setup

The project uses Exomiser as a library. However, Exomiser is not available from Maven central repository.
Therefore, we must install Exomiser locally to be able to build the code.

The installation proceeds as follows:

```shell
# First, let's check out Exomiser source code
git clone https://github.com/exomiser/Exomiser.git

# Check out the version we use
cd Exomiser
git checkout 14.1.0

# Make mvnw executable
chmod u+x mvnw

# Install Exomiser locally
./mvnw install
```

This should install Exomiser into local Maven repository, and it should be possible to build the project.

## Run tests

We use Maven Surefire plugin to run tests:

```shell
./mvnw test
```

## Run CLI

Run the following to run the CLI:

```shell
./mvnw -Prelease package

java -jar boqa-cli/target/boqa-cli-0.1.0-SNAPSHOT.jar --help
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
We use the following HPO data sources:

**1. Disease-phenotype associations: `phenotype.hpoa`**

```
wget -O phenotype.hpoa XXX
```

**2. HPO: `hp.json`**

```
wget -O hp.json XXX
```

**3. Phenotypic features observed in individuals: phenopackets from `phenopacket-store`**

```
wget -O phenopacket-store XXX
```

## TODO

Add more information at some point. 
