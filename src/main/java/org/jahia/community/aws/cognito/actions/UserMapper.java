package org.jahia.community.aws.cognito.actions;

import org.jahia.community.aws.cognito.connector.AwsCognitoConstants;
import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.ConnectorResultProcessor;
import org.jahia.modules.jahiaauth.service.JahiaAuthConstants;
import org.osgi.service.component.annotations.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Map;

@Component(service = ConnectorResultProcessor.class)
public class UserMapper implements ConnectorResultProcessor {
    private static final String SSO_LOGIN = "sub";

    @Override
    public void execute(ConnectorConfig connectorConfig, Map<String, Object> results) {
        if (results.containsKey(AwsCognitoConstants.SSO_LOGIN)) {
            RequestContextHolder.getRequestAttributes()
                    .setAttribute(JahiaAuthConstants.SSO_LOGIN, results.get(AwsCognitoConstants.SSO_LOGIN), RequestAttributes.SCOPE_REQUEST);
        }
    }
}
