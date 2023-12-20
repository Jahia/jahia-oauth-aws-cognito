package org.jahia.community.aws.cognito.api;

import org.apache.commons.lang.StringUtils;
import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.JahiaAuthConstants;
import org.jahia.modules.jahiaauth.service.SettingsService;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthConstants;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthService;
import org.jahia.services.sites.JahiaSitesService;

public final class AwsCognitoConstants {
    private AwsCognitoConstants() {
        // No constructor
    }

    public static final String PROVIDER_KEY = "awsCognito";
    public static final String CONNECTOR_KEY = "AwsCognitoApi20";
    public static final String SESSION_OAUTH_AWS_COGNITO_RETURN_URL = "oauth.aws-cognito.return-url";
    public static final String CUSTOM_PROPERTY_EMAIL = "email";
    public static final String USER_PROPERTY_EMAIL = "j:email";

    public static final String TARGET_SITE = "target.site";
    public static final String ACCESS_KEY_ID = "accessKeyId";
    public static final String SECRET_ACCESS_KEY = "secretAccessKey";
    public static final String USER_POOL_ID = "userPoolId";
    public static final String LOGOUT_ENDPOINT = "logoutEndpoint";
    public static final String LOGOUT_CALLBACK_URL = "logoutCallbackUrl";
    private static final String LOGOUT_URL = "%s/logout?client_id=%s&logout_uri=%s";

    public static String getAuthorizationUrl(String siteKey, String sessionId, SettingsService settingsService, JahiaOAuthService jahiaOAuthService) {
        if (siteKey == null) {
            return null;
        }
        ConnectorConfig connectorConfig = settingsService.getConnectorConfig(siteKey, CONNECTOR_KEY);
        if (connectorConfig == null) {
            // fallback to systemsite
            connectorConfig = settingsService.getConnectorConfig(JahiaSitesService.SYSTEM_SITE_KEY, CONNECTOR_KEY);
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

    public static String getLogoutUrl(String siteKey, SettingsService settingsService) {
        if (siteKey == null) {
            return null;
        }
        ConnectorConfig connectorConfig = settingsService.getConnectorConfig(siteKey, CONNECTOR_KEY);
        if (connectorConfig == null) {
            // fallback to systemsite
            connectorConfig = settingsService.getConnectorConfig(JahiaSitesService.SYSTEM_SITE_KEY, CONNECTOR_KEY);
            if (connectorConfig == null) {
                // no configuration found
                return null;
            }
        }
        if (!connectorConfig.getBooleanProperty(JahiaAuthConstants.PROPERTY_IS_ENABLED) || StringUtils.isBlank(connectorConfig.getProperty(LOGOUT_ENDPOINT))) {
            return null;
        }
        return String.format(LOGOUT_URL,
                connectorConfig.getProperty(LOGOUT_ENDPOINT),
                connectorConfig.getProperty(JahiaOAuthConstants.PROPERTY_API_KEY),
                connectorConfig.getProperty(LOGOUT_CALLBACK_URL));
    }
}
