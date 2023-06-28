package org.jahia.community.aws.cognito.client;

import org.jahia.services.usermanager.JahiaGroupImpl;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GroupType;

import java.io.Serializable;
import java.util.List;

public class AwsCognitoGroup implements Serializable {
    private static final long serialVersionUID = 4349942162870561387L;

    private final String name;
    private JahiaGroupImpl jahiaGroup;
    private List<String> members;

    public AwsCognitoGroup(GroupType group) {
        name = group.groupName();
    }

    public String getName() {
        return name;
    }

    public JahiaGroupImpl getJahiaGroup() {
        return jahiaGroup;
    }

    public void setJahiaGroup(JahiaGroupImpl jahiaGroup) {
        this.jahiaGroup = jahiaGroup;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }
}
