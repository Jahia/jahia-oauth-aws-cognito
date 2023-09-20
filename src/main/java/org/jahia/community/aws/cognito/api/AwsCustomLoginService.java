package org.jahia.community.aws.cognito.api;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

public interface AwsCustomLoginService {
    Response login(String userIdentifier, HttpServletRequest httpServletRequest, String siteKey, AwsCognitoConfiguration awsCognitoConfiguration);
}
