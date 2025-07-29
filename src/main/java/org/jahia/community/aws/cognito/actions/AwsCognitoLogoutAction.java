package org.jahia.community.aws.cognito.actions;

import org.apache.commons.lang.StringUtils;
import org.jahia.api.content.JCRTemplate;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.bin.Logout;
import org.jahia.bin.Render;
import org.jahia.community.aws.cognito.api.AwsCognitoConstants;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.jahia.services.sites.JahiaSitesService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@Component(service = Action.class)
public class AwsCognitoLogoutAction extends Action {
    private static final Logger logger = LoggerFactory.getLogger(AwsCognitoLogoutAction.class);

    private static final String NAME = "awsCognitoLogoutAction";

    @Reference
    private JCRTemplate jcrTemplate;
    @Reference
    private JahiaSitesService jahiaSitesService;

    public AwsCognitoLogoutAction() {
        setName(NAME);
        setRequiredMethods(Render.METHOD_GET);
    }

    @Override
    public ActionResult doExecute(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper jcrSessionWrapper, Map<String, List<String>> parameters, URLResolver urlResolver) {
        try {
            String siteKey = renderContext.getSite().getSiteKey();

            String returnUrl = (String) httpServletRequest.getSession(false).getAttribute(AwsCognitoConstants.SESSION_OAUTH_AWS_COGNITO_RETURN_URL);
            if (StringUtils.isBlank(returnUrl)) {
                returnUrl = jcrTemplate.doExecuteWithSystemSessionAsUser(null, renderContext.getWorkspace(), renderContext.getMainResourceLocale(), systemSession ->
                        jahiaSitesService.getSiteByKey(siteKey, systemSession).getHome().getUrl());
            }

            String logoutUrl = Logout.getLogoutServletPath();
            if (StringUtils.isNotBlank(returnUrl)) {
                logoutUrl += "?redirect=" + returnUrl;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("AWS Cognito Logout URL: {}", logoutUrl);
            }

            return new ActionResult(HttpServletResponse.SC_OK, logoutUrl, true, null);
        } catch (RepositoryException e) {
            logger.error("", e);
        }
        return ActionResult.BAD_REQUEST;
    }
}
