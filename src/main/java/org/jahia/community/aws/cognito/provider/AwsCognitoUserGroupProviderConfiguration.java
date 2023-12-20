package org.jahia.community.aws.cognito.provider;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.api.settings.SettingsBean;
import org.jahia.community.aws.cognito.api.AwsCognitoConstants;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.modules.external.users.UserGroupProviderConfiguration;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRContentUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;

@Component(service = UserGroupProviderConfiguration.class)
public class AwsCognitoUserGroupProviderConfiguration implements UserGroupProviderConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(AwsCognitoUserGroupProviderConfiguration.class);
    private static final long serialVersionUID = 574584048983934991L;

    protected static final String PROVIDER_KEY_PROP = AwsCognitoConstants.PROVIDER_KEY + ".provider.key";

    private AwsCognitoKarafConfigurationFactory awsCognitoKarafConfigurationFactory;
    private ConfigurationAdmin configurationAdmin;
    private SettingsBean settingsBean;
    private String moduleKey;

    @Reference
    private void setAwsCognitoConfigurationFactory(AwsCognitoKarafConfigurationFactory awsCognitoKarafConfigurationFactory) {
        this.awsCognitoKarafConfigurationFactory = awsCognitoKarafConfigurationFactory;
    }

    @Reference
    private void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    @Reference
    private void setSettingsBean(SettingsBean settingsBean) {
        this.settingsBean = settingsBean;
    }

    @Activate
    private void onActivate(BundleContext bundleContext) {
        moduleKey = BundleUtils.getModule(bundleContext.getBundle()).getId();
    }

    public AwsCognitoUserGroupProviderConfiguration() {
        moduleKey = "jahia-oauth-aws-cognito";
    }

    @Override
    public String getProviderClass() {
        return AwsCognitoUserGroupProvider.class.getCanonicalName();
    }

    @Override
    public String getName() {
        return AwsCognitoConstants.PROVIDER_KEY;
    }

    @Override
    public boolean isCreateSupported() {
        return true;
    }

    @Override
    public String getCreateJSP() {
        return "/modules/" + moduleKey + "/userGroupProviderEdit.jsp";
    }

    private static Properties getProperties(Map<String, Object> parameters) {
        Properties properties = new Properties();
        if (parameters.containsKey("configName")) {
            properties.put("configName", parameters.get("configName"));
        }
        if (parameters.containsKey("propValue." + AwsCognitoConstants.TARGET_SITE) && StringUtils.isNotBlank((String) parameters.get("propValue." + AwsCognitoConstants.TARGET_SITE))) {
            properties.put(AwsCognitoConstants.TARGET_SITE, parameters.get("propValue." + AwsCognitoConstants.TARGET_SITE));
        }
        if (parameters.containsKey("propValue." + AwsCognitoConstants.ACCESS_KEY_ID)) {
            properties.put(AwsCognitoConstants.ACCESS_KEY_ID, parameters.get("propValue." + AwsCognitoConstants.ACCESS_KEY_ID));
        }
        if (parameters.containsKey("propValue." + AwsCognitoConstants.SECRET_ACCESS_KEY)) {
            properties.put(AwsCognitoConstants.SECRET_ACCESS_KEY, parameters.get("propValue." + AwsCognitoConstants.SECRET_ACCESS_KEY));
        }
        if (parameters.containsKey("propValue." + AwsCognitoConstants.USER_POOL_ID)) {
            properties.put(AwsCognitoConstants.USER_POOL_ID, parameters.get("propValue." + AwsCognitoConstants.USER_POOL_ID));
        }
        return properties;
    }

    @Override
    public String create(Map<String, Object> parameters, Map<String, Object> flashScope) throws Exception {
        Properties properties = getProperties(parameters);
        flashScope.put(AwsCognitoConstants.PROVIDER_KEY + "Properties", properties);

        // config name
        String configName = (String) parameters.get("configName");
        if (StringUtils.isBlank(configName)) {
            // if we didn't provide a not-blank config name, generate one
            configName = AwsCognitoConstants.PROVIDER_KEY + System.currentTimeMillis();
        }
        // normalize the name
        configName = JCRContentUtils.generateNodeName(configName);
        flashScope.put("configName", configName);

        // provider key
        String providerKey = AwsCognitoConstants.PROVIDER_KEY + "." + configName;
        configName = awsCognitoKarafConfigurationFactory.getName() + "-" + configName + ".cfg";

        // check that we don't already have a provider with that key
        String pid = awsCognitoKarafConfigurationFactory.getConfigPID(providerKey);
        if (pid != null) {
            throw new Exception("An " + AwsCognitoConstants.PROVIDER_KEY + " provider with key '" + providerKey + "' already exists");
        }


        File folder = new File(settingsBean.getJahiaVarDiskPath(), "karaf/etc");
        if (folder.exists()) {
            FileOutputStream out = new FileOutputStream(new File(folder, configName));
            try {
                properties.store(out, "");
            } finally {
                IOUtils.closeQuietly(out);
            }
        } else {
            Configuration configuration = configurationAdmin.createFactoryConfiguration(awsCognitoKarafConfigurationFactory.getName());
            properties.put(PROVIDER_KEY_PROP, providerKey);
            configuration.update((Dictionary) properties);
        }
        return providerKey;
    }

    @Override
    public boolean isEditSupported() {
        return true;
    }

    @Override
    public String getEditJSP() {
        return "/modules/" + moduleKey + "/userGroupProviderEdit.jsp";
    }

    private File getConfigFile(String providerKey) {
        String configName;
        if (AwsCognitoConstants.PROVIDER_KEY.equals(providerKey)) {
            configName = awsCognitoKarafConfigurationFactory.getName() + "-config.cfg";
        } else if (providerKey.startsWith(AwsCognitoConstants.PROVIDER_KEY + ".")) {
            configName = awsCognitoKarafConfigurationFactory.getName() + "-" + providerKey.substring((AwsCognitoConstants.PROVIDER_KEY + ".").length()) + ".cfg";
        } else {
            throw new JahiaRuntimeException("Wrong provider key: " + providerKey);
        }
        File file = new File(settingsBean.getJahiaVarDiskPath(), "karaf/etc/" + configName);
        if (!file.exists()) {
            file = new File(settingsBean.getJahiaVarDiskPath(), "modules/" + configName);
        }
        return file;
    }

    @Override
    public void edit(String providerKey, Map<String, Object> parameters, Map<String, Object> flashScope) throws Exception {
        Properties properties = getProperties(parameters);
        flashScope.put(AwsCognitoConstants.PROVIDER_KEY + "Properties", properties);

        File file = getConfigFile(providerKey);
        if (file.exists()) {
            FileOutputStream out = new FileOutputStream(file);
            try {
                properties.store(out, "");
            } finally {
                IOUtils.closeQuietly(out);
            }
        } else {
            String pid = awsCognitoKarafConfigurationFactory.getConfigPID(providerKey);
            if (pid == null) {
                throw new Exception("Cannot find " + AwsCognitoConstants.PROVIDER_KEY + " provider " + providerKey);
            }
            Configuration configuration = configurationAdmin.getConfiguration(pid);
            properties.put(PROVIDER_KEY_PROP, providerKey);
            configuration.update((Dictionary) properties);
        }
    }

    @Override
    public boolean isDeleteSupported() {
        return true;
    }

    @Override
    public void delete(String providerKey, Map<String, Object> flashScope) throws Exception {
        File file = getConfigFile(providerKey);
        if (file.exists()) {
            if (!FileUtils.deleteQuietly(file)) {
                logger.error("Unable to delete the configuration file: {}", file.getPath());
            }
        } else {
            String pid = awsCognitoKarafConfigurationFactory.getConfigPID(providerKey);
            if (pid == null) {
                throw new JahiaRuntimeException("Cannot find provider " + providerKey);
            }
            configurationAdmin.getConfiguration(pid).delete();
        }
    }
}
