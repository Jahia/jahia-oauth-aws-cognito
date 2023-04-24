package org.jahia.community.aws.cognito.provider;

public class AwsCognitoConfiguration {
    public static final String PROP_TARGET_SITE = "prop.target.site";
    public static final String PROP_KEY_ID = "prop.keyId";
    public static final String PROP_ACCESS_TOKEN = "prop.accessToken";
    public static final String PROP_REGION = "prop.region";

    private final String targetSite;
    private final String keyId;
    private final String accessKey;
    private final String region;

    public AwsCognitoConfiguration(String targetSite, String keyId, String accessKey, String region) {
        this.targetSite = targetSite;
        this.keyId = keyId;
        this.accessKey = accessKey;
        this.region = region;
    }

    public String getTargetSite() {
        return targetSite;
    }

    public String getKeyId() {
        return keyId;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getRegion() {
        return region;
    }
}
