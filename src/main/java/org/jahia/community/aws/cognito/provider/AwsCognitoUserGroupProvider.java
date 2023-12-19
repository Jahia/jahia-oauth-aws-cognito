package org.jahia.community.aws.cognito.provider;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.community.aws.cognito.connector.AwsCognitoConfiguration;
import org.jahia.community.aws.cognito.connector.AwsCognitoConstants;
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
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                        () -> awsCognitoClientService.getUser(awsCognitoConfiguration, PROP_USERNAME, userId))
                .orElseThrow(() -> new UserNotFoundException("User '" + userId + "' not found.")).getJahiaUser();
    }

    @Override
    public JahiaGroup getGroup(String groupId) throws GroupNotFoundException {
        if (!isAvailable()) {
            throw new GroupNotFoundException();
        }
        if (JahiaGroupManagerService.PROTECTED_GROUPS.contains(groupId) || JahiaGroupManagerService.POWERFUL_GROUPS.contains(groupId)) {
            logger.warn("Group {} is protected", groupId);
            return null;
        }
        return awsCognitoCacheManager.getOrRefreshGroup(getKey(), getSiteKey(), groupId, () -> awsCognitoClientService.getGroup(awsCognitoConfiguration, groupId))
                .orElseThrow(() -> new GroupNotFoundException("Group '" + groupId + "' not found.")).getJahiaGroup();
    }

    @Override
    public List<Member> getGroupMembers(String groupId) {
        if (!isAvailable()) {
            throw new JahiaRuntimeException("Service not available");
        }
        if (JahiaGroupManagerService.PROTECTED_GROUPS.contains(groupId) || JahiaGroupManagerService.POWERFUL_GROUPS.contains(groupId)) {
            logger.warn("Group {} is protected", groupId);
            return null;
        }
        // List of members in the groupId
        Optional<AwsCognitoGroup> group = awsCognitoCacheManager.getGroup(getKey(), getSiteKey(), groupId);
        if (!group.isPresent()) {
            logger.warn("Unable to get group members {}", groupId);
            return Collections.emptyList();
        }
        if (CollectionUtils.isNotEmpty(group.get().getMembers())) {
            logger.debug("Group member {} are in cache", groupId);
            return group.get().getMembers().stream().map(member -> new Member(member, Member.MemberType.USER))
                    .collect(Collectors.toList());
        }

        List<Member> members = new ArrayList<>();
        awsCognitoClientService.getGroupMembers(awsCognitoConfiguration, groupId).orElse(Collections.emptyList())
                .forEach(user -> members.add(new Member(user.getUsername(), Member.MemberType.USER)));
        awsCognitoCacheManager.getOrRefreshGroup(getKey(), getSiteKey(), groupId, () -> awsCognitoClientService.getGroup(awsCognitoConfiguration, groupId))
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
        Optional<AwsCognitoUser> user = awsCognitoCacheManager.getUser(getKey(), getSiteKey(), userId);
        if (!user.isPresent()) {
            logger.warn("Unable to user groups {}", userId);
            return Collections.emptyList();
        }
        if (CollectionUtils.isNotEmpty(user.get().getGroups())) {
            logger.debug("User groups {} are in cache", userId);
            return user.get().getGroups();
        }

        List<String> groups = new ArrayList<>();
        awsCognitoClientService.getMembership(awsCognitoConfiguration, userId).orElse(Collections.emptyList())
                .forEach(group -> groups.add(group.getName()));
        awsCognitoCacheManager.getOrRefreshUser(getKey(), getSiteKey(), userId, () -> awsCognitoClientService.getUser(awsCognitoConfiguration, PROP_USERNAME, userId))
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
        if (searchCriteria.containsKey(PROP_USERNAME) && searchCriteria.size() == 1 && !searchCriteria.getProperty(PROP_USERNAME).contains("*")) {
            String userId = searchCriteria.getProperty(PROP_USERNAME);
            return awsCognitoCacheManager.getOrRefreshUser(getKey(), getSiteKey(), userId,
                            () -> awsCognitoClientService.getUser(awsCognitoConfiguration, PROP_USERNAME, userId))
                    .map(awsCognitoUser -> Collections.singletonList(awsCognitoUser.getUsername()))
                    .orElse(Collections.emptyList());
        }

        // search one user in the cache by email
        if (searchCriteria.containsKey(AwsCognitoConstants.CUSTOM_PROPERTY_EMAIL)) {
            String email = searchCriteria.getProperty(AwsCognitoConstants.CUSTOM_PROPERTY_EMAIL);
            return awsCognitoCacheManager.getOrRefreshUser(getKey(), getSiteKey(), email,
                            () -> awsCognitoClientService.getUser(awsCognitoConfiguration, AwsCognitoConstants.CUSTOM_PROPERTY_EMAIL, email.replace("*", "")))
                    .map(awsCognitoUser -> Collections.singletonList(awsCognitoUser.getUsername()))
                    .orElse(Collections.emptyList());
        }

        int iOffset = (int) offset;
        int iLimit = (int) limit;
        Optional<List<AwsCognitoUser>> awsCognitoUsers;
        if (searchCriteria.isEmpty()) {
            // cache user with offset and limit
            awsCognitoUsers = awsCognitoCacheManager.getUsers(getKey(), getSiteKey(), iOffset, iLimit,
                    () -> awsCognitoClientService.getUsers(awsCognitoConfiguration, iOffset, iLimit));
        } else {
            // cache all users
            awsCognitoUsers = awsCognitoCacheManager.getUsers(getKey(), getSiteKey(), iOffset, -1,
                            () -> awsCognitoClientService.getUsers(awsCognitoConfiguration, iOffset, -1))
                    .map(l -> {
                        Stream<AwsCognitoUser> users = l.stream();
                        if (searchCriteria.containsKey("*")) {
                            String search = searchCriteria.getProperty("*").replace("*", "");
                            users = users.filter(user -> StringUtils.contains(user.getUsername(), search) ||
                                    user.getAttributes().values().stream()
                                            .anyMatch(attribute -> StringUtils.containsIgnoreCase(attribute.toString(), search)));
                        } else {
                            for (Map.Entry<Object, Object> entry : searchCriteria.entrySet()) {
                                users = users.filter(user -> user.getAttributes().containsKey(entry.getKey().toString()) &&
                                        StringUtils.containsIgnoreCase(user.getAttributes().get(entry.getKey().toString()).toString(),
                                                entry.getValue().toString().replace("*", "")));
                            }
                        }
                        return users.collect(Collectors.toList());
                    }).map(list -> limit == -1 ? list : list.subList(iOffset, Math.min(list.size(), iOffset + iLimit)));
        }
        return awsCognitoUsers.orElse(Collections.emptyList()).stream().map(AwsCognitoUser::getUsername).collect(Collectors.toList());
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

        Optional<List<AwsCognitoGroup>> awsCognitoGroups;
        if (searchCriteria.containsKey("*")) {
            awsCognitoGroups = awsCognitoClientService.getGroups(awsCognitoConfiguration, searchCriteria.getProperty("*").replace("*", ""), (int) offset, (int) limit);
        } else if (searchCriteria.isEmpty()) {
            awsCognitoGroups = awsCognitoClientService.getGroups(awsCognitoConfiguration, null, (int) offset, (int) limit);
        } else if (searchCriteria.containsKey(PROP_GROUPNAME)) {
            awsCognitoGroups = awsCognitoClientService.getGroups(awsCognitoConfiguration, searchCriteria.getProperty(PROP_GROUPNAME).replace("*", ""), (int) offset, (int) limit);
        } else {
            logger.warn("Unable to search groups multiple attributes");
            awsCognitoGroups = Optional.empty();
        }
        List<String> groupIds = new ArrayList<>();
        awsCognitoGroups.orElse(Collections.emptyList())
                .forEach(group -> {
                    groupIds.add(group.getName());
                    awsCognitoCacheManager.cacheGroup(getKey(), getSiteKey(), group);
                });
        return Collections.unmodifiableList(groupIds);
    }

    @Override
    public boolean verifyPassword(String userName, String userPassword) {
        return awsCognitoClientService.getUser(awsCognitoConfiguration, PROP_USERNAME, userName)
                .filter(awsCognitoUser -> awsCognitoClientService.login(awsCognitoConfiguration, awsCognitoUser.getUsername(), userPassword)
                        .isPresent()).isPresent();
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
