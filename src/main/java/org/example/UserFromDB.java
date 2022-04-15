package org.example;

public class UserFromDB {
    private String id;
    private String version;
    private String name;
    private byte[] hashedPassword;
    private byte[] salt;

    public UserFromDB(
            String id,
            String version,
            String name,
            byte[] hashedPassword,
            byte[] salt) {
        this.id = id;
        this.version = version;
        this.name = name;
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

    public byte[] getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(byte[] hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public byte[] getSalt() {
        return salt;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt;
    }
}
