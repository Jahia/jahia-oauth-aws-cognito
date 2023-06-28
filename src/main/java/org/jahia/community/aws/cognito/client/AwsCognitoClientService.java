package org.jahia.community.aws.cognito.client;

import org.apache.shiro.util.CollectionUtils;
import org.jahia.community.aws.cognito.provider.AwsCognitoConfiguration;
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
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListGroupsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListGroupsResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersInGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersInGroupResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse;

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

    public Optional<AwsCognitoUser> getUser(AwsCognitoConfiguration awsCognitoConfiguration, String username) {
        lock.lock();
        ListUsersRequest request = ListUsersRequest.builder()
                .userPoolId(awsCognitoConfiguration.getUserPoolId())
                .filter("username=" + username)
                .build();
        try (CognitoIdentityProviderClient cognitoIdentityProviderClient = getCognitoIdentityProviderClient(awsCognitoConfiguration)) {
            ListUsersResponse response = cognitoIdentityProviderClient.listUsers(request);
            if (!response.hasUsers() || CollectionUtils.isEmpty(response.users())) {
                return Optional.empty();
            }
            return Optional.of(new AwsCognitoUser(response.users().get(0)));
        } finally {
            lock.unlock();
        }
    }

    public Optional<List<AwsCognitoUser>> getUsers(AwsCognitoConfiguration awsCognitoConfiguration, long offset, long limit) {
        lock.lock();
        ListUsersRequest request = ListUsersRequest.builder()
                .userPoolId(awsCognitoConfiguration.getUserPoolId())
                .build();
        try (CognitoIdentityProviderClient cognitoIdentityProviderClient = getCognitoIdentityProviderClient(awsCognitoConfiguration)) {
            ListUsersResponse response = cognitoIdentityProviderClient.listUsers(request);
            if (!response.hasUsers() || CollectionUtils.isEmpty(response.users())) {
                return Optional.empty();
            }
            return Optional.of(response.users().stream().map(AwsCognitoUser::new).collect(Collectors.toList()));
        } finally {
            lock.unlock();
        }
    }

    public Optional<List<AwsCognitoUser>> searchUsers(AwsCognitoConfiguration awsCognitoConfiguration, String search, long offset, long limit) {
        logger.warn("Method searchUsers not implemented");
        return getUsers(awsCognitoConfiguration, offset, limit);
    }

    public Optional<AwsCognitoGroup> getGroup(AwsCognitoConfiguration awsCognitoConfiguration, String groupName) {
        lock.lock();
        GetGroupRequest request = GetGroupRequest.builder()
                .userPoolId(awsCognitoConfiguration.getUserPoolId())
                .groupName(groupName)
                .build();
        try (CognitoIdentityProviderClient cognitoIdentityProviderClient = getCognitoIdentityProviderClient(awsCognitoConfiguration)) {
            GetGroupResponse response = cognitoIdentityProviderClient.getGroup(request);
            if (response.group() == null) {
                return Optional.empty();
            }
            return Optional.of(new AwsCognitoGroup(response.group()));
        } finally {
            lock.unlock();
        }
    }

    public Optional<List<AwsCognitoGroup>> getGroups(AwsCognitoConfiguration awsCognitoConfiguration, long offset, long limit) {
        lock.lock();
        ListGroupsRequest request = ListGroupsRequest.builder()
                .userPoolId(awsCognitoConfiguration.getUserPoolId())
                .build();
        try (CognitoIdentityProviderClient cognitoIdentityProviderClient = getCognitoIdentityProviderClient(awsCognitoConfiguration)) {
            ListGroupsResponse response = cognitoIdentityProviderClient.listGroups(request);
            if (!response.hasGroups() || CollectionUtils.isEmpty(response.groups())) {
                return Optional.empty();
            }
            return Optional.of(response.groups().stream().map(AwsCognitoGroup::new).collect(Collectors.toList()));
        } finally {
            lock.unlock();
        }
    }

    public Optional<List<AwsCognitoUser>> getGroupMembers(AwsCognitoConfiguration awsCognitoConfiguration, String groupName) {
        lock.lock();
        ListUsersInGroupRequest request = ListUsersInGroupRequest.builder()
                .userPoolId(awsCognitoConfiguration.getUserPoolId())
                .groupName(groupName)
                .build();
        try (CognitoIdentityProviderClient cognitoIdentityProviderClient = getCognitoIdentityProviderClient(awsCognitoConfiguration)) {
            ListUsersInGroupResponse response = cognitoIdentityProviderClient.listUsersInGroup(request);
            if (!response.hasUsers() || CollectionUtils.isEmpty(response.users())) {
                return Optional.empty();
            }
            return Optional.of(response.users().stream().map(AwsCognitoUser::new).collect(Collectors.toList()));
        } finally {
            lock.unlock();
        }
    }

    public Optional<List<AwsCognitoGroup>> getMembership(AwsCognitoConfiguration awsCognitoConfiguration, String username) {
        lock.lock();
        AdminListGroupsForUserRequest request = AdminListGroupsForUserRequest.builder()
                .userPoolId(awsCognitoConfiguration.getUserPoolId())
                .username(username)
                .build();
        try (CognitoIdentityProviderClient cognitoIdentityProviderClient = getCognitoIdentityProviderClient(awsCognitoConfiguration)) {
            AdminListGroupsForUserResponse response = cognitoIdentityProviderClient.adminListGroupsForUser(request);
            if (!response.hasGroups() || CollectionUtils.isEmpty(response.groups())) {
                return Optional.empty();
            }
            return Optional.of(response.groups().stream().map(AwsCognitoGroup::new).collect(Collectors.toList()));
        } finally {
            lock.unlock();
        }
    }
}
