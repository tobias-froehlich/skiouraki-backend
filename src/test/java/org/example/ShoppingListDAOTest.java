package org.example;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ShoppingListDAOTest extends TestWithDB {

    private UserDAO userDAO;
    private ShoppingListDAO shoppingListDAO;

    @BeforeAll
    @Override
    public void beforeAll() {
        super.beforeAll();
        userDAO = new UserDAO(dslContext);
        shoppingListDAO = new ShoppingListDAO(dslContext);
    }

    @BeforeEach
    @Override
    public void beforeEach() {
        super.beforeEach();
        dslContext.insertInto(table("user_account"))
                .columns(field("id"), field("version"), field("name"), field("normalized_name"), field("hashed_password"), field("salt"))
                .values("id-john", "version-john", "John", "john", "hashed-password-john", "salt-john")
                .execute();
        dslContext.insertInto(table("user_account"))
                .columns(field("id"), field("version"), field("name"), field("normalized_name"), field("hashed_password"), field("salt"))
                .values("id-joe", "version-joe", "Joe", "joe", "hashed-password-joe", "salt-joe")
                .execute();
    }

    @Test
    public void testGetShoppingList() {
        dslContext.insertInto(table("shopping_list"))
                .columns(field("id"), field("version"), field("name"), field("owner"))
                .values("id-1", "version-1", "list-name-1", "id-john")
                .execute();
        dslContext.insertInto(table("shopping_list"))
                .columns(field("id"), field("version"), field("name"), field("owner"))
                .values("id-2", "version-2", "list-name-2", "id-joe")
                .execute();
        ShoppingList expected = new ShoppingList("id-1", "version-1", "list-name-1", "id-john");
        ShoppingList actual = shoppingListDAO.getShoppingList("id-1");

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testTryGettingNotExistingShoppingList() {
        dslContext.insertInto(table("shopping_list"))
                .columns(field("id"), field("version"), field("name"), field("owner"))
                .values("id-1", "version-1", "list-name-1", "id-john")
                .execute();
        dslContext.insertInto(table("shopping_list"))
                .columns(field("id"), field("version"), field("name"), field("owner"))
                .values("id-2", "version-2", "list-name-2", "id-joe")
                .execute();
        assertThatThrownBy(() -> {
            shoppingListDAO.getShoppingList("not-existing-id");
        }).isInstanceOf(ApplicationException.class).hasMessage("ShoppingList not found.");
    }

    @Test
    public void testGetOwnShoppingLists() {
        dslContext.insertInto(table("shopping_list"))
                .columns(field("id"), field("version"), field("name"), field("owner"))
                .values("id-1", "version-1", "list-name-1", "id-john")
                .execute();
        dslContext.insertInto(table("shopping_list"))
                .columns(field("id"), field("version"), field("name"), field("owner"))
                .values("id-2", "version-2", "list-name-2", "id-joe")
                .execute();
        dslContext.insertInto(table("shopping_list"))
                .columns(field("id"), field("version"), field("name"), field("owner"))
                .values("id-3", "version-3", "list-name-3", "id-joe")
                .execute();
        List<ShoppingList> expected = List.of(
                new ShoppingList("id-2", "version-2", "list-name-2", "id-joe"),
                new ShoppingList("id-3", "version-3", "list-name-3", "id-joe")
        );
        User authenticatedUser = new User("id-joe", "version-joe", "Joe", null);
        List<ShoppingList> actual = shoppingListDAO.getOwnShoppingLists(authenticatedUser);
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testGetOwnShoppingListsWhereTheNumberIsZero() {
        dslContext.insertInto(table("shopping_list"))
                .columns(field("id"), field("version"), field("name"), field("owner"))
                .values("id-1", "version-1", "list-name-1", "id-john")
                .execute();
        User authenticatedUser = new User("id-joe", "version-joe", "Joe", null);
        List<ShoppingList> actual = shoppingListDAO.getOwnShoppingLists(authenticatedUser);
        assertThat(actual).hasSize(0);
    }

    @Test
    public void testAddShoppingList() {
        User authenticatedUser = new User("id-joe", "version-joe", "Joe", null);
        ShoppingList newShoppingList = new ShoppingList("", "", "new-shopping-list", "");
        ShoppingList addedShoppingList = shoppingListDAO.addShoppingList(authenticatedUser, newShoppingList);
        String id = addedShoppingList.getId();
        String version = addedShoppingList.getVersion();
        ShoppingList expected = new ShoppingList(id, version, "new-shopping-list", "id-joe");
        assertThat(addedShoppingList).isEqualTo(expected);
        ShoppingList fromDb = shoppingListDAO.getShoppingList(id);
        assertThat(fromDb).isEqualTo(expected);
    }

    @Test
    public void testAddShoppingListWithMinimalNameLength() {
        User authenticatedUser = new User("id-joe", "version-joe", "Joe", null);
        ShoppingList newShoppingList = new ShoppingList("", "", "a", "");
        ShoppingList addedShoppingList = shoppingListDAO.addShoppingList(authenticatedUser, newShoppingList);
        String id = addedShoppingList.getId();
        String version = addedShoppingList.getVersion();
        ShoppingList expected = new ShoppingList(id, version, "a", "id-joe");
        assertThat(addedShoppingList).isEqualTo(expected);
        ShoppingList fromDb = shoppingListDAO.getShoppingList(id);
        assertThat(fromDb).isEqualTo(expected);
    }

    @Test
    public void testAddShoppingListWithTooShortName() {
        User authenticatedUser = new User("id-joe", "version-joe", "Joe", null);
        ShoppingList newShoppingList = new ShoppingList("", "", "", "");
        assertThatThrownBy(() -> {
            shoppingListDAO.addShoppingList(authenticatedUser, newShoppingList);
        }).isInstanceOf(ApplicationException.class).hasMessage("Invalid name.");
    }

    @Test
    public void testAddShoppingListWithMaximalNameLength() {
        User authenticatedUser = new User("id-joe", "version-joe", "Joe", null);
        ShoppingList newShoppingList = new ShoppingList("", "", "abcdefghijklmnopqrstuvwxyzabcdef", "");
        ShoppingList addedShoppingList = shoppingListDAO.addShoppingList(authenticatedUser, newShoppingList);
        String id = addedShoppingList.getId();
        String version = addedShoppingList.getVersion();
        ShoppingList expected = new ShoppingList(id, version, "abcdefghijklmnopqrstuvwxyzabcdef", "id-joe");
        assertThat(addedShoppingList).isEqualTo(expected);
        ShoppingList fromDb = shoppingListDAO.getShoppingList(id);
        assertThat(fromDb).isEqualTo(expected);
    }

    @Test
    public void testAddShoppingListWithTooLongName() {
        User authenticatedUser = new User("id-joe", "version-joe", "Joe", null);
        ShoppingList newShoppingList = new ShoppingList("", "", "abcdefghijklmnopqrstuvwxyzabcdefg", "");
        assertThatThrownBy(() -> {
            shoppingListDAO.addShoppingList(authenticatedUser, newShoppingList);
        }).isInstanceOf(ApplicationException.class).hasMessage("Invalid name.");
    }

    @Test
    public void testRenameShoppingList() {
        User authenticatedUser = new User("id-joe", "version-joe", "Joe", null);
        ShoppingList newShoppingList = new ShoppingList("", "", "old-name", "");
        ShoppingList addedShoppingList = shoppingListDAO.addShoppingList(authenticatedUser, newShoppingList);
        String id = addedShoppingList.getId();
        String version = addedShoppingList.getVersion();
        ShoppingList renamedShoppingList = shoppingListDAO.renameShoppingList(authenticatedUser, id, "new-name");
        assertThat(renamedShoppingList.getId()).isEqualTo(id);
        assertThat(renamedShoppingList.getVersion()).isNotEqualTo(version);
        assertThat(renamedShoppingList.getName()).isEqualTo("new-name");
        assertThat(renamedShoppingList.getOwner()).isEqualTo("id-joe");
        ShoppingList fromDb = shoppingListDAO.getShoppingList(id);
        assertThat(fromDb).isEqualTo(renamedShoppingList);
    }

    @Test
    public void testRenameShoppingListWithMaximumLengthName() {
        User authenticatedUser = new User("id-joe", "version-joe", "Joe", null);
        ShoppingList newShoppingList = new ShoppingList("", "", "old-name", "");
        ShoppingList addedShoppingList = shoppingListDAO.addShoppingList(authenticatedUser, newShoppingList);
        String id = addedShoppingList.getId();
        String version = addedShoppingList.getVersion();
        ShoppingList renamedShoppingList = shoppingListDAO.renameShoppingList(authenticatedUser, id, "abcdefghijklmnopqrstuvwxyzabcdef");
        assertThat(renamedShoppingList.getId()).isEqualTo(id);
        assertThat(renamedShoppingList.getVersion()).isNotEqualTo(version);
        assertThat(renamedShoppingList.getName()).isEqualTo("abcdefghijklmnopqrstuvwxyzabcdef");
        assertThat(renamedShoppingList.getOwner()).isEqualTo("id-joe");
        ShoppingList fromDb = shoppingListDAO.getShoppingList(id);
        assertThat(fromDb).isEqualTo(renamedShoppingList);
    }

    @Test
    public void testTryToRenameShoppingListWithTooLongName() {
        User authenticatedUser = new User("id-joe", "version-joe", "Joe", null);
        ShoppingList newShoppingList = new ShoppingList("", "", "old-name", "");
        ShoppingList addedShoppingList = shoppingListDAO.addShoppingList(authenticatedUser, newShoppingList);
        String id = addedShoppingList.getId();
        assertThatThrownBy(() -> {
                    shoppingListDAO.renameShoppingList(authenticatedUser, id, "abcdefghijklmnopqrstuvwxyzabcdefg");
        }).isInstanceOf(ApplicationException.class).hasMessage("Invalid name.");
        ShoppingList fromDb = shoppingListDAO.getShoppingList(id);
        assertThat(fromDb).isEqualTo(addedShoppingList);
    }

    @Test
    public void testRenameShoppingListWithMinimumLengthName() {
        User authenticatedUser = new User("id-joe", "version-joe", "Joe", null);
        ShoppingList newShoppingList = new ShoppingList("", "", "old-name", "");
        ShoppingList addedShoppingList = shoppingListDAO.addShoppingList(authenticatedUser, newShoppingList);
        String id = addedShoppingList.getId();
        String version = addedShoppingList.getVersion();
        ShoppingList renamedShoppingList = shoppingListDAO.renameShoppingList(authenticatedUser, id, "a");
        assertThat(renamedShoppingList.getId()).isEqualTo(id);
        assertThat(renamedShoppingList.getVersion()).isNotEqualTo(version);
        assertThat(renamedShoppingList.getName()).isEqualTo("a");
        assertThat(renamedShoppingList.getOwner()).isEqualTo("id-joe");
        ShoppingList fromDb = shoppingListDAO.getShoppingList(id);
        assertThat(fromDb).isEqualTo(renamedShoppingList);
    }

    @Test
    public void testTryToRenameShoppingListWithTooShortName() {
        User authenticatedUser = new User("id-joe", "version-joe", "Joe", null);
        ShoppingList newShoppingList = new ShoppingList("", "", "old-name", "");
        ShoppingList addedShoppingList = shoppingListDAO.addShoppingList(authenticatedUser, newShoppingList);
        String id = addedShoppingList.getId();
        assertThatThrownBy(() -> {
            shoppingListDAO.renameShoppingList(authenticatedUser, id, "");
        }).isInstanceOf(ApplicationException.class).hasMessage("Invalid name.");
        ShoppingList fromDb = shoppingListDAO.getShoppingList(id);
        assertThat(fromDb).isEqualTo(addedShoppingList);
    }

    @Test
    public void testTryToRenameShoppingListAsWrongUser() {
        User authenticatedUser = new User("id-joe", "version-joe", "Joe", null);
        User wrongUser = new User("id-john", "version-john", "John", null);
        ShoppingList newShoppingList = new ShoppingList("", "", "old-name", "");
        ShoppingList addedShoppingList = shoppingListDAO.addShoppingList(authenticatedUser, newShoppingList);
        String id = addedShoppingList.getId();
        assertThatThrownBy(() -> {
            shoppingListDAO.renameShoppingList(wrongUser, id, "new-name");
        }).isInstanceOf(ApplicationException.class).hasMessage("Cannot rename ShoppingList.");
        ShoppingList fromDb = shoppingListDAO.getShoppingList(id);
        assertThat(fromDb).isEqualTo(addedShoppingList);
    }

    @Test
    public void testDeleteShoppingList() {
        User authenticatedUser = new User("id-joe", "version-joe", "Joe", null);
        ShoppingList newShoppingList = new ShoppingList("", "", "new-shopping-list", "");
        ShoppingList addedShoppingList = shoppingListDAO.addShoppingList(authenticatedUser, newShoppingList);
        String id = addedShoppingList.getId();
        String version = addedShoppingList.getVersion();
        ShoppingList expected = new ShoppingList(id, version, "new-shopping-list", "id-joe");
        assertThat(addedShoppingList).isEqualTo(expected);
        ShoppingList fromDb = shoppingListDAO.getShoppingList(id);
        assertThat(fromDb).isEqualTo(expected);
        ShoppingList deletedShoppingList = shoppingListDAO.deleteShoppingList(authenticatedUser, id);
        assertThat(deletedShoppingList).isEqualTo(expected);
        assertThatThrownBy(() -> {
            shoppingListDAO.getShoppingList(id);
        }).isInstanceOf(ApplicationException.class).hasMessage("ShoppingList not found.");
    }

    @Test
    public void testTryToDeleteShoppingListAsWrongUser() {
        User authenticatedUser = new User("id-joe", "version-joe", "Joe", null);
        User wrongUser = new User("id-john", "version-john", "John", null);
        ShoppingList newShoppingList = new ShoppingList("", "", "new-shopping-list", "");
        ShoppingList addedShoppingList = shoppingListDAO.addShoppingList(authenticatedUser, newShoppingList);
        String id = addedShoppingList.getId();
        String version = addedShoppingList.getVersion();
        ShoppingList expected = new ShoppingList(id, version, "new-shopping-list", "id-joe");
        assertThat(addedShoppingList).isEqualTo(expected);
        ShoppingList fromDb = shoppingListDAO.getShoppingList(id);
        assertThat(fromDb).isEqualTo(expected);
        assertThatThrownBy(() -> {
            shoppingListDAO.deleteShoppingList(wrongUser, id);
        }).isInstanceOf(ApplicationException.class).hasMessage("Cannot delete ShoppingList.");
    }

}
