package org.jahia.community.aws.cognito.connector;

import org.jahia.community.aws.cognito.api.AwsCognitoConstants;
import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.ConnectorResultProcessor;
import org.jahia.modules.jahiaauth.service.JahiaAuthConstants;
import org.jahia.modules.jahiaoauth.service.JahiaOAuthConstants;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component(service = ConnectorResultProcessor.class)
public class AwsCognitoUserMapper implements ConnectorResultProcessor {
    private static final Logger logger = LoggerFactory.getLogger(AwsCognitoUserMapper.class);

    @Override
    public void execute(ConnectorConfig connectorConfig, Map<String, Object> results) {
        try {
            JSONObject tokenData = getTokenData(results);
            if (tokenData == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Error parsing OpenID Token: {}", results);
                }
            } else if (tokenData.has(AwsCognitoConstants.SSO_LOGIN)) {
                RequestContextHolder.getRequestAttributes()
                        .setAttribute(JahiaAuthConstants.SSO_LOGIN, tokenData.getString(AwsCognitoConstants.SSO_LOGIN), RequestAttributes.SCOPE_REQUEST);
            }
        } catch (JSONException e) {
            logger.error("Error parsing OpenID Token");
            if (logger.isDebugEnabled()) {
                logger.debug("", e);
            }
        }
    }

    private JSONObject getTokenData(Map<String, Object> results) throws JSONException {
        Map<String, String> tokenData = (Map<String, String>) results.get(JahiaOAuthConstants.TOKEN_DATA);
        if (tokenData.containsKey(JahiaOAuthConstants.OPEN_ID_TOKEN)) {
            String token = tokenData.get(JahiaOAuthConstants.OPEN_ID_TOKEN);
            if (token != null) {
                String[] chunks = token.split("\\.");
                return new JSONObject(new String(Base64.getUrlDecoder().decode(chunks[1]), StandardCharsets.UTF_8));
            }
        }
        return null;
    }
}
