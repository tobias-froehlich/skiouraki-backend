package org.example;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Map;

public class User {
    private String id;
    private String version;
    private String name;
    private String password;

    private static final String VALID_CHARACTERS      = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZäöußÄÖÜẞΑαΒβΓγΔδΕεΖζΗηΘθΙιΚκΛλΜμΝνΞξΟοΠπΡρΣσςΤτΥυΦφΧχΨψΩω";
    private static final String NORMALIZED_CHARACTERS = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzaousaousaabbggddeezzhhttiikkllmmnnccoopprrsssttyyffxxppoo";
    private static final int MAX_NAME_LENGTH = 16;

    public User(
            @JsonProperty("id") String id,
            @JsonProperty("version") String version,
            @JsonProperty("name") String name,
            @JsonProperty("password") String password) {
        this.id = id;
        this.version = version;
        this.name = name;
        this.password = password;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static boolean isNameValid(String name) {
        if (name.length() > MAX_NAME_LENGTH) {
            return false;
        }
        for(int i = 0; i < name.length(); i++) {
            if (!VALID_CHARACTERS.contains(name.substring(i, i+1))) {
                return false;
            }
        }
        return true;
    }

    public static String getNormalizedName(String name) {
        int length = name.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char c = name.charAt(i);
            int index = VALID_CHARACTERS.indexOf(c);
            sb.append(NORMALIZED_CHARACTERS.charAt(index));
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof User)) {
            return false;
        }
        User other = (User) obj;
        if (other.getId() != null && (this.getId() == null || !this.getId().equals(other.getId()))) {
            return false;
        }
        if  (other.getVersion() != null && (this.getVersion() == null || !this.getVersion().equals(other.getVersion()))) {
            return false;
        }
        if (other.getName() != null && (this.getName() == null || !this.getName().equals(other.getName()))) {
            return false;
        }
        if (other.getPassword() != null && (this.getPassword() == null || !this.getPassword().equals(other.getPassword()))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", version='" + version + '\'' +
                ", name='" + name + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
