package org.jahia.community.aws.cognito.connector;

import org.jahia.community.aws.cognito.api.AwsCognitoConstants;
import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.ConnectorPropertyInfo;
import org.jahia.modules.jahiaauth.service.ConnectorService;
import org.jahia.modules.jahiaauth.service.JahiaAuthConstants;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthService;
import org.jahia.modules.jahiaoauth.service.OAuthConnectorService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

@Component(service = {AwsCognitoConnector.class, OAuthConnectorService.class, ConnectorService.class}, property = {JahiaAuthConstants.CONNECTOR_SERVICE_NAME + "=" + AwsCognitoConstants.CONNECTOR_KEY}, immediate = true)
public class AwsCognitoConnector implements OAuthConnectorService {
    private static final Logger logger = LoggerFactory.getLogger(AwsCognitoConnector.class);

    @Reference
    private JahiaOAuthService jahiaOAuthService;

    @Activate
    private void onActivate() {
        logger.info("Register AwsCognitoConnector");
        jahiaOAuthService.addOAuthDefaultApi20(AwsCognitoConstants.CONNECTOR_KEY, connectorConfig -> AwsCognitoApi20.instance(connectorConfig.getProperty(AwsCognitoConstants.ENDPOINT)));
    }

    @Deactivate
    private void onDeactivate() {
        logger.info("Unregister AwsCognitoConnector");
        jahiaOAuthService.removeOAuthDefaultApi20(AwsCognitoConstants.CONNECTOR_KEY);
    }

    @Override
    public String getProtectedResourceUrl(ConnectorConfig connectorConfig) {
        return null;
    }

    @Override
    public List<String> getProtectedResourceUrls(ConnectorConfig config) {
        return Collections.emptyList();
    }

    @Override
    public List<ConnectorPropertyInfo> getAvailableProperties() {
        return Collections.emptyList();
    }
}
