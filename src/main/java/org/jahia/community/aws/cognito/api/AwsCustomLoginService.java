package org.jahia.community.aws.cognito.api;

import javax.servlet.http.HttpServletRequest;

public interface AwsCustomLoginService {
    boolean login(String userIdentifier, HttpServletRequest httpServletRequest, String siteKey, AwsCognitoConfiguration awsCognitoConfiguration);
}
