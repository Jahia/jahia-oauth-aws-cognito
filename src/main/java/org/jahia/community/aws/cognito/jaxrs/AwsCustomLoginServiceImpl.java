package org.jahia.community.aws.cognito.jaxrs;

import org.jahia.community.aws.cognito.api.AwsCognitoConfiguration;
import org.jahia.community.aws.cognito.api.AwsCognitoConstants;
import org.jahia.community.aws.cognito.api.AwsCustomLoginService;
import org.jahia.params.valves.LoginEngineAuthValveImpl;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.usermanager.JahiaUser;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Calendar;

@Component(service = AwsCustomLoginService.class, property = Constants.SERVICE_RANKING + ":Integer=0")
public class AwsCustomLoginServiceImpl implements AwsCustomLoginService {
    private static final Logger logger = LoggerFactory.getLogger(AwsCustomLoginServiceImpl.class);

    @Override
    public Response login(JCRUserNode jcrUserNode, HttpServletRequest httpServletRequest, String siteKey, AwsCognitoConfiguration awsCognitoConfiguration) {
        JahiaUser jahiaUser = jcrUserNode.getJahiaUser();
        httpServletRequest.getSession().invalidate();
        // user has been successfully authenticated, note this in the current session.
        httpServletRequest.getSession().setAttribute(org.jahia.api.Constants.SESSION_USER, jahiaUser);
        httpServletRequest.setAttribute(LoginEngineAuthValveImpl.VALVE_RESULT, LoginEngineAuthValveImpl.OK);

        try {
            jcrUserNode.setProperty(AwsCognitoConstants.USER_PROPERTY_LAST_CONNECTION_DATE, Calendar.getInstance());
            jcrUserNode.saveSession();
        } catch (RepositoryException e) {
            logger.error("", e);
        }

        String returnUrl = (String) httpServletRequest.getSession(false).getAttribute(AwsCognitoConstants.SESSION_OAUTH_AWS_COGNITO_RETURN_URL);
        if (returnUrl == null) {
            returnUrl = "/";
        }
        return Response.seeOther(URI.create(returnUrl)).build();
    }
}
