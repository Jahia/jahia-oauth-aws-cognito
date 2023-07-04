package org.jahia.community.aws.cognito.client;

import org.jahia.api.Constants;
import org.jahia.services.usermanager.JahiaGroupImpl;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GroupType;

import java.io.Serializable;
import java.util.List;
import java.util.Properties;

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

    public String cacheGroup(String siteKey) {
        Properties properties = new Properties();
        properties.put(Constants.JCR_TITLE, name);
        jahiaGroup = new JahiaGroupImpl(name, name, siteKey, properties);
        return name;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }
}
