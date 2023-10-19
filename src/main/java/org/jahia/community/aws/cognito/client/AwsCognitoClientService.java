package org.jahia.community.aws.cognito.client;

import com.auth0.jwt.JWT;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.util.CollectionUtils;
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
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GetGroupResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListGroupsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListGroupsResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersInGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersInGroupResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersResponse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
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

    private static final String HMAC_SHA256 = "HmacSHA256";

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
                .filter("username=\"" + username + "\"")
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
            logger.warn("Unable to get user: {}", username);
            if (logger.isDebugEnabled()) {
                logger.debug("", e);
            }
            return Optional.empty();
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
            if (logger.isDebugEnabled()) {
                logger.debug(response.toString());
            }
            if (!response.hasUsers() || CollectionUtils.isEmpty(response.users())) {
                return Optional.empty();
            }
            return Optional.of(response.users().stream().map(AwsCognitoUser::new).collect(Collectors.toList()));
        } catch (Exception e) {
            logger.warn("Unable to get users");
            if (logger.isDebugEnabled()) {
                logger.debug("", e);
            }
            return Optional.empty();
        } finally {
            lock.unlock();
        }
    }

    public Optional<List<AwsCognitoUser>> searchUsers(AwsCognitoConfiguration awsCognitoConfiguration, String search, long offset, long limit) {
        return getUsers(awsCognitoConfiguration, offset, limit)
                .map(awsCognitoUsers -> awsCognitoUsers.stream()
                        .filter(user -> StringUtils.contains(user.getSub(), search) ||
                                StringUtils.contains(user.getUsername(), search) ||
                                StringUtils.containsIgnoreCase(user.getFirstname(), search) ||
                                StringUtils.containsIgnoreCase(user.getLastname(), search) ||
                                StringUtils.containsIgnoreCase(user.getEmail(), search))
                        .collect(Collectors.toList()));
    }

    public Optional<List<AwsCognitoUser>> searchUsersByFirstname(AwsCognitoConfiguration awsCognitoConfiguration, String firstname, long offset, long limit) {
        return getUsers(awsCognitoConfiguration, offset, limit)
                .map(awsCognitoUsers -> awsCognitoUsers.stream()
                        .filter(user -> StringUtils.containsIgnoreCase(user.getFirstname(), firstname))
                        .collect(Collectors.toList()));
    }

    public Optional<List<AwsCognitoUser>> searchUsersByLastname(AwsCognitoConfiguration awsCognitoConfiguration, String lastname, long offset, long limit) {
        return getUsers(awsCognitoConfiguration, offset, limit)
                .map(awsCognitoUsers -> awsCognitoUsers.stream()
                        .filter(user -> StringUtils.containsIgnoreCase(user.getLastname(), lastname))
                        .collect(Collectors.toList()));
    }

    public Optional<List<AwsCognitoUser>> searchUsersByEmail(AwsCognitoConfiguration awsCognitoConfiguration, String email, long offset, long limit) {
        return getUsers(awsCognitoConfiguration, offset, limit)
                .map(awsCognitoUsers -> awsCognitoUsers.stream()
                        .filter(user -> StringUtils.containsIgnoreCase(user.getEmail(), email))
                        .collect(Collectors.toList()));
    }

    public Optional<List<AwsCognitoGroup>> searchGroups(AwsCognitoConfiguration awsCognitoConfiguration, String search, long offset, long limit) {
        return getGroups(awsCognitoConfiguration, offset, limit)
                .map(awsCognitoGroups -> awsCognitoGroups.stream()
                        .filter(group -> StringUtils.containsIgnoreCase(group.getName(), search))
                        .collect(Collectors.toList()));
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

    public Optional<List<AwsCognitoGroup>> getGroups(AwsCognitoConfiguration awsCognitoConfiguration, long offset, long limit) {
        lock.lock();
        ListGroupsRequest request = ListGroupsRequest.builder()
                .userPoolId(awsCognitoConfiguration.getUserPoolId())
                .build();
        try (CognitoIdentityProviderClient cognitoIdentityProviderClient = getCognitoIdentityProviderClient(awsCognitoConfiguration)) {
            ListGroupsResponse response = cognitoIdentityProviderClient.listGroups(request);
            if (logger.isDebugEnabled()) {
                logger.debug(response.toString());
            }
            if (!response.hasGroups() || CollectionUtils.isEmpty(response.groups())) {
                return Optional.empty();
            }
            return Optional.of(response.groups().stream().map(AwsCognitoGroup::new).collect(Collectors.toList()));
        } catch (Exception e) {
            logger.warn("Unable to get groups");
            if (logger.isDebugEnabled()) {
                logger.debug("", e);
            }
            return Optional.empty();
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
            if (logger.isDebugEnabled()) {
                logger.debug(response.toString());
            }
            if (!response.hasUsers() || CollectionUtils.isEmpty(response.users())) {
                return Optional.empty();
            }
            return Optional.of(response.users().stream().map(AwsCognitoUser::new).collect(Collectors.toList()));
        } catch (Exception e) {
            logger.warn("Unable to get group {} members", groupName);
            if (logger.isDebugEnabled()) {
                logger.debug("", e);
            }
            return Optional.empty();
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
            if (logger.isDebugEnabled()) {
                logger.debug(response.toString());
            }
            if (!response.hasGroups() || CollectionUtils.isEmpty(response.groups())) {
                return Optional.empty();
            }
            return Optional.of(response.groups().stream().map(AwsCognitoGroup::new).collect(Collectors.toList()));
        } catch (Exception e) {
            logger.warn("Unable to get membership for user: {}", username);
            if (logger.isDebugEnabled()) {
                logger.debug("", e);
            }
            return Optional.empty();
        } finally {
            lock.unlock();
        }
    }

    public Optional<String> login(String region, String clientId, String clientSecret, String username, String password) {
        lock.lock();
        Map<String, String> authParameters = new HashMap<>();
        authParameters.put("USERNAME", username);
        authParameters.put("PASSWORD", password);
        authParameters.put("SECRET_HASH", calculateSecretHash(clientId, clientSecret, username));
        try (CognitoIdentityProviderClient cognitoIdentityProviderClient = CognitoIdentityProviderClient.builder()
                .region(Region.of(region)).build()) {
            InitiateAuthRequest request = InitiateAuthRequest.builder()
                    .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                    .authParameters(authParameters)
                    .clientId(clientId)
                    .build();
            InitiateAuthResponse response = cognitoIdentityProviderClient.initiateAuth(request);
            if (logger.isDebugEnabled()) {
                logger.debug(response.toString());
            }
            return Optional.ofNullable(response.authenticationResult()).map(result -> JWT.decode(result.idToken()).getSubject());
        } finally {
            lock.unlock();
        }
    }

    private static String calculateSecretHash(String clientId, String clientSecret, String userName) {
        SecretKeySpec signingKey = new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(signingKey);
            mac.update(userName.getBytes(StandardCharsets.UTF_8));
            byte[] rawHmac = mac.doFinal(clientId.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error while calculating secret hash", e);
        }
    }
}
