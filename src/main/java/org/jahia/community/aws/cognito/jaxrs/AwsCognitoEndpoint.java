package org.jahia.community.aws.cognito.jaxrs;

import org.apache.commons.lang.StringUtils;
import org.jahia.api.content.JCRTemplate;
import org.jahia.api.usermanager.JahiaUserManagerService;
import org.jahia.bin.Logout;
import org.jahia.community.aws.cognito.api.AwsCognitoConfiguration;
import org.jahia.community.aws.cognito.api.AwsCognitoConstants;
import org.jahia.community.aws.cognito.api.AwsCustomLoginService;
import org.jahia.community.aws.cognito.client.AwsCognitoClientService;
import org.jahia.community.aws.cognito.client.AwsCognitoUser;
import org.jahia.community.aws.cognito.provider.AwsCognitoCacheManager;
import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.sites.JahiaSitesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.DigestException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

@Path("/cognito")
public class AwsCognitoEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(AwsCognitoEndpoint.class);

    @GET
    public Response getData(@QueryParam("user") String body, @Context HttpServletRequest httpServletRequest) {
        try {
            String siteKey = AwsCognitoConstants.getSiteKey(httpServletRequest, BundleUtils.getOsgiService(JCRTemplate.class, null), BundleUtils.getOsgiService(JahiaSitesService.class, null));
            if (siteKey == null) {
                logger.warn("Site not found.");
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            ConnectorConfig connectorConfig = AwsCognitoConstants.getConnectorConfig(siteKey);
            if (connectorConfig == null) {
                // no configuration found
                logger.warn("The site {} doesn't have the AWS Cognito configuration", siteKey);
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            String secretKey = connectorConfig.getProperty(AwsCognitoConstants.SECRET_KEY);
            String sub = decryptUser(body, secretKey);
            if (sub == null) {
                logger.warn("No AWS user in body {}", body);
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            AwsCognitoConfiguration awsCognitoConfiguration = AwsCognitoConstants.getAwsCognitoConfiguration(httpServletRequest);
            if (awsCognitoConfiguration == null) {
                logger.warn("No AWS cognito configuration for site {}", siteKey);
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            String username = BundleUtils.getOsgiService(AwsCognitoCacheManager.class, null).getOrRefreshUser(awsCognitoConfiguration.getProviderKey(), awsCognitoConfiguration.getSiteKey(), sub,
                            () -> BundleUtils.getOsgiService(AwsCognitoClientService.class, null).getUser(awsCognitoConfiguration, "sub", sub))
                    .map(AwsCognitoUser::getUsername)
                    .orElse(null);
            if (username == null) {
                logger.error("User not found for sub {}", sub);
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            JCRUserNode jcrUserNode = BundleUtils.getOsgiService(JahiaUserManagerService.class, null).lookupUser(username);
            if (jcrUserNode == null) {
                logger.error("User not found for sub {} and username {}", sub, username);
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            AwsCustomLoginServiceFactory awsCustomLoginServiceFactory = BundleUtils.getOsgiService(AwsCustomLoginServiceFactory.class, null);
            if (awsCustomLoginServiceFactory == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            AwsCustomLoginService awsCustomLoginService = awsCustomLoginServiceFactory.getAwsCustomLoginService(siteKey, jcrUserNode.getSession());
            if (awsCustomLoginService == null) {
                logger.warn("No AWS custom login service to login the user: {}", jcrUserNode.getName());
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            logger.debug("Call custom AWS login service on site {} to authenticate user {} ({})", siteKey, jcrUserNode.getPath(), awsCustomLoginService.getClass().getName());
            return awsCustomLoginService.login(jcrUserNode, httpServletRequest, siteKey, awsCognitoConfiguration);
        } catch (Exception e) {
            logger.warn("", e);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    private static String decryptUser(String userEncrypted, String secretKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, DigestException {
        if (StringUtils.isBlank(secretKey)) {
            return null;
        }

        byte[] cipherData = Base64.getDecoder().decode(userEncrypted);
        byte[] saltData = Arrays.copyOfRange(cipherData, 8, 16);

        MessageDigest md = MessageDigest.getInstance("MD5");
        int digestLength = md.getDigestLength();
        int requiredLength = (32 + 16 + digestLength - 1) / digestLength * digestLength;
        byte[] generatedData = new byte[requiredLength];
        int generatedLength = 0;

        md.reset();

        // Repeat process until sufficient data has been generated
        while (generatedLength < 32 + 16) {
            // Digest data (last digest if available, password data, salt if available)
            if (generatedLength > 0) {
                md.update(generatedData, generatedLength - digestLength, digestLength);
            }
            md.update(secretKey.getBytes(StandardCharsets.UTF_8));
            md.update(saltData, 0, 8);
            md.digest(generatedData, generatedLength, digestLength);
            generatedLength += digestLength;
        }

        byte[] encrypted = Arrays.copyOfRange(cipherData, 16, cipherData.length);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(Arrays.copyOfRange(generatedData, 0, 32), "AES"), new IvParameterSpec(Arrays.copyOfRange(generatedData, 32, 32 + 16)));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    @GET
    @Path("/logout")
    public Response logout(@Context HttpServletRequest httpServletRequest, @Context HttpServletResponse httpServletResponse) {
        try {
            String returnUrl = (String) httpServletRequest.getSession().getAttribute(AwsCognitoConstants.SESSION_OAUTH_AWS_COGNITO_RETURN_URL);
            String logoutUrl = Logout.getLogoutServletPath();
            if (StringUtils.isNotBlank(returnUrl)) {
                logoutUrl += "?redirect=" + returnUrl;
            }
            return Response.status(Response.Status.FOUND).location(new URI(logoutUrl)).build();
        } catch (Exception e) {
            logger.error("");
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
