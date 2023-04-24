package org.jahia.community.aws.cognito.provider;

public class AwsCognitoConfiguration {
    private final String targetSite;
    private final String organization;
    private final String apiToken;

    private String region;
    private String pool;


    public AwsCognitoConfiguration(String targetSite, String organization, String apiToken) {
        this.targetSite = targetSite;
        this.organization = organization;
        this.apiToken = apiToken;
    }

    public String getTargetSite() {
        return targetSite;
    }

    public String getOrganization() {
        return organization;
    }

    public String getApiToken() {
        return apiToken;
    }
}
