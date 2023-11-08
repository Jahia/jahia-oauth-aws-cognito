package org.jahia.community.aws.cognito.connector;

import org.apache.commons.lang.StringUtils;
import org.jahia.api.content.JCRTemplate;
import org.jahia.community.aws.cognito.api.AwsCognitoConstants;
import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.JahiaAuthConstants;
import org.jahia.modules.jahiaauth.service.SettingsService;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthService;
import org.jahia.params.valves.LoginUrlProvider;
import org.jahia.services.sites.JahiaSitesService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

@Component(service = LoginUrlProvider.class)
public class AwsCognitoLoginUrlProvider implements LoginUrlProvider {
    private static final Logger logger = LoggerFactory.getLogger(AwsCognitoLoginUrlProvider.class);

    private SettingsService settingsService;
    private JahiaOAuthService jahiaOAuthService;
    private JahiaSitesService jahiaSitesService;
    private JCRTemplate jcrTemplate;

    @Reference
    private void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Reference
    private void setJahiaOAuthService(JahiaOAuthService jahiaOAuthService) {
        this.jahiaOAuthService = jahiaOAuthService;
    }

    @Reference
    private void setJahiaSitesService(JahiaSitesService jahiaSitesService) {
        this.jahiaSitesService = jahiaSitesService;
    }

    @Reference
    private void setJcrTemplate(JCRTemplate jcrTemplate) {
        this.jcrTemplate = jcrTemplate;
    }

    @Override
    public boolean hasCustomLoginUrl() {
        return true;
    }

    @Override
    public String getLoginUrl(HttpServletRequest httpServletRequest) {
        String siteKey = AwsCognitoConstants.getSiteKey(httpServletRequest, jcrTemplate, jahiaSitesService);
        if (siteKey == null) {
            logger.warn("Site not found.");
            return null;
        }
        String authorizationUrl = getAuthorizationUrl(
                siteKey,
                httpServletRequest.getSession().getId(),
                settingsService,
                jahiaOAuthService, true);
        if (authorizationUrl == null) {
            return null;
        }

        // save the requestUri in the session
        String originalRequestUri = (String) httpServletRequest.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (originalRequestUri == null) {
            originalRequestUri = httpServletRequest.getRequestURI();
        }
        if (originalRequestUri == null) {
            originalRequestUri = httpServletRequest.getHeader("Referer");
        }
        httpServletRequest.getSession(false).setAttribute(AwsCognitoConstants.SESSION_OAUTH_AWS_COGNITO_RETURN_URL, originalRequestUri);
        // redirect to SSO
        return authorizationUrl;
    }

    public static String getAuthorizationUrl(String siteKey, String sessionId, SettingsService settingsService, JahiaOAuthService jahiaOAuthService, boolean checkIfEnabled) {
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
        if (checkIfEnabled && !connectorConfig.getBooleanProperty(JahiaAuthConstants.PROPERTY_IS_ENABLED)) {
            return null;
        }
        if (connectorConfig.getBooleanProperty(AwsCognitoConstants.WITH_CUSTOM_LOGIN)) {
            String loginUrl = connectorConfig.getProperty(AwsCognitoConstants.LOGIN_URL);
            return StringUtils.isNotBlank(loginUrl) ? loginUrl + "&app=jahia&action=login&siteKey=" + siteKey : null;
        }
        return jahiaOAuthService.getAuthorizationUrl(connectorConfig, sessionId, null);
    }
}
