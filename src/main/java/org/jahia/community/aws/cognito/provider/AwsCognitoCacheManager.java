package org.jahia.community.aws.cognito.provider;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import org.jahia.community.aws.cognito.api.AwsCognitoConstants;
import org.jahia.community.aws.cognito.client.AwsCognitoGroup;
import org.jahia.community.aws.cognito.client.AwsCognitoUser;
import org.jahia.services.cache.CacheHelper;
import org.jahia.services.cache.CacheProvider;
import org.jahia.services.cache.ModuleClassLoaderAwareCacheEntry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Component(service = AwsCognitoCacheManager.class)
public class AwsCognitoCacheManager {
    private static final String MODULE_NAME = "jahia-oauth-aws-cognito";
    private static final String USER_CACHE = "AwsCognitoUsersCache";
    private static final String GROUP_CACHE = "AwsCognitoGroupsCache";
    private static final int TIME_TO_IDLE = 3600;

    private static final Logger logger = LoggerFactory.getLogger(AwsCognitoCacheManager.class);

    private CacheProvider cacheProvider;
    private Ehcache groupCache;
    private Ehcache userCache;

    @Reference
    private void setCacheProvider(CacheProvider cacheProvider) {
        this.cacheProvider = cacheProvider;
    }

    @Activate
    private void onActivate() {
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

    public Optional<AwsCognitoUser> getUser(String providerKey, String siteKey, String attribute) {
        return Optional.ofNullable((AwsCognitoUser) CacheHelper.getObjectValue(userCache, getCacheNameKey(providerKey, siteKey, attribute)));
    }

    public Optional<AwsCognitoUser> getOrRefreshUser(String providerKey, String siteKey, String attribute, Supplier<Optional<AwsCognitoUser>> supplier) {
        return getUser(providerKey, siteKey, attribute).map(Optional::of).orElseGet(() -> {
            logger.debug("User {} not found in the cache", attribute);
            Optional<AwsCognitoUser> awsCognitoUser = supplier.get();
            awsCognitoUser.ifPresent(user -> cacheUser(providerKey, siteKey, user));
            return awsCognitoUser;
        });
    }

    public void cacheUser(String providerKey, String siteKey, AwsCognitoUser awsCognitoUser) {
        if (logger.isDebugEnabled()) {
            logger.debug("Caching user {} in site {}", awsCognitoUser.getUsername(), siteKey);
        }
        ModuleClassLoaderAwareCacheEntry cacheEntry = new ModuleClassLoaderAwareCacheEntry(awsCognitoUser, MODULE_NAME);
        userCache.put(new Element(getCacheNameKey(providerKey, siteKey, awsCognitoUser.cacheJahiaUser(providerKey, siteKey)), cacheEntry));
    }

    public Optional<List<AwsCognitoUser>> getUsers(String providerKey, String siteKey, int offset, int limit, Supplier<Optional<List<AwsCognitoUser>>> supplier) {
        String cacheKey = "all_" + offset + "_" + limit;
        return Optional.ofNullable((List<AwsCognitoUser>) CacheHelper.getObjectValue(userCache, getCacheNameKey(providerKey, siteKey, cacheKey))).map(Optional::of).orElseGet(() -> {
            Optional<List<AwsCognitoUser>> awsCognitoUsers = supplier.get();
            awsCognitoUsers.ifPresent(users -> {
                if (logger.isDebugEnabled()) {
                    logger.debug("Caching users {} in site {}", cacheKey, siteKey);
                }
                ModuleClassLoaderAwareCacheEntry cacheEntry = new ModuleClassLoaderAwareCacheEntry(users, MODULE_NAME);
                userCache.put(new Element(getCacheNameKey(providerKey, siteKey, cacheKey), cacheEntry));
            });
            return awsCognitoUsers;
        });
    }

    public Optional<List<AwsCognitoGroup>> getGroups(String providerKey, String siteKey, int offset, int limit, Supplier<Optional<List<AwsCognitoGroup>>> supplier) {
        String cacheKey = "all_" + offset + "_" + limit;
        return Optional.ofNullable((List<AwsCognitoGroup>) CacheHelper.getObjectValue(userCache, getCacheNameKey(providerKey, siteKey, cacheKey))).map(Optional::of).orElseGet(() -> {
            Optional<List<AwsCognitoGroup>> awsCognitoUsers = supplier.get();
            awsCognitoUsers.ifPresent(users -> {
                if (logger.isDebugEnabled()) {
                    logger.debug("Caching users {} in site {}", cacheKey, siteKey);
                }
                ModuleClassLoaderAwareCacheEntry cacheEntry = new ModuleClassLoaderAwareCacheEntry(users, MODULE_NAME);
                userCache.put(new Element(getCacheNameKey(providerKey, siteKey, cacheKey), cacheEntry));
            });
            return awsCognitoUsers;
        });
    }

    public Optional<AwsCognitoGroup> getGroup(String providerKey, String siteKey, String groupname) {
        return Optional.ofNullable((AwsCognitoGroup) CacheHelper.getObjectValue(groupCache, getCacheNameKey(providerKey, siteKey, groupname)));
    }

    public Optional<AwsCognitoGroup> getOrRefreshGroup(String providerKey, String siteKey, String groupname, Supplier<Optional<AwsCognitoGroup>> supplier) {
        return getGroup(providerKey, siteKey, groupname).map(Optional::of).orElseGet(() -> {
            logger.debug("Group {} not found in the cache", groupname);
            Optional<AwsCognitoGroup> awsCognitoGroup = supplier.get();
            awsCognitoGroup.ifPresent(group -> cacheGroup(providerKey, siteKey, group));
            return awsCognitoGroup;
        });
    }

    public void cacheGroup(String providerKey, String siteKey, AwsCognitoGroup awsCognitoGroup) {
        if (logger.isDebugEnabled()) {
            logger.debug("Caching group ({}): {} in site {}", awsCognitoGroup.getName(), awsCognitoGroup.getName(), siteKey);
        }
        ModuleClassLoaderAwareCacheEntry cacheEntry = new ModuleClassLoaderAwareCacheEntry(awsCognitoGroup, MODULE_NAME);
        groupCache.put(new Element(getCacheNameKey(providerKey, siteKey, awsCognitoGroup.cacheGroup(siteKey)), cacheEntry));
    }

    private static String getCacheNameKey(String providerKey, String siteKey, String objectName) {
        return providerKey + "_" + siteKey + "_" + AwsCognitoConstants.PROVIDER_KEY + "_" + objectName;
    }

    public void flushCaches() {
        CacheHelper.flushEhcacheByName("org.jahia.services.usermanager.JahiaUserManagerService.userPathByUserNameCache", true);
        CacheHelper.flushEhcacheByName("org.jahia.services.usermanager.JahiaGroupManagerService.groupPathByGroupNameCache", true);
        CacheHelper.flushEhcacheByName("org.jahia.services.usermanager.JahiaGroupManagerService.membershipCache", true);
        CacheHelper.flushEhcacheByName(USER_CACHE, true);
        CacheHelper.flushEhcacheByName(GROUP_CACHE, true);
    }
}
