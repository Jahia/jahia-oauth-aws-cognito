package org.jahia.community.aws.cognito.client;

import org.jahia.services.usermanager.JahiaUserImpl;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;

import java.io.Serializable;
import java.util.List;
import java.util.Properties;

public class AwsCognitoUser implements Serializable {
    private static final long serialVersionUID = -200885001913981199L;

    private final String username;
    private String givenName;
    private String familyName;
    private String email;
    private JahiaUserImpl jahiaUser;
    private List<String> groups;

    public AwsCognitoUser(UserType awsUser) {
        username = awsUser.username();
        awsUser.attributes().forEach(attributeType -> {
            if ("given_name".equals(attributeType.name())) {
                givenName = attributeType.value();
            } else if ("family_name".equals(attributeType.name())) {
                familyName = attributeType.value();
            } else if ("email".equals(attributeType.name())) {
                email = attributeType.value();
            }
        });
    }

    public String getUsername() {
        return username;
    }

    public JahiaUserImpl getJahiaUser() {
        return jahiaUser;
    }

    public String cacheJahiaUser(String providerKey, String siteKey) {
        Properties properties = new Properties();
        properties.put("j:firstName", givenName);
        properties.put("j:lastName", familyName);
        properties.put("j:email", email);
        jahiaUser = new JahiaUserImpl(username, username, properties, false, providerKey, siteKey);
        return username;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }
}
