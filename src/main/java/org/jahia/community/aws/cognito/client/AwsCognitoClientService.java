package org.jahia.community.aws.cognito.client;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.community.aws.cognito.connector.AwsCognitoConfiguration;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminListGroupsForUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminListGroupsForUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetGroupResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GroupType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListGroupsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListGroupsResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersInGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersInGroupResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                .filter(filterKey + "^=\"" + filterValue + "\"")
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

    private void getUsersRecursively(AwsCognitoConfiguration awsCognitoConfiguration, List<UserType> users, int offset, int limit, String paginationToken) {
        lock.lock();
        ListUsersRequest.Builder request = ListUsersRequest.builder()
                .userPoolId(awsCognitoConfiguration.getUserPoolId());
        if (paginationToken != null) {
            request.paginationToken(paginationToken);
        }
        try (CognitoIdentityProviderClient cognitoIdentityProviderClient = getCognitoIdentityProviderClient(awsCognitoConfiguration)) {
            ListUsersResponse response = cognitoIdentityProviderClient.listUsers(request.build());
            if (logger.isDebugEnabled()) {
                logger.debug(response.toString());
            }
            if (response.hasUsers() && !CollectionUtils.isEmpty(response.users())) {
                users.addAll(response.users());
                paginationToken = response.paginationToken();
                if (paginationToken != null && (limit == -1 || users.size() <= (offset + limit))) {
                    getUsersRecursively(awsCognitoConfiguration, users, offset, limit, paginationToken);
                }
            }
        } catch (Exception e) {
            logger.warn("Unable to get users");
            if (logger.isDebugEnabled()) {
                logger.debug("", e);
            }
        } finally {
            lock.unlock();
        }
    }

    public Optional<List<AwsCognitoUser>> getUsers(AwsCognitoConfiguration awsCognitoConfiguration, int offset, int limit) {
        List<UserType> users = new ArrayList<>();
        getUsersRecursively(awsCognitoConfiguration, users, offset, limit, null);
        if (users.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(users.stream().map(AwsCognitoUser::new).collect(Collectors.toList())).map(list -> limit == -1 ? list : list.subList(offset, Math.min(list.size(), offset + limit)));
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

    private void getGroupsRecursively(AwsCognitoConfiguration awsCognitoConfiguration, List<GroupType> groups, int offset, int limit, String nextToken) {
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
                if (nextToken != null && (limit == -1 || groups.size() <= (offset + limit))) {
                    getGroupsRecursively(awsCognitoConfiguration, groups, offset, limit, nextToken);
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

    public Optional<List<AwsCognitoGroup>> getGroups(AwsCognitoConfiguration awsCognitoConfiguration, String filter, int offset, int limit) {
        List<GroupType> groups = new ArrayList<>();
        getGroupsRecursively(awsCognitoConfiguration, groups, offset, -1, null);
        if (groups.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(groups.stream()
                .filter(group -> filter == null || StringUtils.containsIgnoreCase(group.groupName(), filter))
                .map(AwsCognitoGroup::new)
                .collect(Collectors.toList())).map(list -> limit == -1 ? list : list.subList(offset, Math.min(list.size(), offset + limit)));
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

    public Optional<String> login(AwsCognitoConfiguration awsCognitoConfiguration, String username, String password) {
        lock.lock();
        Map<String, String> authParameters = new HashMap<>();
        authParameters.put("USERNAME", username);
        authParameters.put("PASSWORD", password);
        try (CognitoIdentityProviderClient cognitoIdentityProviderClient = getCognitoIdentityProviderClient(awsCognitoConfiguration)) {
            InitiateAuthRequest request = InitiateAuthRequest.builder()
                    .clientId(awsCognitoConfiguration.getClientId())
                    .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                    .authParameters(authParameters)
                    .build();
            InitiateAuthResponse response = cognitoIdentityProviderClient.initiateAuth(request);
            if (logger.isDebugEnabled()) {
                logger.debug(response.toString());
            }
            return Optional.ofNullable(response.authenticationResult()).map(result -> {
                String[] chunks = result.idToken().split("\\.");
                try {
                    JSONObject payload = new JSONObject(new String(Base64.getDecoder().decode(chunks[1].getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
                    return payload.getString("sub");
                } catch (JSONException e) {
                    logger.error("", e);
                    return null;
                }
            });
        } catch (Exception e) {
            logger.warn("Unable to log in user: {}", username);
            if (logger.isDebugEnabled()) {
                logger.debug("", e);
            }
            return Optional.empty();
        } finally {
            lock.unlock();
        }
    }
}
