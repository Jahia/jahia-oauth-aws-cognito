package org.jahia.community.aws.cognito.connector;

import com.github.scribejava.apis.openid.OpenIdJsonTokenExtractor;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.TokenExtractor;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth2.clientauthentication.ClientAuthentication;
import com.github.scribejava.core.oauth2.clientauthentication.RequestBodyAuthenticationScheme;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class AwsCognitoApi20 extends DefaultApi20 {
    private static final ConcurrentMap<String, AwsCognitoApi20> INSTANCES = new ConcurrentHashMap<>();
    private final String endpoint;

    private AwsCognitoApi20(String endpoint) {
        this.endpoint = endpoint;
    }

    public static AwsCognitoApi20 instance(String endpoint) {
        return INSTANCES.computeIfAbsent(endpoint, key -> new AwsCognitoApi20(endpoint));
    }

    @Override
    public String getAccessTokenEndpoint() {
        return endpoint + "/oauth2/token";
    }

    @Override
    protected String getAuthorizationBaseUrl() {
        return endpoint + "/oauth2/authorize";
    }

    @Override
    public ClientAuthentication getClientAuthentication() {
        return RequestBodyAuthenticationScheme.instance();
    }

    @Override
    public TokenExtractor<OAuth2AccessToken> getAccessTokenExtractor() {
        return OpenIdJsonTokenExtractor.instance();
    }
}
