package org.jahia.community.aws.cognito.jaxrs;

import org.jahia.api.usermanager.JahiaUserManagerService;
import org.jahia.community.aws.cognito.api.AwsCognitoConfiguration;
import org.jahia.community.aws.cognito.api.AwsCustomLoginService;
import org.jahia.osgi.BundleUtils;
import org.jahia.params.valves.LoginEngineAuthValveImpl;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.usermanager.JahiaUser;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

@Component(service = AwsCustomLoginService.class, property = Constants.SERVICE_RANKING + ":Integer=0")
public class AwsCustomLoginServiceImpl implements AwsCustomLoginService {
    private static final Logger logger = LoggerFactory.getLogger(AwsCustomLoginServiceImpl.class);

    public boolean login(String userIdentifier, HttpServletRequest httpServletRequest, String siteKey, AwsCognitoConfiguration awsCognitoConfiguration) {
        JCRUserNode jcrUserNode = BundleUtils.getOsgiService(JahiaUserManagerService.class, null).lookupUser(userIdentifier);
        if (jcrUserNode != null) {
            JahiaUser jahiaUser = jcrUserNode.getJahiaUser();
            httpServletRequest.getSession().invalidate();
            // user has been successfully authenticated, note this in the current session.
            httpServletRequest.getSession().setAttribute(org.jahia.api.Constants.SESSION_USER, jahiaUser);
            // eventually set the Jahia user
            httpServletRequest.setAttribute(LoginEngineAuthValveImpl.VALVE_RESULT, LoginEngineAuthValveImpl.OK);
            return true;
        }

        logger.warn("Login failed (user {} not found in JCR).", userIdentifier);
        return false;
    }
}
