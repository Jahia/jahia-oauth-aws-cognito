package org.jahia.community.aws.cognito.provider;

import org.apache.commons.lang.StringUtils;
import org.jahia.community.aws.cognito.api.AwsCognitoConfiguration;
import org.jahia.community.aws.cognito.api.AwsCognitoConstants;
import org.jahia.community.aws.cognito.client.AwsCognitoClientService;
import org.jahia.modules.external.users.ExternalUserGroupService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

public class AwsCognitoKarafConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(AwsCognitoKarafConfiguration.class);

    private final String providerKey;
    private AwsCognitoUserGroupProvider awsCognitoUserGroupProvider;

    public AwsCognitoKarafConfiguration(Dictionary<String, ?> dictionary) {
        providerKey = computeProviderKey(dictionary);
    }

    private static String computeProviderKey(Dictionary<String, ?> dictionary) {
        String provideKey = (String) dictionary.get(AwsCognitoUserGroupProviderConfiguration.PROVIDER_KEY_PROP);
        if (provideKey != null) {
            return provideKey;
        }
        String filename = (String) dictionary.get("felix.fileinstall.filename");
        String factoryPid = (String) dictionary.get(ConfigurationAdmin.SERVICE_FACTORYPID);
        String confId;
        if (StringUtils.isBlank(filename)) {
            confId = (String) dictionary.get(Constants.SERVICE_PID);
            if (StringUtils.startsWith(confId, factoryPid + ".")) {
                confId = StringUtils.substringAfter(confId, factoryPid + ".");
            }
        } else {
            confId = StringUtils.removeEnd(StringUtils.substringAfter(filename, factoryPid + "-"), ".cfg");
        }
        return (StringUtils.isBlank(confId) || "config".equals(confId)) ? AwsCognitoUserGroupProviderConfiguration.KEY : (AwsCognitoUserGroupProviderConfiguration.KEY + "." + confId);
    }

    public String getProviderKey() {
        return providerKey;
    }

    public void setContext(ExternalUserGroupService externalUserGroupService, AwsCognitoCacheManager awsCognitoCacheManager, AwsCognitoClientService awsCognitoClientService, BundleContext bundleContext, Dictionary<String, ?> dictionary) {
        if (awsCognitoUserGroupProvider == null) {
            awsCognitoUserGroupProvider = new AwsCognitoUserGroupProvider(awsCognitoCacheManager, awsCognitoClientService);
            awsCognitoUserGroupProvider.setExternalUserGroupService(externalUserGroupService);
            awsCognitoUserGroupProvider.setBundleContext(bundleContext);
        } else {
            // Deactivate the provider before reconfiguring it.
            awsCognitoUserGroupProvider.unregister();
        }

        awsCognitoUserGroupProvider.setKey(providerKey);

        awsCognitoUserGroupProvider.setAwsCognitoConfiguration(new AwsCognitoConfiguration(
                (String) dictionary.get(AwsCognitoConstants.TARGET_SITE),
                (String) dictionary.get(AwsCognitoConstants.ACCESS_KEY_ID),
                (String) dictionary.get(AwsCognitoConstants.SECRET_ACCESS_KEY),
                (String) dictionary.get(AwsCognitoConstants.USER_POOL_ID),
                null, null, null));
        // Activate (again)
        awsCognitoUserGroupProvider.register();
    }

    public void unregister() {
        if (awsCognitoUserGroupProvider != null) {
            awsCognitoUserGroupProvider.unregister();
            awsCognitoUserGroupProvider = null;
        }
    }
}
