package org.jahia.community.aws.cognito.jaxrs;

import org.apache.commons.lang.StringUtils;
import org.jahia.api.content.JCRTemplate;
import org.jahia.community.aws.cognito.api.AwsCognitoConfiguration;
import org.jahia.community.aws.cognito.api.AwsCustomLoginService;
import org.jahia.community.aws.cognito.connector.AwsCognitoConnector;
import org.jahia.community.aws.cognito.connector.AwsCognitoLoginUrlProvider;
import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.SettingsService;
import org.jahia.osgi.BundleUtils;
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

    private static AwsCognitoConfiguration getAwsCognitoConfiguration(HttpServletRequest httpServletRequest) {
        String siteKey = AwsCognitoLoginUrlProvider.getSiteKey(httpServletRequest, BundleUtils.getOsgiService(JCRTemplate.class, null), BundleUtils.getOsgiService(JahiaSitesService.class, null));
        if (siteKey == null) {
            logger.warn("Site not found.");
            return null;
        }
        ConnectorConfig connectorConfig = getConnectorConfig(siteKey);
        if (connectorConfig == null) {
            logger.warn("The site {} doesn't have the AWS Cognito configuration", siteKey);
            return null;
        }
        return new AwsCognitoConfiguration(
                null,
                connectorConfig.getProperty(AwsCognitoConfiguration.ACCESS_KEY_ID),
                connectorConfig.getProperty(AwsCognitoConfiguration.SECRET_ACCESS_KEY),
                connectorConfig.getProperty(AwsCognitoConfiguration.USER_POOL_ID)
        );
    }

    private static ConnectorConfig getConnectorConfig(String siteKey) {
        SettingsService settingsService = BundleUtils.getOsgiService(SettingsService.class, null);
        ConnectorConfig connectorConfig = settingsService.getConnectorConfig(siteKey, AwsCognitoConnector.KEY);
        if (connectorConfig == null) {
            // fallback to systemsite
            connectorConfig = settingsService.getConnectorConfig(JahiaSitesService.SYSTEM_SITE_KEY, AwsCognitoConnector.KEY);
        }
        return connectorConfig;
    }

    @GET
    public Response getData(@QueryParam("user") String body, @Context HttpServletRequest httpServletRequest) {
        try {
            String siteKey = AwsCognitoLoginUrlProvider.getSiteKey(httpServletRequest, BundleUtils.getOsgiService(JCRTemplate.class, null), BundleUtils.getOsgiService(JahiaSitesService.class, null));
            if (siteKey == null) {
                logger.warn("Site not found.");
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            ConnectorConfig connectorConfig = getConnectorConfig(siteKey);
            if (connectorConfig == null) {
                // no configuration found
                logger.warn("The site {} doesn't have the AWS Cognito configuration", siteKey);
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            String secretKey = connectorConfig.getProperty(AwsCognitoConfiguration.SECRET_KEY);
            String subject = decryptUser(body, secretKey);
            if (subject == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            AwsCustomLoginService awsCustomLoginService = BundleUtils.getOsgiService(AwsCustomLoginService.class, null);
            if (awsCustomLoginService == null) {
                logger.warn("No AWS custom login service to login the user: {}", subject);
            }
            AwsCognitoConfiguration awsCognitoConfiguration = getAwsCognitoConfiguration(httpServletRequest);
            if (awsCognitoConfiguration == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            if (awsCustomLoginService != null && !awsCustomLoginService.login(subject, httpServletRequest, siteKey, awsCognitoConfiguration)) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            String returnUrl = (String) httpServletRequest.getSession(false).getAttribute(AwsCognitoLoginUrlProvider.SESSION_OAUTH_AWS_COGNITO_RETURN_URL);
            if (returnUrl == null) {
                returnUrl = "/";
            }
            return Response.seeOther(URI.create(returnUrl)).build();
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
}
