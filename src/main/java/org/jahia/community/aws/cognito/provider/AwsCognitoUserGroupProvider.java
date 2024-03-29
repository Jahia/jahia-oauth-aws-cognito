package org.jahia.community.aws.cognito.provider;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.community.aws.cognito.api.AwsCognitoConfiguration;
import org.jahia.community.aws.cognito.api.AwsCognitoConstants;
import org.jahia.community.aws.cognito.client.AwsCognitoClientService;
import org.jahia.community.aws.cognito.client.AwsCognitoGroup;
import org.jahia.community.aws.cognito.client.AwsCognitoUser;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.modules.external.users.BaseUserGroupProvider;
import org.jahia.modules.external.users.GroupNotFoundException;
import org.jahia.modules.external.users.Member;
import org.jahia.modules.external.users.UserNotFoundException;
import org.jahia.services.usermanager.JahiaGroup;
import org.jahia.services.usermanager.JahiaGroupManagerService;
import org.jahia.services.usermanager.JahiaUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

public class AwsCognitoUserGroupProvider extends BaseUserGroupProvider {
    private static final Logger logger = LoggerFactory.getLogger(AwsCognitoUserGroupProvider.class);

    private static final String PROP_USERNAME = "username";
    private static final String PROP_GROUPNAME = "groupname";

    private final AwsCognitoCacheManager awsCognitoCacheManager;
    private final AwsCognitoClientService awsCognitoClientService;
    private AwsCognitoConfiguration awsCognitoConfiguration;

    public AwsCognitoUserGroupProvider(AwsCognitoCacheManager awsCognitoCacheManager, AwsCognitoClientService awsCognitoClientService) {
        this.awsCognitoCacheManager = awsCognitoCacheManager;
        this.awsCognitoClientService = awsCognitoClientService;
    }

    public void setAwsCognitoConfiguration(AwsCognitoConfiguration awsCognitoConfiguration) {
        this.awsCognitoConfiguration = awsCognitoConfiguration;
    }

    @Override
    protected String getSiteKey() {
        if (awsCognitoConfiguration == null) {
            return null;
        }
        return awsCognitoConfiguration.getTargetSite();
    }

    @Override
    public JahiaUser getUser(String userId) throws UserNotFoundException {
        if (!isAvailable()) {
            throw new UserNotFoundException();
        }
        return awsCognitoCacheManager.getOrRefreshUser(getKey(), getSiteKey(), userId,
                        () -> awsCognitoClientService.getUser(awsCognitoConfiguration, AwsCognitoConstants.SSO_LOGIN, userId))
                .orElseThrow(() -> new UserNotFoundException("User '" + userId + "' not found.")).getJahiaUser();
    }

    @Override
    public JahiaGroup getGroup(String groupname) throws GroupNotFoundException {
        if (!isAvailable()) {
            throw new GroupNotFoundException();
        }
        if (JahiaGroupManagerService.PROTECTED_GROUPS.contains(groupname) || JahiaGroupManagerService.POWERFUL_GROUPS.contains(groupname)) {
            logger.warn("Group {} is protected", groupname);
            return null;
        }
        return awsCognitoCacheManager.getOrRefreshGroup(getKey(), getSiteKey(), groupname, () -> awsCognitoClientService.getGroup(awsCognitoConfiguration, groupname))
                .orElseThrow(() -> new GroupNotFoundException("Group '" + groupname + "' not found.")).getJahiaGroup();
    }

    @Override
    public List<Member> getGroupMembers(String groupname) {
        if (!isAvailable()) {
            throw new JahiaRuntimeException("Service not available");
        }
        if (JahiaGroupManagerService.PROTECTED_GROUPS.contains(groupname) || JahiaGroupManagerService.POWERFUL_GROUPS.contains(groupname)) {
            logger.warn("Group {} is protected", groupname);
            return null;
        }
        // List of members in the groupname
        Optional<AwsCognitoGroup> group = awsCognitoCacheManager.getGroup(getKey(), getSiteKey(), groupname);
        if (!group.isPresent()) {
            logger.warn("Unable to get group members {}", groupname);
            return Collections.emptyList();
        }
        if (CollectionUtils.isNotEmpty(group.get().getMembers())) {
            logger.debug("Group member {} are in cache", groupname);
            return group.get().getMembers().stream().map(member -> new Member(member, Member.MemberType.USER))
                    .collect(Collectors.toList());
        }

        List<Member> members = new ArrayList<>();
        awsCognitoClientService.getGroupMembers(awsCognitoConfiguration, groupname).orElse(Collections.emptyList())
                .forEach(user -> members.add(new Member(user.getUsername(), Member.MemberType.USER)));
        awsCognitoCacheManager.getOrRefreshGroup(getKey(), getSiteKey(), groupname, () -> awsCognitoClientService.getGroup(awsCognitoConfiguration, groupname))
                .ifPresent(g -> g.setMembers(members.stream().map(Member::getName).collect(Collectors.toList())));
        return Collections.unmodifiableList(members);
    }

