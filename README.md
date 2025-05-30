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

1. Disease-phenotype associations: phenotype.hpoa

2. HPO in JSON format: hpo.json

## TODO

Add more information at some point. 
