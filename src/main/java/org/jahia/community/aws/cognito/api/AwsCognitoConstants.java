package org.jahia.community.aws.cognito.api;

import org.jahia.api.Constants;
import org.jahia.api.content.JCRTemplate;
import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.SettingsService;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.sites.JahiaSite;
import org.jahia.services.sites.JahiaSitesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

public final class AwsCognitoConstants {
    private static final Logger logger = LoggerFactory.getLogger(AwsCognitoConstants.class);

    private AwsCognitoConstants() {
        // No constructor
    }

    public static final String KEY = "AwsCognitoApi20";
    public static final String URL = "https://%s.auth.%s.amazoncognito.com";
    public static final String SESSION_OAUTH_AWS_COGNITO_RETURN_URL = "oauth.aws-cognito.return-url";

    public static final String TARGET_SITE = "target.site";
    public static final String ACCESS_KEY_ID = "accessKeyId";
    public static final String SECRET_ACCESS_KEY = "secretAccessKey";
    public static final String REGION = "region";
    public static final String USER_POOL_ID = "userPoolId";
    public static final String SECRET_KEY = "secretKey";
    public static final String WITH_CUSTOM_LOGIN = "withCustomLogin";
    public static final String LOGIN_URL = "loginUrl";
    public static final String ENDPOINT = "endpoint";

    public static final String USER_PROPERTY_FIRSTNAME = "j:firstName";
    public static final String USER_PROPERTY_LASTNAME = "j:lastName";
    public static final String USER_PROPERTY_EMAIL = "j:email";
    public static final String USER_PROPERTY_ACCOUNTLOCKED = "j:accountLocked";
    public static final String USER_PROPERTY_STATUS = "status";
    public static final String USER_ATTRIBUTE_ACCOUNTLOCKED = "locked";

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

    public static AwsCognitoConfiguration getAwsCognitoConfiguration(HttpServletRequest httpServletRequest) {
        String siteKey = AwsCognitoConstants.getSiteKey(httpServletRequest, BundleUtils.getOsgiService(JCRTemplate.class, null), BundleUtils.getOsgiService(JahiaSitesService.class, null));
        if (siteKey == null) {
            logger.warn("Site not found.");
            return null;
        }
        ConnectorConfig connectorConfig = getConnectorConfig(siteKey);
        if (connectorConfig == null) {
            logger.warn("The site {} doesn't have the AWS Cognito configuration", siteKey);
            return null;
        }
        return new AwsCognitoConfiguration(
                null,
                connectorConfig.getProperty(AwsCognitoConstants.ACCESS_KEY_ID),
                connectorConfig.getProperty(AwsCognitoConstants.SECRET_ACCESS_KEY),
                connectorConfig.getProperty(AwsCognitoConstants.USER_POOL_ID)
        );
    }

    public static ConnectorConfig getConnectorConfig(String siteKey) {
        SettingsService settingsService = BundleUtils.getOsgiService(SettingsService.class, null);
        ConnectorConfig connectorConfig = settingsService.getConnectorConfig(siteKey, AwsCognitoConstants.KEY);
        if (connectorConfig == null) {
            // fallback to systemsite
            connectorConfig = settingsService.getConnectorConfig(JahiaSitesService.SYSTEM_SITE_KEY, AwsCognitoConstants.KEY);
        }
        return connectorConfig;
    }
}
