package org.jahia.community.aws.cognito.connector;

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

@Component(service = {AwsCognitoConnector.class, OAuthConnectorService.class, ConnectorService.class}, property = {JahiaAuthConstants.CONNECTOR_SERVICE_NAME + "=" + AwsCognitoConnector.KEY}, immediate = true)
public class AwsCognitoConnector implements OAuthConnectorService {
    private static final Logger logger = LoggerFactory.getLogger(AwsCognitoConnector.class);

    public static final String KEY = "AwsCognitoApi20";
    protected static final String URL = "https://%s.auth.%s.amazoncognito.com/oauth2";
    private static final String PROPERTY_ENDPOINT = "endpoint";
    private static final String PROPERTY_REGION = "region";

    private JahiaOAuthService jahiaOAuthService;

    @Reference
    private void setJahiaOAuthService(JahiaOAuthService jahiaOAuthService) {
        this.jahiaOAuthService = jahiaOAuthService;
    }

    @Activate
    private void onActivate() {
        logger.info("Register AwsCognitoConnector");
        jahiaOAuthService.addOAuthDefaultApi20(KEY, connectorConfig -> AwsCognitoApi20.instance(connectorConfig.getProperty(PROPERTY_ENDPOINT), connectorConfig.getProperty(PROPERTY_REGION)));
    }

    @Deactivate
    private void onDeactivate() {
        logger.info("Unregister AwsCognitoConnector");
        jahiaOAuthService.removeOAuthDefaultApi20(KEY);
    }

    @Override
    public String getProtectedResourceUrl(ConnectorConfig config) {
        return String.format(URL, config.getProperty(PROPERTY_ENDPOINT), config.getProperty(PROPERTY_REGION)) + "/userinfo";
    }

    @Override
    public List<ConnectorPropertyInfo> getAvailableProperties() {
        return Collections.emptyList();
    }

    @Override
    public void validateSettings(ConnectorConfig connectorConfig) {
        // Do nothing
    }
}
