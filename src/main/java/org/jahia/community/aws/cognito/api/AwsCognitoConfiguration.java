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
    private final String clientSecret;
    private final String providerKey;
    private final String siteKey;

    public AwsCognitoConfiguration(String targetSite, String accessKeyId, String secretAccessKey, String userPoolId, String endpoint, String clientId, String clientSecret, String providerKey, String siteKey) {
        this.targetSite = targetSite;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.region = userPoolId != null ? StringUtils.split(userPoolId, "_")[0] : null;
        this.userPoolId = userPoolId;
        this.endpoint = endpoint;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.providerKey = providerKey;
        this.siteKey = siteKey;
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

    public String getClientSecret() {
        return clientSecret;
    }

    public String getProviderKey() {
        return providerKey;
    }

    public String getSiteKey() {
        return siteKey;
    }
}
