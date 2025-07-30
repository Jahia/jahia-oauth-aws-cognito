package org.jahia.community.aws.cognito.api;

import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.api.content.JCRTemplate;
import org.jahia.bin.Logout;
import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.JahiaAuthConstants;
import org.jahia.modules.jahiaauth.service.SettingsService;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthConstants;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthService;
import org.jahia.services.sites.JahiaSite;
import org.jahia.services.sites.JahiaSitesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class AwsCognitoConstants {
    private AwsCognitoConstants() {
        // No constructor
    }

    private static final Logger logger = LoggerFactory.getLogger(AwsCognitoConstants.class);

    public static final String PROVIDER_KEY = "awsCognito";
    public static final String CONNECTOR_KEY = "AwsCognitoApi20";
    public static final String SESSION_OAUTH_AWS_COGNITO_RETURN_URL = "oauth.aws-cognito.return-url";
    public static final String CUSTOM_PROPERTY_EMAIL = "email";
    public static final String USER_PROPERTY_EMAIL = "j:email";
    public static final String SSO_LOGIN = "sub";
    public static final String AWS_USERNAME = "custom:username";

    public static final String TARGET_SITE = "target.site";
    public static final String ACCESS_KEY_ID = "accessKeyId";
    public static final String SECRET_ACCESS_KEY = "secretAccessKey";
    public static final String USER_POOL_ID = "userPoolId";
    public static final String ENDPOINT = "endpoint";
    public static final String LOGOUT_AWS = "logoutAWS";
    public static final String LOGOUT_CALLBACK_URL = "logoutCallbackUrl";
    private static final String LOGOUT_URL = "%s/logout?client_id=%s&logout_uri=%s";

    private static ConnectorConfig getConfig(String siteKey, SettingsService settingsService) {
        if (siteKey == null) {
            return null;
        }
        ConnectorConfig connectorConfig = settingsService.getConnectorConfig(siteKey, CONNECTOR_KEY);
        if (connectorConfig == null) {
            // fallback to systemsite
            connectorConfig = settingsService.getConnectorConfig(JahiaSitesService.SYSTEM_SITE_KEY, CONNECTOR_KEY);
        }
        return connectorConfig;
    }

    public static String getAuthorizationUrl(String siteKey, String sessionId, SettingsService settingsService, JahiaOAuthService jahiaOAuthService) {
        ConnectorConfig connectorConfig = getConfig(siteKey, settingsService);
        if (connectorConfig == null || !connectorConfig.getBooleanProperty(JahiaAuthConstants.PROPERTY_IS_ENABLED)) {
            return null;
        }
        return jahiaOAuthService.getAuthorizationUrl(connectorConfig, sessionId, null);
    }

    public static String getLogoutUrl(HttpServletRequest httpServletRequest, JCRTemplate jcrTemplate, JahiaSitesService jahiaSitesService, SettingsService settingsService) {
        String siteKey = getSiteKey(httpServletRequest, jcrTemplate, jahiaSitesService);
        ConnectorConfig connectorConfig = getConfig(siteKey, settingsService);
        if (connectorConfig == null || !connectorConfig.getBooleanProperty(JahiaAuthConstants.PROPERTY_IS_ENABLED) || StringUtils.isBlank(connectorConfig.getProperty(ENDPOINT))) {
            return null;
        }

        String logoutUrl = Logout.getLogoutServletPath();
        if (connectorConfig.getBooleanProperty(AwsCognitoConstants.LOGOUT_AWS)) {
            try {
                logoutUrl = String.format(LOGOUT_URL,
                        connectorConfig.getProperty(ENDPOINT),
                        connectorConfig.getProperty(JahiaOAuthConstants.PROPERTY_API_KEY),
                        URLEncoder.encode(connectorConfig.getProperty(LOGOUT_CALLBACK_URL), StandardCharsets.UTF_8.name()));
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.error("", e);
                }
            }
        }
        return logoutUrl;
    }

    public static String getSiteKey(HttpServletRequest httpServletRequest, JCRTemplate jcrTemplate, JahiaSitesService jahiaSitesService) {
        try {
            return jcrTemplate.doExecuteWithSystemSessionAsUser(null, Constants.EDIT_WORKSPACE, null, systemSession -> {
                JahiaSite jahiaSite = jahiaSitesService.getSiteByServerName(httpServletRequest.getServerName(), systemSession);
                if (jahiaSite != null) {
                    return jahiaSite.getSiteKey();
                }

                jahiaSite = jahiaSitesService.getDefaultSite(systemSession);
                if (jahiaSite != null) {
                    return jahiaSite.getSiteKey();
                }
                return JahiaSitesService.SYSTEM_SITE_KEY;
            });
        } catch (RepositoryException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("", e);
            }
        }
        return null;
    }
}
