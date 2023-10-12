package org.jahia.community.aws.cognito.api;

import org.jahia.services.content.decorator.JCRUserNode;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

public interface AwsCustomLoginService {
    Response login(JCRUserNode jcrUserNode, HttpServletRequest httpServletRequest, String siteKey, AwsCognitoConfiguration awsCognitoConfiguration);
}
