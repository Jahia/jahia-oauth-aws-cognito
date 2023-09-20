package org.jahia.community.aws.cognito.jaxrs;

import org.jahia.api.usermanager.JahiaUserManagerService;
import org.jahia.community.aws.cognito.api.AwsCognitoConfiguration;
import org.jahia.community.aws.cognito.api.AwsCognitoConstants;
import org.jahia.community.aws.cognito.api.AwsCustomLoginService;
import org.jahia.params.valves.LoginEngineAuthValveImpl;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.usermanager.JahiaUser;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.net.URI;

@Component(service = AwsCustomLoginService.class, property = Constants.SERVICE_RANKING + ":Integer=0")
public class AwsCustomLoginServiceImpl implements AwsCustomLoginService {
    private static final Logger logger = LoggerFactory.getLogger(AwsCustomLoginServiceImpl.class);

    private JahiaUserManagerService jahiaUserManagerService;

    @Reference
    private void setJahiaUserManagerService(JahiaUserManagerService jahiaUserManagerService) {
        this.jahiaUserManagerService = jahiaUserManagerService;
    }

    public Response login(String userIdentifier, HttpServletRequest httpServletRequest, String siteKey, AwsCognitoConfiguration awsCognitoConfiguration) {
        JCRUserNode jcrUserNode = jahiaUserManagerService.lookupUser(userIdentifier);
        if (jcrUserNode != null) {
            JahiaUser jahiaUser = jcrUserNode.getJahiaUser();
            httpServletRequest.getSession().invalidate();
            // user has been successfully authenticated, note this in the current session.
            httpServletRequest.getSession().setAttribute(org.jahia.api.Constants.SESSION_USER, jahiaUser);
            httpServletRequest.setAttribute(LoginEngineAuthValveImpl.VALVE_RESULT, LoginEngineAuthValveImpl.OK);

            String returnUrl = (String) httpServletRequest.getSession(false).getAttribute(AwsCognitoConstants.SESSION_OAUTH_AWS_COGNITO_RETURN_URL);
            if (returnUrl == null) {
                returnUrl = "/";
            }
            return Response.seeOther(URI.create(returnUrl)).build();
        }

        logger.warn("Login failed (user {} not found in JCR).", userIdentifier);
        return Response.status(Response.Status.NOT_FOUND).build();
    }
}
