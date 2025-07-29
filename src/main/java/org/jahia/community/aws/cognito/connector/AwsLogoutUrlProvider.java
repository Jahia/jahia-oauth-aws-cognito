package org.jahia.community.aws.cognito.connector;

import org.jahia.api.content.JCRTemplate;
import org.jahia.community.aws.cognito.api.AwsCognitoConstants;
import org.jahia.modules.jahiaauth.service.SettingsService;
import org.jahia.params.valves.LogoutUrlProvider;
import org.jahia.services.sites.JahiaSitesService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

@Component(service = LogoutUrlProvider.class)
public class AwsLogoutUrlProvider implements LogoutUrlProvider {
    private static final Logger logger = LoggerFactory.getLogger(AwsLogoutUrlProvider.class);

    @Reference
    private SettingsService settingsService;
    @Reference
    private JCRTemplate jcrTemplate;
    @Reference
    private JahiaSitesService jahiaSitesService;

    @Override
    public String getLogoutUrl(HttpServletRequest httpServletRequest) {
        return AwsCognitoConstants.getLogoutUrl(httpServletRequest, jcrTemplate, jahiaSitesService, settingsService);
    }

    @Override
    public boolean hasCustomLogoutUrl() {
        return true;
    }
}
