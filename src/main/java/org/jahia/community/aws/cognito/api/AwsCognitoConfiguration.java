package org.jahia.community.aws.cognito.api;

import org.apache.commons.lang.StringUtils;

public class AwsCognitoConfiguration {
    private final String targetSite;
    private final String accessKeyId;
    private final String secretAccessKey;
    private final String region;
    private final String userPoolId;

    public AwsCognitoConfiguration(String targetSite, String accessKeyId, String secretAccessKey, String userPoolId) {
        this.targetSite = targetSite;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.region = StringUtils.split(userPoolId, "_")[0];
        this.userPoolId = userPoolId;
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
}
