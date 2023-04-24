package org.jahia.community.aws.cognito.provider;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.community.aws.cognito.provider.client.AwsCognitoGroup;
import org.jahia.community.aws.cognito.provider.client.AwsCognitoUser;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.cache.CacheHelper;
import org.jahia.services.cache.ModuleClassLoaderAwareCacheEntry;
import org.jahia.services.cache.ehcache.EhCacheProvider;
import org.jahia.services.usermanager.JahiaGroupImpl;
import org.jahia.services.usermanager.JahiaUserImpl;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

@Component(service = AwsCognitoCacheManager.class)
public class AwsCognitoCacheManager {
    private static final String USER_CACHE = "AwsCognitoUsersCache";
    private static final String GROUP_CACHE = "AwsCognitoGroupsCache";
    private static final int TIME_TO_IDLE = 3600;

    private static final Logger logger = LoggerFactory.getLogger(AwsCognitoCacheManager.class);

    private Ehcache groupCache;
    private Ehcache userCache;

    @Activate
    private void onActivate() {
        EhCacheProvider cacheProvider = (EhCacheProvider) SpringContextSingleton.getInstance().getContext().getBean("ehCacheProvider");
        final CacheManager cacheManager = cacheProvider.getCacheManager();
        userCache = cacheManager.getCache(USER_CACHE);
        if (userCache == null) {
            userCache = createCache(cacheManager, USER_CACHE);
        } else {
            userCache.removeAll();
        }
        groupCache = cacheManager.getCache(GROUP_CACHE);
        if (groupCache == null) {
            groupCache = createCache(cacheManager, GROUP_CACHE);
        } else {
            groupCache.removeAll();
        }
    }

    private static Ehcache createCache(CacheManager cacheManager, String cacheName) {
        CacheConfiguration cacheConfiguration = cacheManager.getConfiguration().getDefaultCacheConfiguration() != null ?
                cacheManager.getConfiguration().getDefaultCacheConfiguration().clone() :
                new CacheConfiguration();
        cacheConfiguration.setName(cacheName);
        cacheConfiguration.setEternal(false);
        cacheConfiguration.setTimeToIdleSeconds(TIME_TO_IDLE);
        // Create a new cache with the configuration
        Ehcache cache = new Cache(cacheConfiguration);
        cache.setName(cacheName);
        // Cache name has been set now we can initialize it by putting it in the manager.
        // Only Cache manager is initializing caches.
        return cacheManager.addCacheIfAbsent(cache);
    }

    @Deactivate
    private void onDeactivate() {
        // flush
        if (userCache != null) {
            userCache.removeAll();
        }
        if (groupCache != null) {
            groupCache.removeAll();
        }
    }

    public Optional<AwsCognitoUser> getUser(String providerKey, String siteKey, String username) {
        return Optional.ofNullable((AwsCognitoUser) CacheHelper.getObjectValue(userCache, getCacheNameKey(providerKey, siteKey, username)));
    }

    public Optional<AwsCognitoUser> getOrRefreshUser(String providerKey, String siteKey, String username, Supplier<Optional<AwsCognitoUser>> supplier) {
        return getUser(providerKey, siteKey, username).map(Optional::of).orElseGet(() -> {
            Optional<AwsCognitoUser> awsCognitoUser = supplier.get();
            awsCognitoUser.ifPresent(user -> cacheUser(providerKey, siteKey, user));
            return awsCognitoUser;
        });
    }

    public void cacheUser(String providerKey, String siteKey, AwsCognitoUser awsCognitoUser) {
        if (logger.isDebugEnabled()) {
            logger.debug("Caching user {} in site {}", awsCognitoUser.getUsername(), siteKey);
        }
        Properties properties = new Properties();
        awsCognitoUser.setJahiaUser(new JahiaUserImpl(awsCognitoUser.getUsername(), awsCognitoUser.getUsername(), properties, false, providerKey, siteKey));
        ModuleClassLoaderAwareCacheEntry cacheEntry = new ModuleClassLoaderAwareCacheEntry(awsCognitoUser, AwsCognitoUserGroupProviderConfiguration.KEY);
        userCache.put(new Element(getCacheNameKey(providerKey, siteKey, awsCognitoUser.getUsername()), cacheEntry));
    }

    public Optional<AwsCognitoGroup> getGroup(String providerKey, String siteKey, String groupname) {
        return Optional.ofNullable((AwsCognitoGroup) CacheHelper.getObjectValue(groupCache, getCacheNameKey(providerKey, siteKey, groupname)));
    }

    public Optional<AwsCognitoGroup> getOrRefreshGroup(String providerKey, String siteKey, String groupname, Supplier<Optional<AwsCognitoGroup>> supplier) {
        return getGroup(providerKey, siteKey, groupname).map(Optional::of).orElseGet(() -> {
            Optional<AwsCognitoGroup> awsCognitoGroup = supplier.get();
            awsCognitoGroup.ifPresent(group -> cacheGroup(providerKey, siteKey, group));
            return awsCognitoGroup;
        });
    }

    public void cacheGroup(String providerKey, String siteKey, AwsCognitoGroup awsCognitoGroup) {
        if (logger.isDebugEnabled()) {
            logger.debug("Caching group ({}): {} in site {}", awsCognitoGroup.getId(), awsCognitoGroup.getName(), siteKey);
        }
        Properties properties = new Properties();
        properties.put(Constants.JCR_TITLE, awsCognitoGroup.getName());
        if (StringUtils.isNotBlank(awsCognitoGroup.getDescription())) {
            properties.put(Constants.JCR_DESCRIPTION, awsCognitoGroup.getDescription());
        }
        awsCognitoGroup.setJahiaGroup(new JahiaGroupImpl(awsCognitoGroup.getId(), awsCognitoGroup.getId(), siteKey, properties));
        ModuleClassLoaderAwareCacheEntry cacheEntry = new ModuleClassLoaderAwareCacheEntry(awsCognitoGroup, AwsCognitoUserGroupProviderConfiguration.KEY);
        groupCache.put(new Element(getCacheNameKey(providerKey, siteKey, awsCognitoGroup.getId()), cacheEntry));
    }

    private static String getCacheNameKey(String providerKey, String siteKey, String objectName) {
        return providerKey + "_" + siteKey + "_" + AwsCognitoUserGroupProviderConfiguration.KEY + "_" + objectName;
    }

    public void flushCaches() {
        CacheHelper.flushEhcacheByName("org.jahia.services.usermanager.JahiaUserManagerService.userPathByUserNameCache", true);
        CacheHelper.flushEhcacheByName("org.jahia.services.usermanager.JahiaGroupManagerService.groupPathByGroupNameCache", true);
        CacheHelper.flushEhcacheByName("org.jahia.services.usermanager.JahiaGroupManagerService.membershipCache", true);
        CacheHelper.flushEhcacheByName(USER_CACHE, true);
        CacheHelper.flushEhcacheByName(GROUP_CACHE, true);
    }
}
