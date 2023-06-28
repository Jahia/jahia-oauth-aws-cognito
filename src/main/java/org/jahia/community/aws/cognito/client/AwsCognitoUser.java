package org.jahia.community.aws.cognito.client;

import org.jahia.services.usermanager.JahiaUserImpl;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;

import java.io.Serializable;
import java.util.List;

public class AwsCognitoUser implements Serializable {
    private static final long serialVersionUID = -200885001913981199L;

    private final String username;
    private JahiaUserImpl jahiaUser;
    private List<String> groups;

    public AwsCognitoUser(UserType awsUser) {
        username = awsUser.username();
    }

    public String getUsername() {
        return username;
    }

    public JahiaUserImpl getJahiaUser() {
        return jahiaUser;
    }

    public void setJahiaUser(JahiaUserImpl jahiaUser) {
        this.jahiaUser = jahiaUser;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }
}
