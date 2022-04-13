package org.example;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

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
                .set(field("password"), "johns-password")
                .execute();
        dslContext.insertInto(table("user_account"))
                .set(field("id"), "test-id-2")
                .set(field("version"), "test-version-2")
                .set(field("name"), "Joe")
                .set(field("password"), "joes-password")
                .execute();
        List<User> expected = List.of(
                new User("test-id-1", "test-version-1", "John", ""),
                new User("test-id-2", "test-version-2", "Joe", "")
        );
        List<User> actual = userDAO.getAllAppUsers();
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);

    }

    @Test
    public void testGetUser() {
        dslContext.insertInto(table("user_account"))
                .set(field("id"), "test-id")
                .set(field("version"), "test-version")
                .set(field("name"), "John")
                .set(field("password"), "johns-password")
                .execute();
        User expected = new User("test-id", "test-version", "John", "johns-password");
        User actual = userDAO.getUser("test-id", "Basic dGVzdC1pZDpqb2hucy1wYXNzd29yZA==");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testTryToGetUserWithWrongPassword() {
        dslContext.insertInto(table("user_account"))
                .set(field("id"), "test-id")
                .set(field("version"), "test-version")
                .set(field("name"), "John")
                .set(field("password"), "johns-password")
                .execute();
        assertThatThrownBy(() -> {
            userDAO.getUser("test-id", makeAuth("test-id", "wrong-password"));
        }).isInstanceOf(ApplicationException.class).hasMessage("Wrong credentials.");

    }

    @Test
    public void testGetUserByName() {
        dslContext.insertInto(table("user_account"))
                .set(field("id"), "test-id")
                .set(field("version"), "test-version")
                .set(field("name"), "John")
                .set(field("normalized_name"), "john")
                .set(field("password"), "johns-password")
                .execute();
        dslContext.insertInto(table("user_account"))
                .set(field("id"), "test-id2")
                .set(field("version"), "test-version2")
                .set(field("name"), "Joe")
                .set(field("normalized_name"), "joe")
                .set(field("password"), "joes-password")
                .execute();
        String expected = "test-id";
        String actual = userDAO.getUserIdByName("John");
        assertThat(actual).isEqualTo(expected);
    }

//    @Test
//    public void testTryGetUserByNameWithWrongPassword() {
//        dslContext.insertInto(table("user_account"))
//                .set(field("id"), "test-id")
//                .set(field("version"), "test-version")
//                .set(field("name"), "John")
//                .set(field("password"), "johns-password")
//                .execute();
//        dslContext.insertInto(table("user_account"))
//                .set(field("id"), "test-id2")
//                .set(field("version"), "test-version2")
//                .set(field("name"), "Joe")
//                .set(field("password"), "joes-password")
//                .execute();
//
//        assertThatThrownBy(() -> {
//            userDAO.getUserByName("John", makeAuth("test-id", "joes-password"));
//        }).isInstanceOf(ApplicationException.class).hasMessage("Wrong credentials.");
//
//    }

    @Test
    public void testAddUser() {
        User user = new User(null, null, "John", "johns-password");
        User addedUser = userDAO.addUser(user);
        String id = addedUser.getId();
        assertThat(addedUser.getName()).isEqualTo("John");
        assertThat(addedUser.getPassword()).isEqualTo("johns-password");
        List<User> usersFromDb = dslContext.selectFrom("user_account")
                .where(field("id").eq(id)).fetch(new UserDAO.UserMapper());
        assertThat(usersFromDb).containsExactlyInAnyOrderElementsOf(List.of(addedUser));
    }

    @Test
    public void testTryToAddUserTwice() {
        User user = new User(null, null, "John", "johns-password");
        User addedUser = userDAO.addUser(user);
        assertThatThrownBy(() -> {
            userDAO.addUser(user);
        }).isInstanceOf(ApplicationException.class).hasMessage("Cannot insert user. User with name John already exists.");
    }

    @Test
    public void testTryToAddUserTwiceWithDifferentSpelling() {
        User user1 = new User(null, null, "John", "johns-password");
        User addedUser = userDAO.addUser(user1);
        User user2 = new User(null, null, "john", "johns-password");
        assertThatThrownBy(() -> {
            userDAO.addUser(user2);
        }).isInstanceOf(ApplicationException.class).hasMessage("Cannot insert user. User with name john already exists.");
    }

    @Test
    public void testAddUserWithMaximumLengthName() {
        User user = new User(null, null, "abcdefghijklmnop", "johns-password");
        User addedUser = userDAO.addUser(user);
        String id = addedUser.getId();
        assertThat(addedUser.getName()).isEqualTo("abcdefghijklmnop");
        assertThat(addedUser.getPassword()).isEqualTo("johns-password");
        List<User> usersFromDb = dslContext.selectFrom("user_account")
                .where(field("id").eq(id)).fetch(new UserDAO.UserMapper());
        assertThat(usersFromDb).containsExactlyInAnyOrderElementsOf(List.of(addedUser));
    }

    @Test
    public void testTryToAddUserWithTooLongUsername() {
        User user = new User(null, null, "abcdefghijklmnopq", "johns-password");
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
    public void testUpdateUser() {
        User addedUser = userDAO.addUser(new User(null, null, "John", "johns-password"));
        userDAO.addUser(new User(null, null, "Joe", "joes-password"));

        User updatedUser = userDAO.updateUser(new User(
                addedUser.getId(),
                addedUser.getVersion(),
                "JohnsNewName",
                "johns-new-password"
        ), makeAuth(addedUser.getId(), "johns-password"));
        assertThat(updatedUser.getId()).isEqualTo(addedUser.getId());
        assertThat(updatedUser.getVersion()).isNotEqualTo(addedUser.getVersion());
        assertThat(updatedUser.getName()).isEqualTo("JohnsNewName");
        assertThat(updatedUser.getPassword()).isEqualTo("johns-new-password");
        List<User> usersFromDb = dslContext.selectFrom("user_account")
                .where(field("id").eq(addedUser.getId()))
                .fetch(new UserDAO.UserMapper());
        assertThat(usersFromDb).containsOnly(updatedUser);
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

        List<User> usersFromDb = dslContext.selectFrom("user_account")
                .fetch(new UserDAO.UserMapper());
        assertThat(usersFromDb).containsOnly(addedUser, otherUser);
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
        }).isInstanceOf(ApplicationException.class).hasMessage("The user does not exist or the version is outdated.");
    }

    @Test
    public void testDeleteUser() {
        User addedUser = userDAO.addUser(new User(null, null, "John", "johns-password"));
        User otherUser = userDAO.addUser(new User(null, null, "Joe", "joes-password"));
        User deletedUser = userDAO.deleteUser(addedUser, makeAuth(addedUser.getId(), "johns-password"));
        assertThat(deletedUser).isEqualTo(addedUser);
        List<User> remainingUsers = dslContext.selectFrom("user_account").fetch(new UserDAO.UserMapper());
        assertThat(remainingUsers).containsOnly(otherUser);
    }

    @Test
    public void testTryToDeleteUserWithWrongPassword() {
        User addedUser = userDAO.addUser(new User(null, null, "John", "johns-password"));
        User otherUser = userDAO.addUser(new User(null, null, "Joe", "joes-password"));
        assertThatThrownBy(() -> {
            userDAO.deleteUser(addedUser, makeAuth(addedUser.getId(), "joes-password"));
        }).isInstanceOf(ApplicationException.class).hasMessage("Wrong credentials.");
    }

    private String makeAuth(String id, String password) {
        return "Basic " + new String(Base64.getEncoder().encode((id + ":" + password).getBytes(StandardCharsets.UTF_8)));
    }
}
