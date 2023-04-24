package org.jahia.community.aws.cognito.provider.client;

import org.apache.shiro.util.CollectionUtils;
import org.jahia.community.aws.cognito.provider.AwsCognitoConfiguration;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
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
                .region(Region.US_EAST_1)
                .credentialsProvider(ProfileCredentialsProvider.create())
                .build();
    }

    public Optional<AwsCognitoUser> getUser(AwsCognitoConfiguration awsCognitoConfiguration, String userId) {
        lock.lock();
        ListUsersRequest request = ListUsersRequest.builder()
                .filter("username=" + userId)
                .build();
        try (CognitoIdentityProviderClient cognitoIdentityProviderClient = getCognitoIdentityProviderClient(awsCognitoConfiguration)) {
            ListUsersResponse response = cognitoIdentityProviderClient.listUsers(request);
            if (!response.hasUsers() || CollectionUtils.isEmpty(response.users())) {
                return Optional.empty();
            }
            return Optional.of(new AwsCognitoUser(response.users().get(0)));
        }
    }

    public Optional<List<AwsCognitoUser>> getUsers(AwsCognitoConfiguration awsCognitoConfiguration, long offset, long limit) {
        lock.lock();
        ListUsersRequest request = ListUsersRequest.builder()
                .limit((int) limit)
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
        logger.error("Method searchUsers not implemented");
        return Optional.empty();
    }

    public Optional<AwsCognitoGroup> getGroup(AwsCognitoConfiguration awsCognitoConfiguration, String groupId) {
        logger.error("Method AwsCognitoConfiguration not implemented");
        return Optional.empty();
    }

    public Optional<List<AwsCognitoGroup>> getGroups(AwsCognitoConfiguration awsCognitoConfiguration, long offset, long limit) {
        logger.error("Method getGroups not implemented");
        return Optional.empty();
    }

    public Optional<List<AwsCognitoUser>> getGroupMembers(AwsCognitoConfiguration awsCognitoConfiguration, String groupId) {
        logger.error("Method getGroupMembers not implemented");
        return Optional.empty();
    }

    public Optional<List<AwsCognitoGroup>> getMembership(AwsCognitoConfiguration awsCognitoConfiguration, String userId) {
        logger.error("Method getMembership not implemented");
        return Optional.empty();
    }
}
