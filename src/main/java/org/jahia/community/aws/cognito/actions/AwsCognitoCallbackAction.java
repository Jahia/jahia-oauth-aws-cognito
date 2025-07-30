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
import org.jahia.modules.jahiaauth.service.JahiaAuthMapperService;
import org.jahia.modules.jahiaauth.service.MappedProperty;
import org.jahia.modules.jahiaauth.service.SettingsService;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthService;
import org.jahia.services.content.JCRSessionWrapper;
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
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Component(service = Action.class)
public class AwsCognitoCallbackAction extends Action {
    private static final Logger logger = LoggerFactory.getLogger(AwsCognitoCallbackAction.class);

    private static final String NAME = "awsCognitoOAuthCallbackAction";

    @Reference
    private JahiaOAuthService jahiaOAuthService;
    @Reference
    private JahiaAuthMapperService jahiaAuthMapperService;
    @Reference
    private SettingsService settingsService;
    @Reference
    private JCRTemplate jcrTemplate;
    @Reference
    private JahiaSitesService jahiaSitesService;
    @Reference
    private JahiaUserManagerService jahiaUserManagerService;

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
                // invalide http session
                httpServletRequest.getSession(false).invalidate();
                // renew http session
                HttpSession session = httpServletRequest.getSession();
                // decode openid token
                jahiaOAuthService.extractAccessTokenAndExecuteMappers(connectorConfig, token, session.getId());
                // cache username
                jahiaAuthMapperService.cacheMapperResults("cognito-mapper", session.getId(), Collections.singletonMap(JahiaAuthConstants.SSO_LOGIN, new MappedProperty(null, httpServletRequest.getAttribute(JahiaAuthConstants.SSO_LOGIN))));
                // redirect
                AtomicReference<String> returnUrl = new AtomicReference<>();
                Arrays.stream(httpServletRequest.getCookies()).filter(cookie -> AwsCognitoConstants.SESSION_OAUTH_AWS_COGNITO_RETURN_URL.equals(cookie.getName()))
                        .findFirst()
                        .ifPresent(cookie -> returnUrl.set(cookie.getValue()));
                if (StringUtils.isBlank(returnUrl.get())) {
                    returnUrl.set(jcrTemplate.doExecuteWithSystemSessionAsUser(null, renderContext.getWorkspace(), renderContext.getMainResourceLocale(), systemSession ->
                            jahiaSitesService.getSiteByKey(siteKey, systemSession).getHome().getUrl()));
                }
                // WARN: site query param is mandatory for the SSOValve in jahia-authentication module
                return new ActionResult(HttpServletResponse.SC_OK, returnUrl.get() + "?site=" + siteKey, true, null);
            } catch (Exception e) {
                logger.error("", e);
            }
        } else {
            logger.error("Could not authenticate user with SSO, the callback from the server was missing mandatory parameters");
        }
        return ActionResult.BAD_REQUEST;
    }
}
