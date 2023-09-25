package org.jahia.community.aws.cognito.api;

import org.apache.commons.lang.StringUtils;

public class AwsCognitoConfiguration {
    private final String targetSite;
    private final String accessKeyId;
    private final String secretAccessKey;
    private final String region;
    private final String userPoolId;
    private final String endpoint;
    private final String clientId;

    public AwsCognitoConfiguration(String targetSite, String accessKeyId, String secretAccessKey, String userPoolId, String endpoint, String clientId) {
        this.targetSite = targetSite;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.region = StringUtils.split(userPoolId, "_")[0];
        this.userPoolId = userPoolId;
        this.endpoint = endpoint;
        this.clientId = clientId;
    }

    public String getTargetSite() {
        return targetSite;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public String getRegion() {
        return region;
    }

    public String getUserPoolId() {
        return userPoolId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getClientId() {
        return clientId;
    }
}
