package org.jahia.community.aws.cognito.api;

import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AwsCognitoConfiguration {
    private final String targetSite;
    private final String accessKeyId;
    private final String secretAccessKey;
    private final String region;
    private final String userPoolId;

    public AwsCognitoConfiguration(Dictionary<String, ?> dictionary) {
        this(Collections.list(dictionary.keys()).stream()
                .collect(Collectors.toMap(Function.identity(), dictionary::get)));
    }

    public AwsCognitoConfiguration(Map<String, ?> props) {
        Object site = props.get(AwsCognitoConstants.TARGET_SITE);
        this.targetSite = site == null ? null : (String) site;
        this.accessKeyId = (String) props.get(AwsCognitoConstants.ACCESS_KEY_ID);
        this.secretAccessKey = (String) props.get(AwsCognitoConstants.SECRET_ACCESS_KEY);
        this.userPoolId = (String) props.get(AwsCognitoConstants.USER_POOL_ID);
        this.region = userPoolId != null ? StringUtils.split(userPoolId, "_")[0] : null;
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
