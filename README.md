# DiBoq

Project template.

## Run tests

We use Maven Surefire plugin to run tests:

```shell
./mvnw test
```

## Run CLI

Run the following to run the CLI:

```shell
./mvnw -Prelease package

java -jar diboq-cli/target/diboq-cli-0.1.0-SNAPSHOT.jar --help
```

## Distribute CLI

The compilation of JAR files, generation of sources and Javadoc, as well as packaging into distribution ZIP file
happens in `release` profile:

```shell
./mvnw -Prelease package 
```

## TODO

Add more information at some point. 