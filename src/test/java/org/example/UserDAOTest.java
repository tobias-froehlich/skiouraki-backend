package org.example;

import org.checkerframework.checker.signature.qual.DotSeparatedIdentifiersOrPrimitiveType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserDAOTest extends TestWithDB {

    private UserDAO userDAO;

    @BeforeAll
    @Override
    public void beforeAll() {
        super.beforeAll();
        userDAO = new UserDAO(dslContext);
    }

    @BeforeEach
    @Override
    public void beforeEach() {
        super.beforeEach();
    }

    @Test
    public void testGetAllAppUsers() {
        dslContext.insertInto(table("user_account"))
                .set(field("id"), "test-id-1")
                .set(field("version"), "test-version-1")
                .set(field("name"), "John")
                .set(field("normalized_name"), "john")
                .set(field("hashed_password"), Base64.getEncoder().encode("johns-hashed-password".getBytes(StandardCharsets.UTF_8)))
                .set(field("salt"), Base64.getEncoder().encode("johns-salt".getBytes(StandardCharsets.UTF_8)))
                .execute();
        dslContext.insertInto(table("user_account"))
                .set(field("id"), "test-id-2")
                .set(field("version"), "test-version-2")
                .set(field("name"), "Joe")
                .set(field("normalized_name"), "joe")
                .set(field("hashed_password"), Base64.getEncoder().encode("joes-hashed-password".getBytes(StandardCharsets.UTF_8)))
                .set(field("salt"), Base64.getEncoder().encode("joes-salt".getBytes(StandardCharsets.UTF_8)))
                .execute();
        List<User> expected = List.of(
                new User("test-id-1", "test-version-1", "John", null),
                new User("test-id-2", "test-version-2", "Joe", null)
        );
        List<User> actual = userDAO.getAllAppUsers();
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);

    }

    @Test
    public void testGetUser() {
        String hashedPassword = hashPasswordWithSalt("johns-password", "abcd");
        System.out.println("before writen to db hashed password = " + hashedPassword);
        dslContext.insertInto(table("user_account"))
                .set(field("id"), "test-id")
                .set(field("version"), "test-version")
                .set(field("name"), "John")
                .set(field("hashed_password"), hashedPassword)
                .set(field("salt"), "abcd")
                .execute();
        User expected = new User("test-id", "test-version", "John", null);
        User actual = userDAO.getUser("test-id");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testAuthenticateUser() {
        String hashedPassword = hashPasswordWithSalt("johns-password", "abcd");
        System.out.println("before writen to db hashed password = " + hashedPassword);
        dslContext.insertInto(table("user_account"))
                .set(field("id"), "test-id")
                .set(field("version"), "test-version")
                .set(field("name"), "John")
                .set(field("hashed_password"), hashedPassword)
                .set(field("salt"), "abcd")
                .execute();
        User expected = new User("test-id", "test-version", "John", null);
        User actual = userDAO.authenticate("test-id", "Basic dGVzdC1pZDpqb2hucy1wYXNzd29yZA==");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testTryToAuthenticateUserWithWrongPassword() {
        String hashedPassword = hashPasswordWithSalt("johns-password", "abcd");
        System.out.println("before writen to db hashed password = " + hashedPassword);
        dslContext.insertInto(table("user_account"))
                .set(field("id"), "test-id")
                .set(field("version"), "test-version")
                .set(field("name"), "John")
                .set(field("hashed_password"), hashedPassword)
                .set(field("salt"), Base64.getEncoder().encode("abcd".getBytes(StandardCharsets.UTF_8)))
                .execute();
        assertThatThrownBy(() -> {
            userDAO.authenticate("test-id", makeAuth("test-id", "wrong-password"));
        }).isInstanceOf(ApplicationException.class).hasMessage("Wrong credentials.");

    }

    @Test
    public void testGetUserByName() {
        dslContext.insertInto(table("user_account"))
                .set(field("id"), "test-id")
                .set(field("version"), "test-version")
                .set(field("name"), "John")
                .set(field("normalized_name"), "john")
                .set(field("hashed_password"), Base64.getEncoder().encode("johns-hashed-password".getBytes(StandardCharsets.UTF_8)))
                .set(field("salt"), Base64.getEncoder().encode("johns-salt".getBytes(StandardCharsets.UTF_8)))
                .execute();
        dslContext.insertInto(table("user_account"))
                .set(field("id"), "test-id2")
                .set(field("version"), "test-version2")
                .set(field("name"), "Joe")
                .set(field("normalized_name"), "joe")
                .set(field("hashed_password"), Base64.getEncoder().encode("joes-hashed_password".getBytes(StandardCharsets.UTF_8)))
                .set(field("salt"), Base64.getEncoder().encode("joes-salt".getBytes(StandardCharsets.UTF_8)))
                .execute();
        String expected = "test-id";
        String actual = userDAO.getUserIdByName("John");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testAddUser() throws Exception {
        User user = new User(null, null, "John", "johns-password");
        User addedUser = null;
        addedUser = userDAO.addUser(user);

        String id = addedUser.getId();
        assertThat(addedUser.getName()).isEqualTo("John");
        assertThat(addedUser.getPassword()).isNull();
        List<UserFromDB> usersFromDB = dslContext.selectFrom("user_account")
                .where(field("id").eq(id)).fetch(new UserDAO.UserMapper());
        assertThat(usersFromDB).hasSize(1);
        assertThat(usersFromDB.get(0).getName()).isEqualTo("John");
        assertThatUserHasPassword(usersFromDB.get(0), "johns-password");
    }

    @Test
    public void testTryToAddUserTwice() {
        User user = new User(null, null, "John", "johns-password");
        User addedUser = userDAO.addUser(user);
        assertThatThrownBy(() -> {
            userDAO.addUser(user);
        }).isInstanceOf(ApplicationException.class).hasMessage("The user name already exists.");
    }

    @Test
    public void testTryToAddUserTwiceWithDifferentSpelling() {
        User user1 = new User(null, null, "John", "johns-password");
        User addedUser = userDAO.addUser(user1);
        User user2 = new User(null, null, "john", "johns-password");
        assertThatThrownBy(() -> {
            userDAO.addUser(user2);
        }).isInstanceOf(ApplicationException.class).hasMessage("The user name already exists.");
    }

    @Test
    public void testAddUserWithMaximumLengthName() {
        User user = new User(null, null, "abcdefghijklmnop", "johns-password");
        User addedUser = userDAO.addUser(user);
        String id = addedUser.getId();
        assertThat(addedUser.getName()).isEqualTo("abcdefghijklmnop");
        assertThat(addedUser.getPassword()).isNull();
        List<UserFromDB> usersFromDB = dslContext.selectFrom("user_account")
                .where(field("id").eq(id)).fetch(new UserDAO.UserMapper());
        assertThat(usersFromDB).hasSize(1);
        assertThatUserHasPassword(usersFromDB.get(0), "johns-password");
    }

    @Test
    public void testAddUserWithMinimumLengthName() {
        User user = new User(null, null, "abc", "johns-password");
        User addedUser = userDAO.addUser(user);
        String id = addedUser.getId();
        assertThat(addedUser.getName()).isEqualTo("abc");
        assertThat(addedUser.getPassword()).isNull();
        List<UserFromDB> usersFromDB = dslContext.selectFrom("user_account")
                .where(field("id").eq(id)).fetch(new UserDAO.UserMapper());
        assertThat(usersFromDB).hasSize(1);
        assertThatUserHasPassword(usersFromDB.get(0), "johns-password");
    }

    @Test
    public void testTryToAddUserWithTooLongUsername() {
        User user = new User(null, null, "abcdefghijklmnopq", "johns-password");
        assertThatThrownBy(() -> {
            userDAO.addUser(user);
        }).isInstanceOf(ApplicationException.class).hasMessage("Invalid user name.");
    }

    @Test
    public void testTryToAddUserWithTooShortUsername() {
        User user = new User(null, null, "ab", "johns-password");
        assertThatThrownBy(() -> {
            userDAO.addUser(user);
        }).isInstanceOf(ApplicationException.class).hasMessage("Invalid user name.");
    }

    @Test
    public void testTryToAddUserWithInvalidCharacters() {
        User user = new User(null, null, "abc-def", "johns-password");
        assertThatThrownBy(() -> {
            userDAO.addUser(user);
        }).isInstanceOf(ApplicationException.class).hasMessage("Invalid user name.");
    }

    @Test
    public void testAddUserWithColonInPassword() {
        User user = new User(null, null, "John", "johns:password");
        User addedUser = userDAO.addUser(user);
        List<UserFromDB> usersFromDB = dslContext.selectFrom("user_account")
                .where(field("id").eq(addedUser.getId()))
                .fetch(new UserDAO.UserMapper());
        assertThat(usersFromDB).hasSize(1);
        assertThatUserHasPassword(usersFromDB.get(0), "johns:password");
    }

    @Test
    public void testAddUserWithMaximumLengthPassword() {
        User user = new User(null, null, "John", "01234567890123456789012345678901");
        User addedUser = null;
        addedUser = userDAO.addUser(user);

        String id = addedUser.getId();
        assertThat(addedUser.getName()).isEqualTo("John");
        assertThat(addedUser.getPassword()).isNull();
        List<UserFromDB> usersFromDB = dslContext.selectFrom("user_account")
                .where(field("id").eq(id)).fetch(new UserDAO.UserMapper());
        assertThat(usersFromDB).hasSize(1);
        assertThat(usersFromDB.get(0).getName()).isEqualTo("John");
        assertThatUserHasPassword(usersFromDB.get(0), "01234567890123456789012345678901");
    }

    @Test
    public void testAddUserWithMinmumLengthPassword() {
        User user = new User(null, null, "John", "0123");
        User addedUser = null;
        addedUser = userDAO.addUser(user);

        String id = addedUser.getId();
        assertThat(addedUser.getName()).isEqualTo("John");
        assertThat(addedUser.getPassword()).isNull();
        List<UserFromDB> usersFromDB = dslContext.selectFrom("user_account")
                .where(field("id").eq(id)).fetch(new UserDAO.UserMapper());
        assertThat(usersFromDB).hasSize(1);
        assertThat(usersFromDB.get(0).getName()).isEqualTo("John");
        assertThatUserHasPassword(usersFromDB.get(0), "0123");
    }

    @Test
    public void testTryToAddUserWithTooLongPassword() {
        User user = new User(null, null, "John", "012345678901234567890123456789012");
        assertThatThrownBy(() -> {
            userDAO.addUser(user);
        }).isInstanceOf(ApplicationException.class).hasMessage("Invalid password.");
    }

    @Test
    public void testTryToAddUserWithTooShortPassword() {
        User user = new User(null, null, "John", "012");
        assertThatThrownBy(() -> {
            userDAO.addUser(user);
        }).isInstanceOf(ApplicationException.class).hasMessage("Invalid password.");
    }

    @Test
    public void testUpdateUserName() {
        User addedUser = userDAO.addUser(new User(null, null, "John", "johns-password"));
        userDAO.addUser(new User(null, null, "Joe", "joes-password"));

        User updatedUser = userDAO.updateUser(new User(
                addedUser.getId(),
                addedUser.getVersion(),
                "JohnsNewName",
                "johns-password"
        ), makeAuth(addedUser.getId(), "johns-password"));
        assertThat(updatedUser.getId()).isEqualTo(addedUser.getId());
        assertThat(updatedUser.getVersion()).isNotEqualTo(addedUser.getVersion());
        assertThat(updatedUser.getName()).isEqualTo("JohnsNewName");
        assertThat(updatedUser.getPassword()).isNull();
        List<UserFromDB> usersFromDB = dslContext.selectFrom("user_account")
                .where(field("id").eq(addedUser.getId()))
                .fetch(new UserDAO.UserMapper());
        assertThat(usersFromDB).hasSize(1);
        assertThatUserHasPassword(usersFromDB.get(0), "johns-password");
        assertThat(usersFromDB.get(0).getId()).isEqualTo(addedUser.getId());
        assertThat(usersFromDB.get(0).getVersion()).isNotEqualTo(addedUser.getVersion());
        assertThat(usersFromDB.get(0).getName()).isEqualTo("JohnsNewName");
        assertThat(usersFromDB.get(0).getNormalizedName()).isEqualTo("johnsnewname");
    }

    @Test
    public void testUpdateUserNameWithMaximumLengthName() {
        User addedUser = userDAO.addUser(new User(null, null, "John", "johns-password"));
        userDAO.addUser(new User(null, null, "Joe", "joes-password"));

        User updatedUser = userDAO.updateUser(new User(
                addedUser.getId(),
                addedUser.getVersion(),
                "abcdefghijklmnop",
                "johns-password"
        ), makeAuth(addedUser.getId(), "johns-password"));
        assertThat(updatedUser.getId()).isEqualTo(addedUser.getId());
        assertThat(updatedUser.getVersion()).isNotEqualTo(addedUser.getVersion());
        assertThat(updatedUser.getName()).isEqualTo("abcdefghijklmnop");
        assertThat(updatedUser.getPassword()).isNull();
        List<UserFromDB> usersFromDB = dslContext.selectFrom("user_account")
                .where(field("id").eq(addedUser.getId()))
                .fetch(new UserDAO.UserMapper());
        assertThat(usersFromDB).hasSize(1);
        assertThatUserHasPassword(usersFromDB.get(0), "johns-password");
        assertThat(usersFromDB.get(0).getId()).isEqualTo(addedUser.getId());
        assertThat(usersFromDB.get(0).getVersion()).isNotEqualTo(addedUser.getVersion());
        assertThat(usersFromDB.get(0).getName()).isEqualTo("abcdefghijklmnop");
        assertThat(usersFromDB.get(0).getNormalizedName()).isEqualTo("abcdefghijklmnop");
    }

    @Test
    public void testUpdateUserNameWithMinimumLengthName() {
        User addedUser = userDAO.addUser(new User(null, null, "John", "johns-password"));
        userDAO.addUser(new User(null, null, "Joe", "joes-password"));

        User updatedUser = userDAO.updateUser(new User(
                addedUser.getId(),
                addedUser.getVersion(),
                "abc",
                "johns-password"
        ), makeAuth(addedUser.getId(), "johns-password"));
        assertThat(updatedUser.getId()).isEqualTo(addedUser.getId());
        assertThat(updatedUser.getVersion()).isNotEqualTo(addedUser.getVersion());
        assertThat(updatedUser.getName()).isEqualTo("abc");
        assertThat(updatedUser.getPassword()).isNull();
        List<UserFromDB> usersFromDB = dslContext.selectFrom("user_account")
                .where(field("id").eq(addedUser.getId()))
                .fetch(new UserDAO.UserMapper());
        assertThat(usersFromDB).hasSize(1);
        assertThatUserHasPassword(usersFromDB.get(0), "johns-password");
        assertThat(usersFromDB.get(0).getId()).isEqualTo(addedUser.getId());
        assertThat(usersFromDB.get(0).getVersion()).isNotEqualTo(addedUser.getVersion());
        assertThat(usersFromDB.get(0).getName()).isEqualTo("abc");
        assertThat(usersFromDB.get(0).getNormalizedName()).isEqualTo("abc");
    }

    @Test
    public void testUpdateSpellingOfUserName() {
        User addedUser = userDAO.addUser(new User(null, null, "John", "johns-password"));
        userDAO.addUser(new User(null, null, "Joe", "joes-password"));

        User updatedUser = userDAO.updateUser(new User(
                addedUser.getId(),
                addedUser.getVersion(),
                "jOHN",
                "johns-password"
        ), makeAuth(addedUser.getId(), "johns-password"));
        assertThat(updatedUser.getId()).isEqualTo(addedUser.getId());
        assertThat(updatedUser.getVersion()).isNotEqualTo(addedUser.getVersion());
        assertThat(updatedUser.getName()).isEqualTo("jOHN");
        assertThat(updatedUser.getPassword()).isNull();
        List<UserFromDB> usersFromDB = dslContext.selectFrom("user_account")
                .where(field("id").eq(addedUser.getId()))
                .fetch(new UserDAO.UserMapper());
        assertThat(usersFromDB).hasSize(1);
        assertThatUserHasPassword(usersFromDB.get(0), "johns-password");
        assertThat(usersFromDB.get(0).getId()).isEqualTo(addedUser.getId());
        assertThat(usersFromDB.get(0).getVersion()).isNotEqualTo(addedUser.getVersion());
        assertThat(usersFromDB.get(0).getName()).isEqualTo("jOHN");
        assertThat(usersFromDB.get(0).getNormalizedName()).isEqualTo("john");
    }

    @Test
    public void testTryToUpdateUserNameWithTooLongName() {
        User addedUser = userDAO.addUser(new User(null, null, "John", "johns-password"));
        userDAO.addUser(new User(null, null, "Joe", "joes-password"));

        assertThatThrownBy(() -> {
            userDAO.updateUser(new User(
                    addedUser.getId(),
                    addedUser.getVersion(),
                    "abcdefghijklmnopq",
                    "johns-password"
            ), makeAuth(addedUser.getId(), "johns-password"));
        }).isInstanceOf(ApplicationException.class).hasMessage("Invalid user name.");
    }

    @Test
    public void testTryToUpdateUserNameWithTooShortName() {
        User addedUser = userDAO.addUser(new User(null, null, "John", "johns-password"));
        userDAO.addUser(new User(null, null, "Joe", "joes-password"));

        assertThatThrownBy(() -> {
            userDAO.updateUser(new User(
                    addedUser.getId(),
                    addedUser.getVersion(),
                    "ab",
                    "johns-password"
            ), makeAuth(addedUser.getId(), "johns-password"));
        }).isInstanceOf(ApplicationException.class).hasMessage("Invalid user name.");
    }

    @Test
    public void testTryToUpdateUserNameWithNameThatAlreadyExists() {
        User addedUser = userDAO.addUser(new User(null, null, "John", "johns-password"));
        userDAO.addUser(new User(null, null, "Joe", "joes-password"));

        assertThatThrownBy(() -> {
            userDAO.updateUser(new User(
                    addedUser.getId(),
                    addedUser.getVersion(),
                    "jOE",
                    "johns-password"
            ), makeAuth(addedUser.getId(), "johns-password"));
        }).isInstanceOf(ApplicationException.class).hasMessage("The new user name already exists.");
    }

    @Test
    public void testUpdateUserPassword() {
        User addedUser = userDAO.addUser(new User(null, null, "John", "johns-password"));
        userDAO.addUser(new User(null, null, "Joe", "joes-password"));

        User updatedUser = userDAO.updateUser(new User(
                addedUser.getId(),
                addedUser.getVersion(),
                "John",
                "johns-new-password"
        ), makeAuth(addedUser.getId(), "johns-password"));
        assertThat(updatedUser.getId()).isEqualTo(addedUser.getId());
        assertThat(updatedUser.getVersion()).isNotEqualTo(addedUser.getVersion());
        assertThat(updatedUser.getName()).isEqualTo("John");
        assertThat(updatedUser.getPassword()).isNull();
        List<UserFromDB> usersFromDB = dslContext.selectFrom("user_account")
                .where(field("id").eq(addedUser.getId()))
                .fetch(new UserDAO.UserMapper());
        assertThat(usersFromDB).hasSize(1);
        assertThatUserHasPassword(usersFromDB.get(0), "johns-new-password");
        assertThat(usersFromDB.get(0).getId()).isEqualTo(addedUser.getId());
        assertThat(usersFromDB.get(0).getVersion()).isNotEqualTo(addedUser.getVersion());
        assertThat(usersFromDB.get(0).getName()).isEqualTo("John");
        assertThat(usersFromDB.get(0).getNormalizedName()).isEqualTo("john");
    }

    @Test
    public void testUpdateUserPasswordWithMaximumLengthPassword() {
        User addedUser = userDAO.addUser(new User(null, null, "John", "johns-password"));
        userDAO.addUser(new User(null, null, "Joe", "joes-password"));

        User updatedUser = userDAO.updateUser(new User(
                addedUser.getId(),
                addedUser.getVersion(),
                "John",
                "01234567890123456789012345678901"
        ), makeAuth(addedUser.getId(), "johns-password"));
        assertThat(updatedUser.getId()).isEqualTo(addedUser.getId());
        assertThat(updatedUser.getVersion()).isNotEqualTo(addedUser.getVersion());
        assertThat(updatedUser.getName()).isEqualTo("John");
        assertThat(updatedUser.getPassword()).isNull();
        List<UserFromDB> usersFromDB = dslContext.selectFrom("user_account")
                .where(field("id").eq(addedUser.getId()))
                .fetch(new UserDAO.UserMapper());
        assertThat(usersFromDB).hasSize(1);
        assertThatUserHasPassword(usersFromDB.get(0), "01234567890123456789012345678901");
        assertThat(usersFromDB.get(0).getId()).isEqualTo(addedUser.getId());
        assertThat(usersFromDB.get(0).getVersion()).isNotEqualTo(addedUser.getVersion());
        assertThat(usersFromDB.get(0).getName()).isEqualTo("John");
        assertThat(usersFromDB.get(0).getNormalizedName()).isEqualTo("john");
    }

    @Test
    public void testUpdateUserPasswordWithMinimumLengthPassword() {
        User addedUser = userDAO.addUser(new User(null, null, "John", "johns-password"));
        userDAO.addUser(new User(null, null, "Joe", "joes-password"));

        User updatedUser = userDAO.updateUser(new User(
                addedUser.getId(),
                addedUser.getVersion(),
                "John",
                "0123"
        ), makeAuth(addedUser.getId(), "johns-password"));
        assertThat(updatedUser.getId()).isEqualTo(addedUser.getId());
        assertThat(updatedUser.getVersion()).isNotEqualTo(addedUser.getVersion());
        assertThat(updatedUser.getName()).isEqualTo("John");
        assertThat(updatedUser.getPassword()).isNull();
        List<UserFromDB> usersFromDB = dslContext.selectFrom("user_account")
                .where(field("id").eq(addedUser.getId()))
                .fetch(new UserDAO.UserMapper());
        assertThat(usersFromDB).hasSize(1);
        assertThatUserHasPassword(usersFromDB.get(0), "0123");
        assertThat(usersFromDB.get(0).getId()).isEqualTo(addedUser.getId());
        assertThat(usersFromDB.get(0).getVersion()).isNotEqualTo(addedUser.getVersion());
        assertThat(usersFromDB.get(0).getName()).isEqualTo("John");
        assertThat(usersFromDB.get(0).getNormalizedName()).isEqualTo("john");
    }

    @Test
    public void tryToUpdatePasswordWithTooLongPassword() {
        User addedUser = userDAO.addUser(new User(null, null, "John", "johns-password"));
        userDAO.addUser(new User(null, null, "Joe", "joes-password"));

        assertThatThrownBy(() -> {
            userDAO.updateUser(new User(
                    addedUser.getId(),
                    addedUser.getVersion(),
                    "John",
                    "012345678901234567890123456789012"
            ), makeAuth(addedUser.getId(), "johns-password"));
        }).isInstanceOf(ApplicationException.class).hasMessage("Invalid password.");
    }

    @Test
    public void tryToUpdatePasswordWithTooShortPassword() {
        User addedUser = userDAO.addUser(new User(null, null, "John", "johns-password"));
        userDAO.addUser(new User(null, null, "Joe", "joes-password"));

        assertThatThrownBy(() -> {
            userDAO.updateUser(new User(
                    addedUser.getId(),
                    addedUser.getVersion(),
                    "John",
                    "012"
            ), makeAuth(addedUser.getId(), "johns-password"));
        }).isInstanceOf(ApplicationException.class).hasMessage("Invalid password.");
    }

    @Test
    public void testTryToUpdateUserWithWrongPassword() {
        User addedUser = userDAO.addUser(new User(null, null, "John", "johns-password"));
        User otherUser = userDAO.addUser(new User(null, null, "Joe", "joes-password"));

        assertThatThrownBy(() -> {
                userDAO.updateUser(new User(
                        addedUser.getId(),
                        addedUser.getVersion(),
                        "JohnsNewName",
                        "johns-new-password"
                ), makeAuth(addedUser.getId(), "joes-password"));
            }).isInstanceOf(ApplicationException.class).hasMessage("Wrong credentials.");

        List<UserFromDB> usersFromDB = dslContext.selectFrom("user_account")
                .fetch(new UserDAO.UserMapper());
        assertThat(usersFromDB).hasSize(2);
        List<String> names = usersFromDB.stream().map(UserFromDB::getName).collect(Collectors.toList());
        assertThat(names).containsExactlyInAnyOrder("John", "Joe");
        for(UserFromDB userFromDB : usersFromDB) {
            if ("John".equals(userFromDB.getName())) {
                assertThatUserHasPassword(userFromDB, "johns-password");
            } else if ("Joe".equals(userFromDB.getName())) {
                assertThatUserHasPassword(userFromDB, "joes-password");
            }
        }
    }

    @Test
    public void testTryToUpdateUserWithOutdatedVersion() {
        User addedUser = userDAO.addUser(new User(null, null, "John", "johns-password"));
        userDAO.addUser(new User(null, null, "Joe", "joes-password"));

        User updatedUser = userDAO.updateUser(new User(
                addedUser.getId(),
                addedUser.getVersion(),
                "JohnsNewName",
                "johns-new-password"
        ), makeAuth(addedUser.getId(), "johns-password"));
        assertThatThrownBy(() -> {
                userDAO.updateUser(new User(
                        addedUser.getId(),
                        addedUser.getVersion(),
                        "JohnsNewName",
                        "johns-even-newer-password"
                ), makeAuth(addedUser.getId(), "johns-new-password"));
        }).isInstanceOf(ApplicationException.class).hasMessage("The version is outdated.");
    }

    @Test
    public void testTryToUpdateUserThatDoesNotExist() {
        User addedUser = userDAO.addUser(new User(null, null, "John", "johns-password"));
        userDAO.addUser(new User(null, null, "Joe", "joes-password"));

        assertThatThrownBy(() -> {
                userDAO.updateUser(new User(
                        "wrong-id",
                        addedUser.getVersion(),
                        "JohnsNewName",
                        "johns-new-password"
                ), makeAuth(addedUser.getId(), "johns-password"));
        })
        .isInstanceOf(ApplicationException.class).hasMessage("User not found.");
    }

    @Test
    public void testDeleteUser() {
        User addedUser = userDAO.addUser(new User(null, null, "John", "johns-password"));
        User otherUser = userDAO.addUser(new User(null, null, "Joe", "joes-password"));
        User deletedUser = userDAO.deleteUser(addedUser.getId(), makeAuth(addedUser.getId(), "johns-password"));
        assertThat(deletedUser).isEqualTo(addedUser);
        List<UserFromDB> remainingUsers = dslContext.selectFrom("user_account").fetch(new UserDAO.UserMapper());
        assertThat(remainingUsers).hasSize(1);
        assertThat(remainingUsers.get(0).getName()).isEqualTo("Joe");
        assertThatUserHasPassword(remainingUsers.get(0), "joes-password");
    }

    @Test
    public void testTryToDeleteUserWithWrongPassword() {
        User addedUser = userDAO.addUser(new User(null, null, "John", "johns-password"));
        User otherUser = userDAO.addUser(new User(null, null, "Joe", "joes-password"));
        assertThatThrownBy(() -> {
            userDAO.deleteUser(addedUser.getId(), makeAuth(addedUser.getId(), "joes-password"));
        }).isInstanceOf(ApplicationException.class).hasMessage("Wrong credentials.");
    }

    @Test
    public void testAuthenticateByAuth() {
        User addedUser = userDAO.addUser(new User(null, null, "John", "johns-password"));
        User otherUser = userDAO.addUser(new User(null, null, "Joe", "joes-password"));
        String auth = makeAuth(addedUser.getId(), "johns-password");
        User expected = new User(addedUser.getId(), addedUser.getVersion(), "John", null);
        User actual = userDAO.authenticate(auth);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testTryAuthenticateByAuthWithWrongPassword() {
        User addedUser = userDAO.addUser(new User(null, null, "John", "johns-password"));
        User otherUser = userDAO.addUser(new User(null, null, "Joe", "joes-password"));
        String auth = makeAuth(addedUser.getId(), "wrong-password");
        assertThatThrownBy(() -> {
            userDAO.authenticate(auth);
        }).isInstanceOf(ApplicationException.class).hasMessage("Wrong credentials.");
    }

    public static String makeAuth(String id, String password) {
        return "Basic " + new String(Base64.getEncoder().encode((id + ":" + password).getBytes(StandardCharsets.UTF_8)));
    }

    private void assertThatUserHasPassword(UserFromDB userFromDB, String password) {
        String hashedPassword = userFromDB.getHashedPassword();
        String salt = userFromDB.getSalt();
        String expectedHashedPassword = hashPasswordWithSalt(password, salt);
        assertThat(hashedPassword).isEqualTo(expectedHashedPassword);
    }

    private String hashPasswordWithSalt(String password, String salt) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        assertThat(digest).isNotNull();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(Base64.getDecoder().decode(salt));
            stream.write(password.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String(Base64.getEncoder().encode(digest.digest(stream.toByteArray())), StandardCharsets.UTF_8);
    }
}
