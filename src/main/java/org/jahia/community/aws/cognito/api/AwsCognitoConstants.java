package org.jahia.community.aws.cognito.api;

import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.JahiaAuthConstants;
import org.jahia.modules.jahiaauth.service.SettingsService;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthService;
import org.jahia.services.sites.JahiaSitesService;

public final class AwsCognitoConstants {
    private AwsCognitoConstants() {
        // No constructor
    }

    public static final String CONNECTOR_KEY = "AwsCognitoApi20";
    public static final String SESSION_OAUTH_AWS_COGNITO_RETURN_URL = "oauth.aws-cognito.return-url";
    public static final String CUSTOM_PROPERTY_EMAIL = "email";
    public static final String USER_PROPERTY_EMAIL = "j:email";

    public static String getAuthorizationUrl(String siteKey, String sessionId, SettingsService settingsService, JahiaOAuthService jahiaOAuthService) {
        if (siteKey == null) {
            return null;
        }
        ConnectorConfig connectorConfig = settingsService.getConnectorConfig(siteKey, AwsCognitoConstants.CONNECTOR_KEY);
        if (connectorConfig == null) {
            // fallback to systemsite
            connectorConfig = settingsService.getConnectorConfig(JahiaSitesService.SYSTEM_SITE_KEY, AwsCognitoConstants.CONNECTOR_KEY);
            if (connectorConfig == null) {
                // no configuration found
                return null;
            }
        }
        if (!connectorConfig.getBooleanProperty(JahiaAuthConstants.PROPERTY_IS_ENABLED)) {
            return null;
        }
        return jahiaOAuthService.getAuthorizationUrl(connectorConfig, sessionId, null);
    }
}
