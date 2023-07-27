package org.jahia.community.aws.cognito.jaxrs;

import org.glassfish.jersey.server.ResourceConfig;

public class JaxRsConfig extends ResourceConfig {
    public JaxRsConfig() {
        super(AwsCognitoEndpoint.class);
    }
}
