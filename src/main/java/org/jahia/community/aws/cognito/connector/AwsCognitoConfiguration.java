package org.jahia.community.aws.cognito.connector;

import org.apache.commons.lang.StringUtils;

public class AwsCognitoConfiguration {
    private final String targetSite;
    private final String accessKeyId;
    private final String secretAccessKey;
    private final String region;
    private final String userPoolId;
    private final String clientId;
    private final String clientSecret;

    public AwsCognitoConfiguration(String targetSite, String accessKeyId, String secretAccessKey, String userPoolId, String clientId, String clientSecret) {
        this.targetSite = targetSite;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.region = userPoolId != null ? StringUtils.split(userPoolId, "_")[0] : null;
        this.userPoolId = userPoolId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
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

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }
}
