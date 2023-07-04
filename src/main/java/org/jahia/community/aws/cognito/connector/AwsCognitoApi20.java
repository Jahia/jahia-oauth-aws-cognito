package org.jahia.community.aws.cognito.connector;

import com.github.scribejava.core.builder.api.DefaultApi20;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class AwsCognitoApi20 extends DefaultApi20 {
    private static final ConcurrentMap<String, AwsCognitoApi20> INSTANCES = new ConcurrentHashMap<>();
    private final String endpoint;
    private final String region;

    private AwsCognitoApi20(String endpoint, String region) {
        this.endpoint = endpoint;
        this.region = region;
    }

    public static AwsCognitoApi20 instance(String endpoint, String region) {
        return INSTANCES.computeIfAbsent(endpoint + "###" + region, key -> new AwsCognitoApi20(endpoint, region));
    }

    @Override
    public String getAccessTokenEndpoint() {
        return String.format(AwsCognitoConnector.URL, endpoint, region) + "/token";
    }

    @Override
    protected String getAuthorizationBaseUrl() {
        return String.format(AwsCognitoConnector.URL, endpoint, region) + "/authorize";
    }
}
