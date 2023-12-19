package org.jahia.community.aws.cognito.actions;

import org.apache.commons.lang.StringUtils;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.bin.Render;
import org.jahia.community.aws.cognito.connector.AwsCognitoConstants;
import org.jahia.modules.jahiaauth.service.SettingsService;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthService;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@Component(service = Action.class)
public class AwsCognitoConnectAction extends Action {
    private static final String NAME = "connectToAwsCognitoAction";

    private SettingsService settingsService;
    private JahiaOAuthService jahiaOAuthService;

    @Reference
    private void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Reference
    private void setJahiaOAuthService(JahiaOAuthService jahiaOAuthService) {
        this.jahiaOAuthService = jahiaOAuthService;
    }

    public AwsCognitoConnectAction() {
        setName(NAME);
        setRequireAuthenticatedUser(false);
        setRequiredMethods(Render.METHOD_GET);
    }

    @Override
    public ActionResult doExecute(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper jcrSessionWrapper, Map<String, List<String>> parameters, URLResolver urlResolver) {
        String referer = httpServletRequest.getHeader("Referer");
        if (StringUtils.isNotBlank(referer)) {
            httpServletRequest.getSession(false).setAttribute(AwsCognitoConstants.SESSION_OAUTH_AWS_COGNITO_RETURN_URL, referer);
        }
        String authorizationUrl = AwsCognitoConstants.getAuthorizationUrl(renderContext.getSite().getSiteKey(), httpServletRequest.getSession().getId(), settingsService, jahiaOAuthService, false);
        if (authorizationUrl == null) {
            return ActionResult.BAD_REQUEST;
        }
        return new ActionResult(HttpServletResponse.SC_OK, authorizationUrl, true, null);
    }
}
