package org.jahia.community.aws.cognito.connector;

import org.apache.commons.lang.StringUtils;
import org.jahia.community.aws.cognito.provider.AwsCognitoConfiguration;
import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.SettingsService;
import org.jahia.params.valves.LoginUrlProvider;
import org.jahia.services.sites.JahiaSitesService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

@Component(service = LoginUrlProvider.class)
public class AwsCognitoLoginUrlProvider implements LoginUrlProvider {
    private static final Logger logger = LoggerFactory.getLogger(AwsCognitoLoginUrlProvider.class);

    private SettingsService settingsService;

    @Reference
    private void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Override
    public boolean hasCustomLoginUrl() {
        return true;
    }

    @Override
    public String getLoginUrl(HttpServletRequest httpServletRequest) {
        ConnectorConfig connectorConfig = settingsService.getConnectorConfig(JahiaSitesService.SYSTEM_SITE_KEY, AwsCognitoConnector.KEY);
        if (connectorConfig == null) {
            logger.warn("The systemsite doesn't have the AWS Cognito configuration");
            return null;
        }
        String loginUrl = connectorConfig.getProperty(AwsCognitoConfiguration.LOGIN_URL);
        return StringUtils.isNotBlank(loginUrl) ? loginUrl : null;
    }
}
