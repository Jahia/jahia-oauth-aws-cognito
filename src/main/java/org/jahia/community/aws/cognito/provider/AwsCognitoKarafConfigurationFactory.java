package org.jahia.community.aws.cognito.provider;

import org.jahia.community.aws.cognito.provider.client.AwsCognitoClientService;
import org.jahia.modules.external.users.ExternalUserGroupService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

@Component(service = {AwsCognitoKarafConfigurationFactory.class, ManagedServiceFactory.class}, property = Constants.SERVICE_PID + "=org.jahia.community.aws.cognito.provider", immediate = true)
public class AwsCognitoKarafConfigurationFactory implements ManagedServiceFactory {
    private static final Logger logger = LoggerFactory.getLogger(AwsCognitoKarafConfigurationFactory.class);

    private ConfigurationAdmin configurationAdmin;
    private AwsCognitoCacheManager awsCognitoCacheManager;
    private AwsCognitoClientService awsCognitoClientService;
    private ExternalUserGroupService externalUserGroupService;
    private BundleContext bundleContext;

    private final Map<String, AwsCognitoKarafConfiguration> awsCognitoConfigurations;
    private final Map<String, String> pidsByProviderKey;

    public AwsCognitoKarafConfigurationFactory() {
        awsCognitoConfigurations = new HashMap<>();
        pidsByProviderKey = new HashMap<>();
    }

    @Reference
    private void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    @Reference
    private void setAwsCognitoCacheManager(AwsCognitoCacheManager awsCognitoCacheManager) {
        this.awsCognitoCacheManager = awsCognitoCacheManager;
    }

    @Reference
    private void setAwsCognitoClientService(AwsCognitoClientService awsCognitoClientService) {
        this.awsCognitoClientService = awsCognitoClientService;
    }

    @Reference
    private void setExternalUserGroupService(ExternalUserGroupService externalUserGroupService) {
        this.externalUserGroupService = externalUserGroupService;
    }

    @Activate
    private void onActivate(BundleContext context) {
        this.bundleContext = context;
    }

    @Deactivate
    private void onDeactivate() {
        for (AwsCognitoKarafConfiguration config : awsCognitoConfigurations.values()) {
            config.unregister();
        }
        awsCognitoConfigurations.clear();
    }


    @Override
    public void updated(String pid, Dictionary<String, ?> dictionary) {
        AwsCognitoKarafConfiguration awsCognitoKarafConfiguration;
        if (awsCognitoConfigurations.containsKey(pid)) {
            awsCognitoKarafConfiguration = awsCognitoConfigurations.get(pid);
        } else {
            awsCognitoKarafConfiguration = new AwsCognitoKarafConfiguration(dictionary);
            awsCognitoConfigurations.put(pid, awsCognitoKarafConfiguration);
            deleteConfig(pidsByProviderKey.put(awsCognitoKarafConfiguration.getProviderKey(), pid));
        }
        awsCognitoKarafConfiguration.setContext(externalUserGroupService, awsCognitoCacheManager, awsCognitoClientService, bundleContext, dictionary);
        awsCognitoCacheManager.flushCaches();
    }

    private void deleteConfig(String pid) {
        if (pid == null) {
            return;
        }
        try {
            Configuration cfg = configurationAdmin.getConfiguration(pid);
            if (cfg != null) {
                cfg.delete();
            }
        } catch (IOException e) {
            logger.error("Unable to delete AwsCognito configuration for pid " + pid, e);
        }
    }

    @Override
    public void deleted(String pid) {
        AwsCognitoKarafConfiguration awsCognitoKarafConfiguration = awsCognitoConfigurations.remove(pid);
        String existingPid = awsCognitoKarafConfiguration != null ? pidsByProviderKey.get(awsCognitoKarafConfiguration.getProviderKey()) : null;
        if (existingPid != null && existingPid.equals(pid)) {
            pidsByProviderKey.remove(awsCognitoKarafConfiguration.getProviderKey());
            awsCognitoKarafConfiguration.unregister();
            awsCognitoCacheManager.flushCaches();
        }
    }

    public String getName() {
        return AwsCognitoKarafConfigurationFactory.class.getPackage().getName();
    }

    public String getConfigPID(String providerKey) {
        return pidsByProviderKey.get(providerKey);
    }
}
