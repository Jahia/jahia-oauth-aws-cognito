package org.jahia.community.aws.cognito.api;

import org.apache.commons.lang.StringUtils;

public class AwsCognitoConfiguration {
    public static final String TARGET_SITE = "target.site";
    public static final String ACCESS_KEY_ID = "accessKeyId";
    public static final String SECRET_ACCESS_KEY = "secretAccessKey";
    public static final String USER_POOL_ID = "userPoolId";
    public static final String SECRET_KEY = "secretKey";
    public static final String WITH_CUSTOM_LOGIN = "withCustomLogin";
    public static final String LOGIN_URL = "loginUrl";
    public static final String WITH_CUSTOM_LOGOUT = "withCustomLogout";

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
