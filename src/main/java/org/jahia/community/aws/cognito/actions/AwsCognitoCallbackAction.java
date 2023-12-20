package org.jahia.community.aws.cognito.actions;

import org.apache.commons.lang.StringUtils;
import org.jahia.api.content.JCRTemplate;
import org.jahia.api.usermanager.JahiaUserManagerService;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.bin.Render;
import org.jahia.community.aws.cognito.api.AwsCognitoConstants;
import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.JahiaAuthConstants;
import org.jahia.modules.jahiaauth.service.SettingsService;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthService;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.sites.JahiaSitesService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@Component(service = Action.class)
public class AwsCognitoCallbackAction extends Action {
    private static final Logger logger = LoggerFactory.getLogger(AwsCognitoCallbackAction.class);

    private static final String NAME = "awsCognitoOAuthCallbackAction";

    private JahiaOAuthService jahiaOAuthService;
    private SettingsService settingsService;
    private JCRTemplate jcrTemplate;
    private JahiaSitesService jahiaSitesService;
    private JahiaUserManagerService jahiaUserManagerService;

    @Reference
    private void setJahiaOAuthService(JahiaOAuthService jahiaOAuthService) {
        this.jahiaOAuthService = jahiaOAuthService;
    }

    @Reference
    private void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Reference
    private void setJcrTemplate(JCRTemplate jcrTemplate) {
        this.jcrTemplate = jcrTemplate;
    }

    @Reference
    private void setJahiaSitesService(JahiaSitesService jahiaSitesService) {
        this.jahiaSitesService = jahiaSitesService;
    }

    @Reference
    private void setJahiaUserManagerService(JahiaUserManagerService jahiaUserManagerService) {
        this.jahiaUserManagerService = jahiaUserManagerService;
    }

    public AwsCognitoCallbackAction() {
        setName(NAME);
        setRequireAuthenticatedUser(false);
        setRequiredMethods(Render.METHOD_GET);
    }

    @Override
    public ActionResult doExecute(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper jcrSessionWrapper, Map<String, List<String>> parameters, URLResolver urlResolver) {
        if (parameters.containsKey("code")) {
            final String token = getRequiredParameter(parameters, "code");
            if (StringUtils.isBlank(token)) {
                return ActionResult.BAD_REQUEST;
            }

            try {
                String siteKey = renderContext.getSite().getSiteKey();
                ConnectorConfig connectorConfig = settingsService.getConnectorConfig(siteKey, AwsCognitoConstants.CONNECTOR_KEY);
                jahiaOAuthService.extractAccessTokenAndExecuteMappers(connectorConfig, token, httpServletRequest.getRequestedSessionId());
                String returnUrl = (String) httpServletRequest.getSession(false).getAttribute(AwsCognitoConstants.SESSION_OAUTH_AWS_COGNITO_RETURN_URL);
                if (StringUtils.isBlank(returnUrl)) {
                    returnUrl = jcrTemplate.doExecuteWithSystemSessionAsUser(null, renderContext.getWorkspace(), renderContext.getMainResourceLocale(), systemSession ->
                            jahiaSitesService.getSiteByKey(siteKey, systemSession).getHome().getUrl());
                }

                if (logger.isDebugEnabled()) {
                    JCRUserNode jcrUserNode = jahiaUserManagerService.lookupUser((String) httpServletRequest.getAttribute(JahiaAuthConstants.SSO_LOGIN));
                    logger.debug("User found: {}", jcrUserNode == null ? null : jcrUserNode.getPath());
                }
                // WARN: site query param is mandatory for the SSOValve in jahia-authentication module
                return new ActionResult(HttpServletResponse.SC_OK, returnUrl + "?site=" + siteKey, true, null);
            } catch (Exception e) {
                logger.error("", e);
            }
        } else {
            logger.error("Could not authenticate user with SSO, the callback from the server was missing mandatory parameters");
        }
        return ActionResult.BAD_REQUEST;
    }
}
