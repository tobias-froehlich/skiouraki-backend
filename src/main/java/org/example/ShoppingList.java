package org.example;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ShoppingList {
    private String id;
    private String version;
    private String name;
    private String owner;

    private static final int MIN_NAME_LENGTH = 1;
    private static final int MAX_NAME_LENGTH = 32;

    public ShoppingList(@JsonProperty("id") String id,
                        @JsonProperty("version") String version,
                        @JsonProperty("name") String name,
                        @JsonProperty("owner") String owner) {
        this.id = id;
        this.version = version;
        this.name = name;
        this.owner = owner;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public static boolean isNameValid(String name) {
        return (name.length() >= MIN_NAME_LENGTH) && (name.length() <= MAX_NAME_LENGTH);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ShoppingList)) {
            return false;
        }
        ShoppingList other = (ShoppingList) obj;
        if (other.getId() != null && (this.getId() == null || !this.getId().equals(other.getId()))) {
            return false;
        }
        if  (other.getVersion() != null && (this.getVersion() == null || !this.getVersion().equals(other.getVersion()))) {
            return false;
        }
        if (other.getName() != null && (this.getName() == null || !this.getName().equals(other.getName()))) {
            return false;
        }
        if (other.getOwner() != null && (this.getOwner() == null || !this.getOwner().equals(other.getOwner()))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ShoppingList{" +
                "id='" + id + '\'' +
                ", version='" + version + '\'' +
                ", name='" + name + '\'' +
                ", owner='" + owner + '\'' +
                '}';
    }
}
