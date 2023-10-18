package org.jahia.community.aws.cognito.actions;

import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.api.usermanager.JahiaUserManagerService;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.bin.Render;
import org.jahia.community.aws.cognito.api.AwsCognitoConstants;
import org.jahia.community.aws.cognito.client.AwsCognitoClientService;
import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.SettingsService;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthConstants;
import org.jahia.params.valves.LoginEngineAuthValveImpl;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.sites.JahiaSitesService;
import org.jahia.services.usermanager.JahiaUser;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component(service = Action.class)
public class AwsCognitoLoginAction extends Action {
    private static final Logger logger = LoggerFactory.getLogger(AwsCognitoLoginAction.class);

    private AwsCognitoClientService awsCognitoClientService;
    private JahiaUserManagerService jahiaUserManagerService;
    private SettingsService settingsService;

    @Reference
    private void setAwsCognitoClientService(AwsCognitoClientService awsCognitoClientService) {
        this.awsCognitoClientService = awsCognitoClientService;
    }

    @Reference
    private void setJahiaUserManagerService(JahiaUserManagerService jahiaUserManagerService) {
        this.jahiaUserManagerService = jahiaUserManagerService;
    }

    @Reference
    private void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public AwsCognitoLoginAction() {
        setRequiredMethods(Render.METHOD_POST);
        setRequireAuthenticatedUser(false);
    }

    @Override
    public ActionResult doExecute(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper jcrSessionWrapper, Map<String, List<String>> parameters, URLResolver urlResolver) {
        ConnectorConfig connectorConfig = settingsService.getConnectorConfig(JahiaSitesService.SYSTEM_SITE_KEY, AwsCognitoConstants.CONNECTOR_KEY);
        if (connectorConfig == null) {
            logger.warn("The systemsite doesn't have the AWS Cognito configuration");
            return ActionResult.BAD_REQUEST;
        }

        String clientId = connectorConfig.getProperty(JahiaOAuthConstants.PROPERTY_API_KEY);
        if (StringUtils.isBlank(clientId)) {
            logger.warn("The systemsite doesn't have the {} property", JahiaOAuthConstants.PROPERTY_API_KEY);
            return ActionResult.BAD_REQUEST;
        }
        String clientSecret = connectorConfig.getProperty(JahiaOAuthConstants.PROPERTY_API_SECRET);
        if (StringUtils.isBlank(clientSecret)) {
            logger.warn("The systemsite doesn't have the {} property", JahiaOAuthConstants.PROPERTY_API_SECRET);
            return ActionResult.BAD_REQUEST;
        }
        String region = connectorConfig.getProperty("region");
        if (StringUtils.isBlank(region)) {
            logger.warn("The systemsite doesn't have the region property");
            return ActionResult.BAD_REQUEST;
        }

        String username = getRequiredParameter(parameters, "username");
        String password = getRequiredParameter(parameters, "password");
        Optional<String> userIdentifier = awsCognitoClientService.login(region, clientId, clientSecret, username, password);
        if (userIdentifier.isPresent()) {
            JCRUserNode jcrUserNode = jahiaUserManagerService.lookupUser(userIdentifier.get());
            if (jcrUserNode != null) {
                JahiaUser jahiaUser = jcrUserNode.getJahiaUser();
                httpServletRequest.getSession().invalidate();
                // user has been successfully authenticated, note this in the current session.
                httpServletRequest.getSession().setAttribute(Constants.SESSION_USER, jahiaUser);
                // eventually set the Jahia user
                ((JCRSessionFactory) jcrSessionWrapper.getRepository()).setCurrentUser(jahiaUser);
                httpServletRequest.setAttribute(LoginEngineAuthValveImpl.VALVE_RESULT, LoginEngineAuthValveImpl.OK);
                return ActionResult.OK;
            } else {
                logger.warn("Login failed (user not found in JCR).");
            }
        } else {
            logger.warn("Login failed (missing accessToken).");
        }
        return ActionResult.BAD_REQUEST;
    }
}
