package org.example;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class ShoppingListItem {
    private String id;
    private String version;
    private String name;
    private String createdBy;
    private String modifiedBy;
    private String boughtBy;
    private String stateChangedBy;

    private static final int MIN_NAME_LENGTH = 1;
    private static final int MAX_NAME_LENGTH = 32;

    public ShoppingListItem(@JsonProperty("id") String id,
                            @JsonProperty("version") String version,
                            @JsonProperty("name") String name,
                            @JsonProperty("createdBy") String createdBy,
                            @JsonProperty("modifiedBy") String modifiedBy,
                            @JsonProperty("boughtBy") String boughtBy,
                            @JsonProperty("stateChangedBy") String stateChangedBy) {
        this.id = id;
        this.version = version;
        this.name = name;
        this.createdBy = createdBy;
        this.modifiedBy = modifiedBy;
        this.boughtBy = boughtBy;
        this.stateChangedBy = stateChangedBy;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public String getBoughtBy() {
        return boughtBy;
    }

    public void setBoughtBy(String boughtBy) {
        this.boughtBy = boughtBy;
    }

    public String getStateChangedBy() {
        return stateChangedBy;
    }

    public void setStateChangedBy(String stateChangedBy) {
        this.stateChangedBy = stateChangedBy;
    }

    public static boolean isNameValid(String name) {
        return (name.length() >= MIN_NAME_LENGTH) && (name.length() <= MAX_NAME_LENGTH);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShoppingListItem that = (ShoppingListItem) o;
        return Objects.equals(id, that.id) && Objects.equals(version, that.version) && Objects.equals(name, that.name) && Objects.equals(createdBy, that.createdBy) && Objects.equals(modifiedBy, that.modifiedBy) && Objects.equals(boughtBy, that.boughtBy) && Objects.equals(stateChangedBy, that.stateChangedBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version, name, createdBy, modifiedBy, boughtBy, stateChangedBy);
    }

    @Override
    public String toString() {
        return "ShoppingListItem{" +
                "id='" + id + '\'' +
                ", version='" + version + '\'' +
                ", name='" + name + '\'' +
                ", createdBy='" + createdBy + '\'' +
                ", modifiedBy='" + modifiedBy + '\'' +
                ", boughtBy='" + boughtBy + '\'' +
                ", stateChangedBy='" + stateChangedBy + '\'' +
                '}';
    }
}
