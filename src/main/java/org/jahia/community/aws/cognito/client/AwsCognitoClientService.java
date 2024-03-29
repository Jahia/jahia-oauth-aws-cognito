package org.jahia.community.aws.cognito.client;

import org.apache.commons.collections.CollectionUtils;
import org.jahia.community.aws.cognito.api.AwsCognitoConfiguration;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminListGroupsForUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminListGroupsForUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetGroupResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GroupType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListGroupsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListGroupsResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersInGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersInGroupResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Component(service = AwsCognitoClientService.class)
public class AwsCognitoClientService {
    private static final Logger logger = LoggerFactory.getLogger(AwsCognitoClientService.class);

    private final ReentrantLock lock;

    public AwsCognitoClientService() {
        lock = new ReentrantLock();
    }

    private static CognitoIdentityProviderClient getCognitoIdentityProviderClient(AwsCognitoConfiguration awsCognitoConfiguration) {
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(awsCognitoConfiguration.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(awsCognitoConfiguration.getAccessKeyId(), awsCognitoConfiguration.getSecretAccessKey())))
                .build();
    }

    public Optional<AwsCognitoUser> getUser(AwsCognitoConfiguration awsCognitoConfiguration, String filterKey, String filterValue) {
        lock.lock();
        ListUsersRequest request = ListUsersRequest.builder()
                .userPoolId(awsCognitoConfiguration.getUserPoolId())
                .filter(filterKey + "=\"" + filterValue + "\"")
                .build();
        try (CognitoIdentityProviderClient cognitoIdentityProviderClient = getCognitoIdentityProviderClient(awsCognitoConfiguration)) {
            ListUsersResponse response = cognitoIdentityProviderClient.listUsers(request);
            if (logger.isDebugEnabled()) {
                logger.debug(response.toString());
            }
            if (!response.hasUsers() || CollectionUtils.isEmpty(response.users())) {
                return Optional.empty();
            }
            return Optional.of(new AwsCognitoUser(response.users().get(0)));
        } catch (Exception e) {
            logger.warn("Unable to get user searching {}:{}", filterKey, filterValue);
            if (logger.isDebugEnabled()) {
                logger.debug("", e);
            }
            return Optional.empty();
        } finally {
            lock.unlock();
        }
    }

    public Optional<List<AwsCognitoUser>> getUsers(AwsCognitoConfiguration awsCognitoConfiguration, int limit) {
        lock.lock();
        List<UserType> users = new ArrayList<>();
        ListUsersRequest.Builder request = ListUsersRequest.builder()
                .userPoolId(awsCognitoConfiguration.getUserPoolId())
                .limit(limit);
        try (CognitoIdentityProviderClient cognitoIdentityProviderClient = getCognitoIdentityProviderClient(awsCognitoConfiguration)) {
            ListUsersResponse response = cognitoIdentityProviderClient.listUsers(request.build());
            if (logger.isDebugEnabled()) {
                logger.debug(response.toString());
            }
            if (response.hasUsers() && !CollectionUtils.isEmpty(response.users())) {
                users.addAll(response.users());
            }
        } catch (Exception e) {
            logger.warn("Unable to get users");
            if (logger.isDebugEnabled()) {
                logger.debug("", e);
            }
        } finally {
            lock.unlock();
        }
        if (users.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(users.stream().map(AwsCognitoUser::new).collect(Collectors.toList()));
    }

    private void getGroupMembersRecursively(AwsCognitoConfiguration awsCognitoConfiguration, String groupName, List<UserType> users, String nextToken) {
        lock.lock();
        ListUsersInGroupRequest.Builder request = ListUsersInGroupRequest.builder()
                .userPoolId(awsCognitoConfiguration.getUserPoolId())
                .groupName(groupName);
        if (nextToken != null) {
            request.nextToken(nextToken);
        }
        try (CognitoIdentityProviderClient cognitoIdentityProviderClient = getCognitoIdentityProviderClient(awsCognitoConfiguration)) {
            ListUsersInGroupResponse response = cognitoIdentityProviderClient.listUsersInGroup(request.build());
            if (logger.isDebugEnabled()) {
                logger.debug(response.toString());
            }
            if (response.hasUsers() && !CollectionUtils.isEmpty(response.users())) {
                users.addAll(response.users());
                nextToken = response.nextToken();
                if (nextToken != null) {
                    getGroupMembersRecursively(awsCognitoConfiguration, groupName, users, nextToken);
                }
            }
        } catch (Exception e) {
            logger.warn("Unable to get group {} members", groupName);
            if (logger.isDebugEnabled()) {
                logger.debug("", e);
            }
        } finally {
            lock.unlock();
        }
    }

    public Optional<List<AwsCognitoUser>> getGroupMembers(AwsCognitoConfiguration awsCognitoConfiguration, String groupName) {
        List<UserType> users = new ArrayList<>();
        getGroupMembersRecursively(awsCognitoConfiguration, groupName, users, null);
        if (users.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(users.stream().map(AwsCognitoUser::new).collect(Collectors.toList()));
    }

    public Optional<AwsCognitoGroup> getGroup(AwsCognitoConfiguration awsCognitoConfiguration, String groupName) {
        lock.lock();
        GetGroupRequest request = GetGroupRequest.builder()
                .userPoolId(awsCognitoConfiguration.getUserPoolId())
                .groupName(groupName)
                .build();
        try (CognitoIdentityProviderClient cognitoIdentityProviderClient = getCognitoIdentityProviderClient(awsCognitoConfiguration)) {
            GetGroupResponse response = cognitoIdentityProviderClient.getGroup(request);
            if (logger.isDebugEnabled()) {
                logger.debug(response.toString());
            }
            if (response.group() == null) {
                return Optional.empty();
            }
            return Optional.of(new AwsCognitoGroup(response.group()));
        } catch (Exception e) {
            logger.warn("Unable to get group: {}", groupName);
            if (logger.isDebugEnabled()) {
                logger.debug("", e);
            }
            return Optional.empty();
        } finally {
            lock.unlock();
        }
    }

    private void getGroupsRecursively(AwsCognitoConfiguration awsCognitoConfiguration, List<GroupType> groups, String nextToken) {
        lock.lock();
        ListGroupsRequest.Builder requestBuilder = ListGroupsRequest.builder().userPoolId(awsCognitoConfiguration.getUserPoolId());
        if (nextToken != null) {
            requestBuilder.nextToken(nextToken);
        }
        try (CognitoIdentityProviderClient cognitoIdentityProviderClient = getCognitoIdentityProviderClient(awsCognitoConfiguration)) {
            ListGroupsResponse response = cognitoIdentityProviderClient.listGroups(requestBuilder.build());
            if (logger.isDebugEnabled()) {
                logger.debug(response.toString());
            }
            if (response.hasGroups() && !CollectionUtils.isEmpty(response.groups())) {
                groups.addAll(response.groups());
                nextToken = response.nextToken();
                if (nextToken != null) {
                    getGroupsRecursively(awsCognitoConfiguration, groups, nextToken);
                }
            }
        } catch (Exception e) {
            logger.warn("Unable to get groups");
            if (logger.isDebugEnabled()) {
                logger.debug("", e);
            }
        } finally {
            lock.unlock();
        }
    }

    public Optional<List<AwsCognitoGroup>> getGroups(AwsCognitoConfiguration awsCognitoConfiguration) {
        List<GroupType> groups = new ArrayList<>();
        getGroupsRecursively(awsCognitoConfiguration, groups, null);
        if (groups.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(groups.stream().map(AwsCognitoGroup::new).collect(Collectors.toList()));
    }

    private void getMembershipRecursively(AwsCognitoConfiguration awsCognitoConfiguration, String username, List<GroupType> groups, String nextToken) {
        lock.lock();
        AdminListGroupsForUserRequest.Builder request = AdminListGroupsForUserRequest.builder()
                .userPoolId(awsCognitoConfiguration.getUserPoolId())
                .username(username);
        if (nextToken != null) {
            request.nextToken(nextToken);
        }
        try (CognitoIdentityProviderClient cognitoIdentityProviderClient = getCognitoIdentityProviderClient(awsCognitoConfiguration)) {
            AdminListGroupsForUserResponse response = cognitoIdentityProviderClient.adminListGroupsForUser(request.build());
            if (logger.isDebugEnabled()) {
                logger.debug(response.toString());
            }
            if (response.hasGroups() && !CollectionUtils.isEmpty(response.groups())) {
                groups.addAll(response.groups());
                nextToken = response.nextToken();
                if (nextToken != null) {
                    getMembershipRecursively(awsCognitoConfiguration, username, groups, nextToken);
                }
            }
        } catch (Exception e) {
            logger.warn("Unable to get membership for user: {}", username);
            if (logger.isDebugEnabled()) {
                logger.debug("", e);
            }
        } finally {
            lock.unlock();
        }
    }

    public Optional<List<AwsCognitoGroup>> getMembership(AwsCognitoConfiguration awsCognitoConfiguration, String username) {
        List<GroupType> groups = new ArrayList<>();
        getMembershipRecursively(awsCognitoConfiguration, username, groups, null);
        if (groups.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(groups.stream().map(AwsCognitoGroup::new).collect(Collectors.toList()));
    }
}
