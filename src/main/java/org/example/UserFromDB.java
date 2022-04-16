package org.example;

public class UserFromDB {
    private String id;
    private String version;
    private String name;
    private String normalizedName;
    private String hashedPassword;
    private String salt;

    public UserFromDB(
            String id,
            String version,
            String name,
            String normalizedName,
            String hashedPassword,
            String salt) {
        this.id = id;
        this.version = version;
        this.name = name;
        this.normalizedName = normalizedName;
        this.hashedPassword = hashedPassword;
        this.salt = salt;
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
    public String getNormalizedName() {
        return normalizedName;
    }

    public void setNormalizedName(String normalizedName) {
        this.normalizedName = normalizedName;
    }
    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }



    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }
}
