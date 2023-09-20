package org.jahia.community.aws.cognito.provider;

import org.apache.commons.collections.CollectionUtils;
import org.jahia.community.aws.cognito.api.AwsCognitoConfiguration;
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
        return awsCognitoCacheManager.getOrRefreshUser(getKey(), getSiteKey(), userId, () -> awsCognitoClientService.getUser(awsCognitoConfiguration, userId))
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
            return Collections.emptyList();
        }
        if (CollectionUtils.isNotEmpty(group.get().getMembers())) {
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
            return Collections.emptyList();
        }
        if (CollectionUtils.isNotEmpty(user.get().getGroups())) {
            return user.get().getGroups();
        }

        List<String> groups = new ArrayList<>();
        awsCognitoClientService.getMembership(awsCognitoConfiguration, userId).orElse(Collections.emptyList())
                .forEach(group -> groups.add(group.getName()));
        awsCognitoCacheManager.getOrRefreshUser(getKey(), getSiteKey(), userId, () -> awsCognitoClientService.getUser(awsCognitoConfiguration, userId))
                .ifPresent(u -> u.setGroups(groups));
        return Collections.unmodifiableList(groups);
    }

    @Override
    public List<String> searchUsers(Properties searchCriteria, long offset, long limit) {
        if (!isAvailable()) {
            throw new JahiaRuntimeException("Service not available");
        }

        // search one user in the cache
        if (searchCriteria.containsKey(PROP_USERNAME) && searchCriteria.size() == 1) {
            String userId = searchCriteria.getProperty(PROP_USERNAME);
            return awsCognitoCacheManager.getOrRefreshUser(getKey(), getSiteKey(), userId, () -> awsCognitoClientService.getUser(awsCognitoConfiguration, userId))
                    .map(awsCognitoUser -> Collections.singletonList(awsCognitoUser.getUsername()))
                    .orElse(Collections.emptyList());
        }

        Optional<List<AwsCognitoUser>> awsCognitoUsers;
        if (searchCriteria.containsKey("*")) {
            awsCognitoUsers = awsCognitoClientService.searchUsers(awsCognitoConfiguration, searchCriteria.getProperty("*").replace("*", ""), offset, limit);
        } else {
            awsCognitoUsers = awsCognitoClientService.getUsers(awsCognitoConfiguration, offset, limit);
        }
        List<String> userIds = new ArrayList<>();
        awsCognitoUsers.orElse(Collections.emptyList())
                .forEach(user -> {
                    userIds.add(user.getUsername());
                    awsCognitoCacheManager.cacheUser(getKey(), getSiteKey(), user);
                });
        return Collections.unmodifiableList(userIds);
    }

    @Override
    public List<String> searchGroups(Properties searchCriteria, long offset, long limit) {
        if (!isAvailable()) {
            throw new JahiaRuntimeException("Service not available");
        }

        // search one group in the cache
        if (searchCriteria.containsKey(PROP_GROUPNAME) && searchCriteria.size() == 1) {
            String groupId = searchCriteria.getProperty(PROP_GROUPNAME);
            if (JahiaGroupManagerService.PROTECTED_GROUPS.contains(groupId) || JahiaGroupManagerService.POWERFUL_GROUPS.contains(groupId)) {
                logger.warn("Group {} is protected", groupId);
                return Collections.emptyList();
            }
            return awsCognitoCacheManager.getOrRefreshGroup(getKey(), getSiteKey(), groupId, () -> awsCognitoClientService.getGroup(awsCognitoConfiguration, groupId))
                    .map(awsCognitoGroup -> Collections.singletonList(awsCognitoGroup.getName()))
                    .orElse(Collections.emptyList());
        }

        List<String> groupIds = new ArrayList<>();
        awsCognitoClientService.getGroups(awsCognitoConfiguration, offset, limit).orElse(Collections.emptyList())
                .forEach(group -> {
                    groupIds.add(group.getName());
                    awsCognitoCacheManager.cacheGroup(getKey(), getSiteKey(), group);
                });
        return Collections.unmodifiableList(groupIds);
    }

    @Override
    public boolean verifyPassword(String userName, String userPassword) {
        return false;
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
