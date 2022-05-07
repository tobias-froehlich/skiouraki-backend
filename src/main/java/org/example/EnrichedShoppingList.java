package org.example;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashSet;
import java.util.List;

public class EnrichedShoppingList extends ShoppingList {

    private List<User> members;
    private List<User> invitedUsers;
    private List<ShoppingListItem> items;

    public EnrichedShoppingList(@JsonProperty("id") String id,
                                @JsonProperty("version") String version,
                                @JsonProperty("name") String name,
                                @JsonProperty("owner") String owner,
                                @JsonProperty("members") List<User> members,
                                @JsonProperty("invitedUsers") List<User> invitedUsers,
                                @JsonProperty("items") List<ShoppingListItem> items) {
        super(id, version, name, owner);
        this.members = members;
        this.invitedUsers = invitedUsers;
        this.items = items;
    }

    public List<User> getMembers() {
        return members;
    }

    public void setMembers(List<User> members) {
        this.members = members;
    }

    public List<User> getInvitedUsers() {
        return invitedUsers;
    }

    public void setInvitedUsers(List<User> invitedUsers) {
        this.invitedUsers = invitedUsers;
    }

    public List<ShoppingListItem> getItems() {
        return items;
    }

    public void setItems(List<ShoppingListItem> items) {
        this.items = items;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        EnrichedShoppingList other = (EnrichedShoppingList)obj;
        if (other.getMembers() != null && (this.getMembers() == null || (this.getMembers().size() != other.getMembers().size()) || !(this.getMembers().containsAll(other.getMembers()))) ) {
            return false;
        }
        if (other.getInvitedUsers() != null && (this.getInvitedUsers() == null || (this.getInvitedUsers().size() != other.getInvitedUsers().size()) || !(this.getInvitedUsers().containsAll(other.getInvitedUsers()))) ) {
            return false;
        }
        if (other.getItems() != null && (this.getItems() == null || (this.getItems().size() != other.getItems().size()) || !(this.getItems().containsAll(other.getItems()))) ) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "EnrichedShoppingList{" +
                "members=" + members +
                ", invitedUsers=" + invitedUsers +
                "} " + super.toString();
    }
}
