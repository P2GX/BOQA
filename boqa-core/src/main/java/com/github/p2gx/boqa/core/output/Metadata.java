package com.github.p2gx.boqa.core.output;

import java.util.Map;

public class Metadata {
    private String timestamp;
    private String hpoVersion;
    private String hpoaVersion;
    private Map<String, Object> patientDataMetadata;
    private Map<String, Object> algorithmParams;
    private String cliArgs;
    private Map<String, String> environment;

    public Metadata(String timestamp,
                    String hpoVersion,
                    String hpoaVersion,
                    Map<String, Object> patientDataMetadata,
                    Map<String, Object> algorithmParams,
                    String cliArgs,
                    Map<String, String> environment) {
        this.timestamp = timestamp;
        this.hpoVersion = hpoVersion;
        this.hpoaVersion = hpoaVersion;
        this.patientDataMetadata = patientDataMetadata;
        this.algorithmParams = algorithmParams;
        this.cliArgs = cliArgs;
        this.environment = environment;
    }

    public String getTimestamp() { return timestamp; }
    public String getHpoVersion() { return hpoVersion; }
    public String getHpoaVersion() { return hpoaVersion; }
    public Map<String, Object> getPatientDataMetadata() { return patientDataMetadata; }
    public Map<String, Object> getAlgorithmParams() { return algorithmParams; }
    public String getCliArgs() { return cliArgs; }
    public Map<String, String> getEnvironment() { return environment; }
}

