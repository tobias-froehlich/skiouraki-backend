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

    public User getUser(String id) throws ApplicationException {
        List<UserFromDB> usersFromDB = dslContext.selectFrom("user_account")
                .where(field("id").eq(id))
                .fetch(new UserMapper());
        if (usersFromDB.size() == 0) {
            throw new ApplicationException("User not found.");
        }
        UserFromDB userFromDB = usersFromDB.get(0);
        return new User(userFromDB.getId(), userFromDB.getVersion(), userFromDB.getName(), null);
    }

    public User authenticate(String id, String auth) throws ApplicationException {
        List<UserFromDB> usersFromDB = dslContext.selectFrom("user_account")
                .where(field("id").eq(id))
                .fetch(new UserMapper());
        if (usersFromDB.size() == 0) {
            throw new ApplicationException("User not found.");
        }
        UserFromDB userFromDB = usersFromDB.get(0);
        verifyAuthentication(auth, userFromDB);
        return new User(userFromDB.getId(), userFromDB.getVersion(), userFromDB.getName(), null);
    }

    public User authenticate(String auth) throws ApplicationException {
        final IdAndPassword idAndPassword = new IdAndPassword(auth);
        return authenticate(idAndPassword.id, auth);
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
        if (!User.isPasswordValid(user.getPassword())) {
            throw new ApplicationException("Invalid password.");
        }
        String newId = UUID.randomUUID().toString();
        String newVersion = UUID.randomUUID().toString();
        String normalizedName = User.getNormalizedName(user.getName());

        HashedPasswordAndSalt hashedPasswordAndSalt = new HashedPasswordAndSalt(digest, user.getPassword());
        String hashedPassword = hashedPasswordAndSalt.hashedPassword;
        String salt = hashedPasswordAndSalt.salt;
        System.out.println(hashedPassword);
        try {
            dslContext.insertInto(table("user_account"))
                    .columns(field("id"), field("version"), field("name"), field("normalized_name"), field("hashed_password"), field("salt"))
                    .values(newId, newVersion, user.getName(), normalizedName, hashedPassword, salt)
                    .execute();
        } catch (DataAccessException e) {
            throw new ApplicationException("The user name already exists.");
        }
        return getUser(newId);
    }

    public User updateUser(User user, String auth) throws ApplicationException {
        User authenticatedUser = authenticate(user.getId(), auth);
        if (!User.isNameValid(user.getName())) {
            throw new ApplicationException("Invalid user name.");
        }
        if (!User.isPasswordValid(user.getPassword())) {
            throw new ApplicationException("Invalid password.");
        }
        String normalizedName = User.getNormalizedName(user.getName());
        String newVersion = UUID.randomUUID().toString();
        HashedPasswordAndSalt hashedPasswordAndSalt = new HashedPasswordAndSalt(digest, user.getPassword());
        String hashedPassword = hashedPasswordAndSalt.hashedPassword;
        String salt = hashedPasswordAndSalt.salt;
        int count = 0;
        try {
            count = dslContext.update(table("user_account"))
                    .set(field("id"), user.getId())
                    .set(field("version"), newVersion)
                    .set(field("name"), user.getName())
                    .set(field("normalized_name"), normalizedName)
                    .set(field("hashed_password"), hashedPassword)
                    .set(field("salt"), salt)
                    .where(field("id").eq(user.getId()))
                    .and(field("version").eq(user.getVersion()))
                    .execute();
        } catch (DataAccessException e) {
            throw new ApplicationException("The new user name already exists.");
        }
        if (count == 0) {
            throw new ApplicationException("The version is outdated.");
        }
        return getUser(user.getId());
    }

    public User deleteUser(String id, String auth) {
        User authenticatedUser = authenticate(id, auth);
        dslContext.transaction(configuration -> {
            DSLContext ctx = DSL.using(configuration);
            List<String> shoppingListIds = ctx.select(field("id"))
                    .from("shopping_list")
                    .where(field("owner").eq(id))
                    .fetch(record -> record.getValue("id", String.class));
//            List<String> itemIds = ctx.select(field("item_id"))
//                    .from("shopping_list_shopping_list_item")
//                    .where(field("shopping_list_id").in(shoppingListIds))
//                    .fetch(record -> record.getValue("id", String.class));
//            ctx.deleteFrom(table("shopping_list_shopping_list_item"))
//                    .where(field("shopping_list_id").in(shoppingListIds))
//                    .execute();
            ctx.deleteFrom(table("shopping_list_item"))
                    .where(field("shopping_list_id").in(shoppingListIds))
                    .execute();
            ctx.deleteFrom(table("shopping_list_authorization"))
                    .where(field("shopping_list_id").in(
                            shoppingListIds
                    ))
                    .execute();
            DSL.using(configuration).deleteFrom(table("shopping_list_authorization"))
                    .where(field("user_id").eq(id))
                    .execute();
            DSL.using(configuration).deleteFrom(table("shopping_list"))
                    .where(field("owner").eq(id))
                    .execute();
            DSL.using(configuration).deleteFrom(table("user_account"))
                    .where(field("id").eq(id))
                    .execute();
        });
        return authenticatedUser;
    }

    private String makeAuth(String id, String password) {
        return "Basic " + new String(
                Base64.getEncoder().encode((id + ":" + password).getBytes(StandardCharsets.UTF_8)));
    }

    private void verifyAuthentication(String auth, UserFromDB userFromDB) {
        final IdAndPassword idAndPassword = new IdAndPassword(auth);
        final String id = idAndPassword.id;
        final String password = idAndPassword.password;

        HashedPasswordAndSalt hashedPasswordAndSalt = new HashedPasswordAndSalt(digest, password, userFromDB.getSalt());
        String hashedPassword = hashedPasswordAndSalt.hashedPassword;
        if (!id.equals(userFromDB.getId()) || !hashedPassword.equals(userFromDB.getHashedPassword())) {
            throw new ApplicationException("Wrong credentials.");
        }
    }

    private static class IdAndPassword {
        public String id;
        public String password;

        public IdAndPassword(String auth) {
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
            if (credentialWords.length != 2) {
                throw new ApplicationException("Wrong credentials.");
            }

            this.id = credentialWords[0];
            this.password = credentialWords[1];

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
                    record.getValue("normalized_name", String.class),
                    record.getValue("hashed_password", String.class),
                    record.getValue("salt", String.class)
            );
        }
    }

}
