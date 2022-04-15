package org.example;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

public class UserDAO {
    private final DSLContext dslContext;
    private MessageDigest digest;



    public UserDAO(DSLContext dslContext) {
        this.dslContext = dslContext;
        try {
            this.digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public List<User> getAllAppUsers() {
        List<UserFromDB> usersFromDB = dslContext.selectFrom("user_account").fetch(new UserMapper());
        List<User> users = new ArrayList<>();
        for(UserFromDB userFromDB : usersFromDB) {
            users.add(new User(userFromDB.getId(), userFromDB.getVersion(), userFromDB.getName(), null));
        }
        return users;
    }

    public User getUser(String id, String auth) throws ApplicationException {
        List<UserFromDB> usersFromDB = dslContext.selectFrom("user_account")
                .where(field("id").eq(id))
                .fetch(new UserMapper());
        if (usersFromDB.size() == 0) {
            throw new ApplicationException("User with id = " + id + " not found.");
        }
        UserFromDB userFromDB = usersFromDB.get(0);
        verifyAuthentication(auth, userFromDB);
        return new User(userFromDB.getId(), userFromDB.getVersion(), userFromDB.getName(), null);
    }

    public String getUserIdByName(String name) throws ApplicationException {
        String normalizedName = User.getNormalizedName(name);
        List<UserFromDB> usersFromDB = dslContext.selectFrom("user_account")
                .where(field("normalized_name").eq(normalizedName))
                .fetch(new UserMapper());
        if (usersFromDB.size() == 0) {
            throw new ApplicationException("User not found.");
        }
        UserFromDB userFromDB = usersFromDB.get(0);
        return userFromDB.getId();
    }

    public User addUser(User user) throws ApplicationException {
        System.out.println(user);
        if (!User.isNameValid(user.getName())) {
            throw new ApplicationException("Invalid user name.");
        }
        String newId = UUID.randomUUID().toString();
        String newVersion = UUID.randomUUID().toString();
        String normalizedName = User.getNormalizedName(user.getName());

        HashedPasswordAndSalt hashedPasswordAndSalt = new HashedPasswordAndSalt(digest, user.getPassword());
        String hashedPassword = hashedPasswordAndSalt.hashedPassword;
        String salt = hashedPasswordAndSalt.salt;
        //System.out.println(Base64.getEncoder().encode(hashedPassword);
        //System.out.println(Base64.getEncoder().encode(salt));
        System.out.println(hashedPassword);
        try {
            dslContext.insertInto(table("user_account"))
                    .columns(field("id"), field("version"), field("name"), field("normalized_name"), field("hashed_password"), field("salt"))
                    .values(newId, newVersion, user.getName(), normalizedName, hashedPassword, salt)
                    .execute();
        } catch (DataAccessException e) {
            e.printStackTrace();
            throw new ApplicationException("Cannot insert user. User with name " + user.getName() + " already exists.");
        }
        return getUser(newId, makeAuth(newId, user.getPassword()));
    }

    public User updateUser(User user, String auth) throws ApplicationException {
        User authenticatedUser = getUser(user.getId(), auth);
        if (!User.isNameValid(user.getName())) {
            throw new ApplicationException("Invalid user name.");
        }
        String newVersion = UUID.randomUUID().toString();
        HashedPasswordAndSalt hashedPasswordAndSalt = new HashedPasswordAndSalt(digest, user.getPassword());
        String hashedPassword = hashedPasswordAndSalt.hashedPassword;
        String salt = hashedPasswordAndSalt.salt;
        int count = dslContext.update(table("user_account"))
                .set(field("id"), user.getId())
                .set(field("version"), newVersion)
                .set(field("name"), user.getName())
                .set(field("hashed_password"), hashedPassword)
                .set(field("salt"), salt)
                .where(field("id").eq(user.getId()))
                .and(field("version").eq(user.getVersion()))
                .execute();
        if (count == 0) {
            throw new ApplicationException("The user does not exist or the version is outdated.");
        }
        return getUser(user.getId(), makeAuth(user.getId(), user.getPassword()));
    }

    public User deleteUser(User user, String auth) {
        User authenticatedUser = getUser(user.getId(), auth);
        dslContext.deleteFrom(table("user_account"))
                .where(field("id").eq(user.getId()))
                .execute();
        return authenticatedUser;
    }

    private String makeAuth(String id, String password) {
//        HashedPasswordAndSalt hashedPasswordAndSalt = new HashedPasswordAndSalt(digest, password, salt);
//        byte[] hashedPassword = hashedPasswordAndSalt.hashedPassword;
        return "Basic " + new String(Base64.getEncoder().encode((id + ":" + password).getBytes(StandardCharsets.UTF_8)));
    }

    private void verifyAuthentication(String auth, UserFromDB userFromDB) {
        if (auth == null) {
            throw new ApplicationException("Authentication header is missing.");
        }
        String[] words = auth.split(" ");
        String credentials = new String(Base64.getDecoder().decode(words[1]));
        String[] credentialWords = credentials.split(":", 2);
        if (words.length != 2) {
            throw new ApplicationException("Wrong authentication header.");
        }
        if (!"Basic".equals(words[0])) {
            throw new ApplicationException("Wrong authentication method.");
        }

        System.out.println("sent password = " + credentialWords[1]);
        HashedPasswordAndSalt hashedPasswordAndSalt = new HashedPasswordAndSalt(digest, credentialWords[1], userFromDB.getSalt());
        String hashedPassword = hashedPasswordAndSalt.hashedPassword;
        System.out.println("sent hashed password = " + hashedPassword);
        System.out.println("db hashed password = " + userFromDB.getHashedPassword());
        if (credentialWords.length != 2 || !credentialWords[0].equals(userFromDB.getId()) || !hashedPassword.equals(userFromDB.getHashedPassword())) {
            throw new ApplicationException("Wrong credentials.");
        }
    }

    private static class HashedPasswordAndSalt {
        public String hashedPassword;
        public String salt;

        public HashedPasswordAndSalt(MessageDigest digest, String password) {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            try {
                stream.write(salt);
                stream.write(password.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.hashedPassword = new String(Base64.getEncoder().encode(digest.digest(stream.toByteArray())), StandardCharsets.UTF_8);
            this.salt = new String(Base64.getEncoder().encode(salt), StandardCharsets.UTF_8);
        }

        public HashedPasswordAndSalt(MessageDigest digest, String password, String salt) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            try {
                stream.write(Base64.getDecoder().decode(salt));
                stream.write(password.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.hashedPassword = new String(Base64.getEncoder().encode(digest.digest(stream.toByteArray())), StandardCharsets.UTF_8);
            this.salt = salt;
        }
    }

    public static class UserMapper implements RecordMapper<Record, UserFromDB> {
        @Override
        public UserFromDB map(Record record) {
            return new UserFromDB(
                    record.getValue("id", String.class),
                    record.getValue("version", String.class),
                    record.getValue("name", String.class),
                    record.getValue("hashed_password", String.class),
                    record.getValue("salt", String.class)
            );
        }
    }

}
