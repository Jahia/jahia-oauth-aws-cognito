package org.jahia.community.aws.cognito.provider.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jahia.services.usermanager.JahiaGroupImpl;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class AwsCognitoGroup implements Serializable {
    private static final long serialVersionUID = 4349942162870561387L;

    private String id;
    private String name;
    private String description;
    private JahiaGroupImpl jahiaGroup;
    private List<String> members;

    @JsonProperty("profile")
    private void setProfile(Map<String, String> profile) {
        name = profile.get("name");
        description = profile.get("description");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
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