    @Override
    public List<String> getMembership(Member member) {
        if (!isAvailable()) {
            throw new JahiaRuntimeException("Service not available");
        }
        if (member.getType() == Member.MemberType.GROUP) {
            return Collections.emptyList();
        }

        // List of groups this principal belongs to
        String userId = member.getName();
        Optional<AwsCognitoUser> user = awsCognitoCacheManager.getOrRefreshUser(getKey(), getSiteKey(), userId,
                () -> awsCognitoClientService.getUser(awsCognitoConfiguration, AwsCognitoConstants.SSO_LOGIN, userId));
        if (!user.isPresent()) {
            logger.warn("Unable to get membership for user {}", userId);
            return Collections.emptyList();
        }
        if (CollectionUtils.isNotEmpty(user.get().getGroups())) {
            logger.debug("User groups {} are in cache", userId);
            return user.get().getGroups();
        }

        List<String> groups = new ArrayList<>();
        awsCognitoClientService.getMembership(awsCognitoConfiguration, (String) user.get().getAttributes().get(AwsCognitoConstants.AWS_USERNAME)).orElse(Collections.emptyList())
                .forEach(group -> groups.add(group.getName()));
        awsCognitoCacheManager.getOrRefreshUser(getKey(), getSiteKey(), userId, () -> awsCognitoClientService.getUser(awsCognitoConfiguration, AwsCognitoConstants.SSO_LOGIN, userId))
                .ifPresent(u -> u.setGroups(groups));
        return Collections.unmodifiableList(groups);
    }

    @Override
    public List<String> searchUsers(Properties searchCriteria, long offset, long limit) {
        if (!isAvailable()) {
            throw new JahiaRuntimeException("Service not available");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Search users: {}", searchCriteria);
        }

        // search one user in the cache by username
        if (searchCriteria.size() == 1 && (searchCriteria.containsKey(PROP_USERNAME) || searchCriteria.containsKey("*"))) {
            String userId = StringUtils.defaultString(searchCriteria.getProperty(PROP_USERNAME), searchCriteria.getProperty("*")).replace("*", "");
            return awsCognitoCacheManager.getOrRefreshUser(getKey(), getSiteKey(), userId,
                            () -> awsCognitoClientService.getUser(awsCognitoConfiguration, AwsCognitoConstants.SSO_LOGIN, userId))
                    .map(awsCognitoUser -> Collections.singletonList(awsCognitoUser.getUsername()))
                    .orElse(Collections.emptyList());
        }

        // search one user in the cache by email
        if (searchCriteria.containsKey(AwsCognitoConstants.CUSTOM_PROPERTY_EMAIL)) {
            String email = searchCriteria.getProperty(AwsCognitoConstants.CUSTOM_PROPERTY_EMAIL).replace("*", "");
            return awsCognitoCacheManager.getOrRefreshUser(getKey(), getSiteKey(), email,
                            () -> awsCognitoClientService.getUser(awsCognitoConfiguration, AwsCognitoConstants.CUSTOM_PROPERTY_EMAIL, email))
                    .map(awsCognitoUser -> Collections.singletonList(awsCognitoUser.getUsername()))
                    .orElse(Collections.emptyList());
        }

        logger.warn("Search users is disabled");
        return Collections.emptyList();
    }

    @Override
    public List<String> searchGroups(Properties searchCriteria, long offset, long limit) {
        if (!isAvailable()) {
            throw new JahiaRuntimeException("Service not available");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Search groups: {}", searchCriteria);
        }

        // search one group in the cache
        if (searchCriteria.containsKey(PROP_GROUPNAME) && searchCriteria.size() == 1 && !searchCriteria.getProperty(PROP_GROUPNAME).contains("*")) {
            String groupId = searchCriteria.getProperty(PROP_GROUPNAME);
            if (JahiaGroupManagerService.PROTECTED_GROUPS.contains(groupId) || JahiaGroupManagerService.POWERFUL_GROUPS.contains(groupId)) {
                logger.warn("Group {} is protected", groupId);
                return Collections.emptyList();
            }
            return awsCognitoCacheManager.getOrRefreshGroup(getKey(), getSiteKey(), groupId, () -> awsCognitoClientService.getGroup(awsCognitoConfiguration, groupId))
                    .map(awsCognitoGroup -> Collections.singletonList(awsCognitoGroup.getName()))
                    .orElse(Collections.emptyList());
        }

        Optional<List<AwsCognitoGroup>> awsCognitoGroups = awsCognitoCacheManager.getGroups(getKey(), getSiteKey(), (int) offset, (int) limit, () ->
                awsCognitoClientService.getGroups(awsCognitoConfiguration));

        String filter;
        if (searchCriteria.containsKey("*")) {
            filter = searchCriteria.getProperty("*").replace("*", "");
        } else if (searchCriteria.containsKey(PROP_GROUPNAME)) {
            filter = searchCriteria.getProperty(PROP_GROUPNAME).replace("*", "");
        } else {
            filter = null;
            logger.warn("Unable to search groups multiple attributes ; return all groups");
        }

        List<String> groupIds = new ArrayList<>();
        List<AwsCognitoGroup> groups = awsCognitoGroups.orElse(Collections.emptyList())
                .stream()
                .filter(group -> filter == null || StringUtils.containsIgnoreCase(group.getName(), filter))
                .collect(Collectors.toList());
        if (!groups.isEmpty() && limit > 0) {
            groups = groups.subList((int) offset, Math.min(groups.size(), (int) (offset + limit)));
        }
        groups.forEach(group -> {
            groupIds.add(group.getName());
            awsCognitoCacheManager.cacheGroup(getKey(), getSiteKey(), group);
        });
        return Collections.unmodifiableList(groupIds);
    }

    @Override
    public boolean verifyPassword(String userName, String userPassword) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean supportsGroups() {
        return isAvailable();
    }

    @Override
    public boolean isAvailable() {
        return awsCognitoClientService != null && awsCognitoConfiguration != null;
    }
}
