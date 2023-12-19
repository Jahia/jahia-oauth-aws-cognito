package org.jahia.community.aws.cognito.client;

import org.jahia.community.aws.cognito.api.AwsCognitoConstants;
import org.jahia.services.usermanager.JahiaUserImpl;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;

import java.io.Serializable;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class AwsCognitoUser implements Serializable {
    private static final long serialVersionUID = -200885001913981199L;

    private final String username;
    private String email;
    private JahiaUserImpl jahiaUser;
    private List<String> groups;
    private final Properties attributes;

    public AwsCognitoUser(UserType awsUser) {
        username = awsUser.username();
        attributes = new Properties();
        attributes.putAll(awsUser.attributes().stream().collect(Collectors.toMap(AttributeType::name, AttributeType::value)));
        if (attributes.containsKey(AwsCognitoConstants.CUSTOM_PROPERTY_EMAIL)) {
            email = (String) attributes.get(AwsCognitoConstants.CUSTOM_PROPERTY_EMAIL);
            attributes.put(AwsCognitoConstants.USER_PROPERTY_EMAIL, email);
        }
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public Properties getAttributes() {
        return attributes;
    }

    public JahiaUserImpl getJahiaUser() {
        return jahiaUser;
    }

    public String cacheJahiaUser(String providerKey, String siteKey) {
        jahiaUser = new JahiaUserImpl(username, username, attributes, false, providerKey, siteKey);
        return username;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }
}
