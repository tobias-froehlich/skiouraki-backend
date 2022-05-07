package org.example;

import org.jooq.Record;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ShoppingListDAOTest extends TestWithDB {

    private UserDAO userDAO;
    private ShoppingListDAO shoppingListDAO;

    private final static User JOHN = new User("id-john", "version-john", "John", null);
    private final static User JOE = new User("id-joe", "version-joe", "Joe", null);
    private final static User JACK = new User("id-jack", "version-jack", "Jack", null);


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
        dslContext.insertInto(table("user_account"))
                .columns(field("id"), field("version"), field("name"), field("normalized_name"), field("hashed_password"), field("salt"))
                .values("id-jack", "version-jack", "Jack", "jack", "hashed-password-jack", "salt-jack")
                .execute();
    }

    @Test
    public void testGetShoppingList() {
        dslContext.insertInto(table("shopping_list"))
                .columns(field("id"), field("version"), field("name"), field("owner"))
                .values("id-1", "version-1", "list-name-1", "id-john")
                .execute();
        dslContext.insertInto(table("shopping_list_authorization"))
                .columns(field("shopping_list_id"), field("user_id"), field("invitation_accepted"))
                .values("id-1", "id-john", true)
                .execute();
        dslContext.insertInto(table("shopping_list"))
                .columns(field("id"), field("version"), field("name"), field("owner"))
                .values("id-2", "version-2", "list-name-2", "id-joe")
                .execute();
        dslContext.insertInto(table("shopping_list_authorization"))
                .columns(field("shopping_list_id"), field("user_id"), field("invitation_accepted"))
                .values("id-2", "id-joe", true)
                .execute();
        ShoppingList expected = new ShoppingList("id-1", "version-1", "list-name-1", "id-john");
        ShoppingList actual = shoppingListDAO.getShoppingList(JOHN, "id-1");

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
            shoppingListDAO.getShoppingList(JOHN, "not-existing-id");
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
        ShoppingList fromDb = shoppingListDAO.getShoppingList(JOE, id);
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
        ShoppingList fromDb = shoppingListDAO.getShoppingList(JOE, id);
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
        ShoppingList fromDb = shoppingListDAO.getShoppingList(JOE, id);
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
        ShoppingList fromDb = shoppingListDAO.getShoppingList(JOE, id);
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
        ShoppingList fromDb = shoppingListDAO.getShoppingList(JOE, id);
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
        ShoppingList fromDb = shoppingListDAO.getShoppingList(JOE, id);
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
        ShoppingList fromDb = shoppingListDAO.getShoppingList(JOE, id);
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
        ShoppingList fromDb = shoppingListDAO.getShoppingList(JOE, id);
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
        ShoppingList fromDb = shoppingListDAO.getShoppingList(JOE, id);
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
        ShoppingList fromDb = shoppingListDAO.getShoppingList(JOE, id);
        assertThat(fromDb).isEqualTo(expected);
        ShoppingList deletedShoppingList = shoppingListDAO.deleteShoppingList(authenticatedUser, id);
        assertThat(deletedShoppingList).isEqualTo(expected);
        assertThatThrownBy(() -> {
            shoppingListDAO.getShoppingList(JOE, id);
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
        ShoppingList fromDb = shoppingListDAO.getShoppingList(JOE, id);
        assertThat(fromDb).isEqualTo(expected);
        assertThatThrownBy(() -> {
            shoppingListDAO.deleteShoppingList(wrongUser, id);
        }).isInstanceOf(ApplicationException.class).hasMessage("ShoppingList not found.");
        ShoppingList actual = shoppingListDAO.getShoppingList(JOE, addedShoppingList.getId());
        assertThat(actual).isEqualTo(addedShoppingList);
    }

    @Test
    public void testGetInvitationsByShoppingList() {
        dslContext.insertInto(table("shopping_list"))
                .columns(field("id"), field("version"), field("name"), field("owner"))
                .values("id-1", "version-1", "list-name-1", "id-jack")
                .execute();
        dslContext.insertInto(table("shopping_list"))
                .columns(field("id"), field("version"), field("name"), field("owner"))
                .values("id-2", "version-2", "list-name-2", "id-joe")
                .execute();
        dslContext.insertInto(table("shopping_list_authorization"))
                .columns(field("shopping_list_id"), field("user_id"), field("invitation_accepted"))
                .values("id-1", "id-jack", true)
                .execute();
        dslContext.insertInto(table("shopping_list_authorization"))
                .columns(field("shopping_list_id"), field("user_id"), field("invitation_accepted"))
                .values("id-2", "id-joe", true)
                .execute();
        dslContext.insertInto(table("shopping_list_authorization"))
                .columns(field("shopping_list_id"), field("user_id"), field("invitation_accepted"))
                .values("id-1", "id-john", false)
                .execute();
        dslContext.insertInto(table("shopping_list_authorization"))
                .columns(field("shopping_list_id"), field("user_id"), field("invitation_accepted"))
                .values("id-1", "id-joe", false)
                .execute();
        dslContext.insertInto(table("shopping_list_authorization"))
                .columns(field("shopping_list_id"), field("user_id"), field("invitation_accepted"))
                .values("id-2", "id-john", false)
                .execute();
        List<User> expected1 = List.of(JOHN, JOE);
        List<User> expected2 = List.of(JOHN);
        List<User> actual1 = shoppingListDAO.getInvitationsByShoppingList(JACK, "id-1");
        List<User> actual2 = shoppingListDAO.getInvitationsByShoppingList(JOE, "id-2");
        assertThat(actual1).containsExactlyInAnyOrderElementsOf(expected1);
        assertThat(actual2).containsExactlyInAnyOrderElementsOf(expected2);
    }

    @Test
    public void testGetInvitationsByUser() {
        dslContext.insertInto(table("shopping_list"))
                .columns(field("id"), field("version"), field("name"), field("owner"))
                .values("id-1", "version-1", "list-name-1", "id-jack")
                .execute();
        dslContext.insertInto(table("shopping_list"))
                .columns(field("id"), field("version"), field("name"), field("owner"))
                .values("id-2", "version-2", "list-name-2", "id-joe")
                .execute();
        dslContext.insertInto(table("shopping_list_authorization"))
                .columns(field("shopping_list_id"), field("user_id"), field("invitation_accepted"))
                .values("id-1", "id-jack", true)
                .execute();
        dslContext.insertInto(table("shopping_list_authorization"))
                .columns(field("shopping_list_id"), field("user_id"), field("invitation_accepted"))
                .values("id-2", "id-joe", true)
                .execute();
        dslContext.insertInto(table("shopping_list_authorization"))
                .columns(field("shopping_list_id"), field("user_id"), field("invitation_accepted"))
                .values("id-1", "id-john", false)
                .execute();
        dslContext.insertInto(table("shopping_list_authorization"))
                .columns(field("shopping_list_id"), field("user_id"), field("invitation_accepted"))
                .values("id-1", "id-joe", false)
                .execute();
        dslContext.insertInto(table("shopping_list_authorization"))
                .columns(field("shopping_list_id"), field("user_id"), field("invitation_accepted"))
                .values("id-2", "id-john", true)
                .execute();
        List<ShoppingList> expected = List.of(new ShoppingList("id-1", "version-1", "list-name-1", "id-jack"));
        List<ShoppingList> actual1 = shoppingListDAO.getInvitationsByUser(JOHN);
        List<ShoppingList> actual2 = shoppingListDAO.getInvitationsByUser(JOE);
        assertThat(actual1).containsExactlyInAnyOrderElementsOf(expected);
        assertThat(actual2).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testGetMembers() {
        dslContext.insertInto(table("shopping_list"))
                .columns(field("id"), field("version"), field("name"), field("owner"))
                .values("id-1", "version-1", "list-name-1", "id-jack")
                .execute();
        dslContext.insertInto(table("shopping_list"))
                .columns(field("id"), field("version"), field("name"), field("owner"))
                .values("id-2", "version-2", "list-name-2", "id-joe")
                .execute();
        dslContext.insertInto(table("shopping_list_authorization"))
                .columns(field("shopping_list_id"), field("user_id"), field("invitation_accepted"))
                .values("id-1", "id-jack", true)
                .execute();
        dslContext.insertInto(table("shopping_list_authorization"))
                .columns(field("shopping_list_id"), field("user_id"), field("invitation_accepted"))
                .values("id-2", "id-joe", true)
                .execute();
        dslContext.insertInto(table("shopping_list_authorization"))
                .columns(field("shopping_list_id"), field("user_id"), field("invitation_accepted"))
                .values("id-1", "id-john", true)
                .execute();
        dslContext.insertInto(table("shopping_list_authorization"))
                .columns(field("shopping_list_id"), field("user_id"), field("invitation_accepted"))
                .values("id-1", "id-joe", false)
                .execute();
        dslContext.insertInto(table("shopping_list_authorization"))
                .columns(field("shopping_list_id"), field("user_id"), field("invitation_accepted"))
                .values("id-2", "id-john", true)
                .execute();
        List<User> expected1 = List.of(JACK, JOHN);
        List<User> expected2 = List.of(JOE, JOHN);
        List<User> actual1 = shoppingListDAO.getMembers("id-1");
        List<User> actual2 = shoppingListDAO.getMembers("id-2");
        assertThat(actual1).containsExactlyInAnyOrderElementsOf(expected1);
        assertThat(actual2).containsExactlyInAnyOrderElementsOf(expected2);
    }

    @Test
    public void testInvite() {
        dslContext.insertInto(table("shopping_list"))
                .columns(field("id"), field("version"), field("name"), field("owner"))
                .values("id-1", "version-1", "list-name-1", "id-jack")
                .execute();
        List<User> expected = List.of(JOHN);
        List<User> actual = shoppingListDAO.invite(JACK, JOHN, "id-1");
        String newVersion = dslContext.selectFrom("shopping_list")
                .where(field("id").eq("id-1"))
                .fetchOne(new ShoppingListDAO.ShoppingListMapper()).getVersion();
        assertThat(newVersion).isNotEqualTo("version-1");
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
        actual = shoppingListDAO.getInvitationsByShoppingList(JACK, "id-1");
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
        expected = List.of(JOHN, JOE);
        actual = shoppingListDAO.invite(JACK, JOE, "id-1");
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
        actual = shoppingListDAO.getInvitationsByShoppingList(JACK, "id-1");
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testTryToGetInvitationsByShoppingListAsNotAuthorizedUser() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList(null, null, "Jack's shopping list", null));
        shoppingListDAO.invite(JACK, JOHN, shoppingList.getId());
        List<User> expected = List.of();
        List<User> actual = shoppingListDAO.getInvitationsByShoppingList(JOHN, shoppingList.getId());
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testTryToInviteTwice() {
        dslContext.insertInto(table("shopping_list"))
                .columns(field("id"), field("version"), field("name"), field("owner"))
                .values("id-1", "version-1", "list-name-1", "id-jack")
                .execute();
        dslContext.insertInto(table("shopping_list_authorization"))
                .columns(field("shopping_list_id"), field("user_id"), field("invitation_accepted"))
                .values("id-1", "id-jack", true)
                .execute();
        shoppingListDAO.invite(JACK, JOHN, "id-1");
        String oldVersion = shoppingListDAO.getShoppingList(JACK, "id-1").getVersion();
        assertThatThrownBy(() -> {
            shoppingListDAO.invite(JACK, JOHN, "id-1");
        }).isInstanceOf(ApplicationException.class).hasMessage("Cannot invite user to ShoppingList.");
        String newVersion = shoppingListDAO.getShoppingList(JACK, "id-1").getVersion();
        assertThat(newVersion).isEqualTo(oldVersion);
    }

    @Test
    public void testWithdrawInvitation() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList(null, null, "Jack's shopping list", null));
        shoppingListDAO.invite(JACK, JOHN, shoppingList.getId());
        shoppingListDAO.invite(JACK, JOE, shoppingList.getId());
        List<User> expected = List.of(JOHN, JOE);
        List<User> actual = shoppingListDAO.getInvitationsByShoppingList(JACK, shoppingList.getId());
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
        shoppingListDAO.withdrawInvitation(JACK, JOHN, shoppingList.getId());
        expected = List.of(JOE);
        actual = shoppingListDAO.getInvitationsByShoppingList(JACK, shoppingList.getId());
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
        String oldVersion = shoppingListDAO.getShoppingList(JACK, shoppingList.getId()).getVersion();
        shoppingListDAO.withdrawInvitation(JACK, JOE, shoppingList.getId());
        String newVersion = shoppingListDAO.getShoppingList(JACK, shoppingList.getId()).getVersion();
        assertThat(newVersion).isNotEqualTo(oldVersion);
        expected = List.of();
        actual = shoppingListDAO.getInvitationsByShoppingList(JACK, shoppingList.getId());
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testTryToWithdrawInvitationThatDoesNotExist() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList(null, null, "Jack's shopping list", null));
        String oldVersion = shoppingList.getVersion();
        assertThatThrownBy(() -> {
            shoppingListDAO.withdrawInvitation(JACK, JOHN, shoppingList.getId());
        }).isInstanceOf(ApplicationException.class).hasMessage("Cannot withdraw invitation because it was not found.");
        String newVersion = shoppingListDAO.getShoppingList(JACK, shoppingList.getId()).getVersion();
        assertThat(newVersion).isEqualTo(oldVersion);
    }

    @Test
    public void testTryToInviteTheOwner() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList(null, null, "Jack's shopping list", null));
        String oldVersion = shoppingList.getVersion();
        assertThatThrownBy(() -> {
            shoppingListDAO.invite(JACK, JACK, shoppingList.getId());
        }).isInstanceOf(ApplicationException.class).hasMessage("Cannot invite user to ShoppingList.");
        String newVersion = shoppingListDAO.getShoppingList(JACK, shoppingList.getId()).getVersion();
        assertThat(newVersion).isEqualTo(oldVersion);
    }

    @Test
    public void testAcceptInvitation() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList(null, null, "Jack's shopping list", null));
        shoppingListDAO.invite(JACK, JOHN, shoppingList.getId());
        shoppingListDAO.acceptInvitation(JOHN, shoppingList.getId());
        List<Boolean> expected = List.of(true);
        List<Boolean> actual = dslContext.selectFrom("shopping_list_authorization")
                .where(field("shopping_list_id").eq(shoppingList.getId()))
                .and(field("user_id").eq(JOHN.getId()))
                .fetch("invitation_accepted", Boolean.class);
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testTryToAcceptInvitationThatDoesNotExist() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList(null, null, "Jack's shopping list", null));
        String oldVersion = shoppingListDAO.getShoppingList(JACK, shoppingList.getId()).getVersion();
        assertThatThrownBy(() -> {
            shoppingListDAO.acceptInvitation(JOHN, shoppingList.getId());
        }).isInstanceOf(ApplicationException.class).hasMessage("Invitation not found.");
        String newVersion = shoppingListDAO.getShoppingList(JACK, shoppingList.getId()).getVersion();
        assertThat(newVersion).isEqualTo(oldVersion);
    }

    @Test
    public void testRejectInvitation() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList(null, null, "Jack's shopping list", null));
        shoppingListDAO.invite(JACK, JOHN, shoppingList.getId());
        shoppingListDAO.invite(JACK, JOE, shoppingList.getId());
        shoppingListDAO.rejectInvitation(JOHN, shoppingList.getId());
        Integer count = dslContext.selectCount().from("shopping_list_authorization")
                .where(field("shopping_list_id").eq(shoppingList.getId()))
                .and(field("user_id").eq(JOHN.getId()))
                .fetchOne(0, Integer.class);
        assertThat(count).isEqualTo(0);
        count = dslContext.selectCount().from("shopping_list_authorization")
                .where(field("shopping_list_id").eq(shoppingList.getId()))
                .and(field("user_id").eq(JOE.getId()))
                .fetchOne(0, Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    public void testTryToRejectInvitationAsOwner() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList(null, null, "Jack's shopping list", null));
        shoppingListDAO.invite(JACK, JOHN, shoppingList.getId());
        shoppingListDAO.invite(JACK, JOE, shoppingList.getId());
        String oldVersion = shoppingListDAO.getShoppingList(JACK, shoppingList.getId()).getVersion();
        assertThatThrownBy(() -> {
            shoppingListDAO.rejectInvitation(JACK, shoppingList.getId());
        }).isInstanceOf(ApplicationException.class).hasMessage("Cannot reject invitation.");
        String newVersion = shoppingListDAO.getShoppingList(JACK, shoppingList.getId()).getVersion();
        assertThat(newVersion).isEqualTo(oldVersion);
    }

    @Test
    public void testGetShoppingListsAfterAcceptingInvitation() {
        ShoppingList shoppingList1 = shoppingListDAO.addShoppingList(JACK, new ShoppingList(null, null, "Jack's shopping list", null));
        ShoppingList shoppingList2 = shoppingListDAO.addShoppingList(JACK, new ShoppingList(null, null, "Jack's other shopping list", null));
        shoppingListDAO.invite(JACK, JOHN, shoppingList1.getId());
        shoppingListDAO.invite(JACK, JOHN, shoppingList2.getId());
        String oldVersion1 = shoppingListDAO.getShoppingList(JACK, shoppingList1.getId()).getVersion();
        String oldVersion2 = shoppingListDAO.getShoppingList(JACK, shoppingList2.getId()).getVersion();
        shoppingListDAO.acceptInvitation(JOHN, shoppingList1.getId());
        shoppingListDAO.acceptInvitation(JOHN, shoppingList2.getId());
        String newVersion1 = shoppingListDAO.getShoppingList(JACK, shoppingList1.getId()).getVersion();
        String newVersion2 = shoppingListDAO.getShoppingList(JACK, shoppingList2.getId()).getVersion();
        assertThat(newVersion1).isNotEqualTo(oldVersion1);
        assertThat(newVersion2).isNotEqualTo(oldVersion2);
        shoppingList1.setVersion(newVersion1);
        shoppingList2.setVersion(newVersion2);
        List<ShoppingList> expected = List.of(shoppingList1, shoppingList2);
        List<ShoppingList> actual = shoppingListDAO.getShoppingLists(JOHN);
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testGetShoppingListsAfterAcceptingOneInvitationAndRejectingTheOtherOne() {
        ShoppingList shoppingList1 = shoppingListDAO.addShoppingList(JACK, new ShoppingList(null, null, "Jack's shopping list", null));
        ShoppingList shoppingList2 = shoppingListDAO.addShoppingList(JACK, new ShoppingList(null, null, "Jack's other shopping list", null));
        shoppingListDAO.invite(JACK, JOHN, shoppingList1.getId());
        shoppingListDAO.invite(JACK, JOHN, shoppingList2.getId());
        String oldVersion1 = shoppingListDAO.getShoppingList(JACK, shoppingList1.getId()).getVersion();
        String oldVersion2 = shoppingListDAO.getShoppingList(JACK, shoppingList2.getId()).getVersion();
        shoppingListDAO.acceptInvitation(JOHN, shoppingList1.getId());
        shoppingListDAO.rejectInvitation(JOHN, shoppingList2.getId());
        String newVersion1 = shoppingListDAO.getShoppingList(JACK, shoppingList1.getId()).getVersion();
        String newVersion2 = shoppingListDAO.getShoppingList(JACK, shoppingList2.getId()).getVersion();
        assertThat(newVersion1).isNotEqualTo(oldVersion1);
        assertThat(newVersion2).isNotEqualTo(oldVersion2);
        shoppingList1.setVersion(newVersion1);
        List<ShoppingList> expected = List.of(shoppingList1);
        List<ShoppingList> actual = shoppingListDAO.getShoppingLists(JOHN);
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testGetShoppingListAfterAcceptingInvitation() {
        ShoppingList expected = shoppingListDAO.addShoppingList(JACK, new ShoppingList(null, null, "Jack's shopping list", null));
        shoppingListDAO.invite(JACK, JOHN, expected.getId());
        shoppingListDAO.acceptInvitation(JOHN, expected.getId());
        ShoppingList actual = shoppingListDAO.getShoppingList(JOHN, expected.getId());
        assertThat(actual.getVersion()).isNotEqualTo(expected.getVersion());
        expected.setVersion(actual.getVersion());
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testTryToGetShoppingListWithoutAcceptingInvitation() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList(null, null, "Jack's shopping list", null));
        shoppingListDAO.invite(JACK, JOHN, shoppingList.getId());
        assertThatThrownBy(() -> {
            shoppingListDAO.getShoppingList(JOHN, shoppingList.getId());
        }).isInstanceOf(ApplicationException.class).hasMessage("ShoppingList not found.");
    }

    @Test
    public void testTryToDeleteShoppingListAfterAcceptingInvitation() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList(null, null, "Jack's shopping list", null));
        shoppingListDAO.invite(JACK, JOHN, shoppingList.getId());
        shoppingListDAO.acceptInvitation(JOHN, shoppingList.getId());
        String oldVersion = shoppingListDAO.getShoppingList(JACK, shoppingList.getId()).getVersion();
        assertThatThrownBy(() -> {
            shoppingListDAO.deleteShoppingList(JOHN, shoppingList.getId());
        }).isInstanceOf(ApplicationException.class).hasMessage("Cannot delete ShoppingList.");
        String newVersion = shoppingListDAO.getShoppingList(JACK, shoppingList.getId()).getVersion();
        assertThat(newVersion).isEqualTo(oldVersion);
    }

    @Test
    public void testDeleteUserAccountWhenUserOwnsAShoppingList() {
        User Jim = userDAO.addUser(new User("", "", "Jim", "jims-password"));
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(Jim, new ShoppingList("", "", "Jim's shopping list", ""));
        userDAO.deleteUser(Jim.getId(), UserDAOTest.makeAuth(Jim.getId(), "jims-password"));
        Integer count = dslContext.selectCount().from("shopping_list")
                .where(field("id").eq(shoppingList.getId()))
                .fetchOne(0, Integer.class);
        assertThat(count).isEqualTo(0);
    }

    @Test
    public void testDeleteUserAccountWhenIsInvitedToAShoppingList() {
        User Jim = userDAO.addUser(new User("", "", "Jim", "jims-password"));
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jim's shopping list", ""));
        shoppingListDAO.invite(JACK, Jim, shoppingList.getId());
        userDAO.deleteUser(Jim.getId(), UserDAOTest.makeAuth(Jim.getId(), "jims-password"));
        List<User> invitedUsers = shoppingListDAO.getInvitationsByShoppingList(JACK, shoppingList.getId());
        assertThat(invitedUsers).hasSize(0);
    }

    @Test
    public void testDeleteUserAccountWhenInvitedOthersToAShoppingList() {
        User Jim = userDAO.addUser(new User("", "", "Jim", "jims-password"));
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(Jim, new ShoppingList("", "", "Jim's shopping list", ""));
        shoppingListDAO.invite(Jim, JOHN, shoppingList.getId());
        userDAO.deleteUser(Jim.getId(), UserDAOTest.makeAuth(Jim.getId(), "jims-password"));
        List<User> invitedUsers = shoppingListDAO.getInvitationsByShoppingList(Jim, shoppingList.getId());
        assertThat(invitedUsers).hasSize(0);
    }

    @Test
    public void testLeaveShoppingList() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JOHN, new ShoppingList("", "", "John's shopping list", ""));
        dslContext.insertInto(table("shopping_list_authorization"))
                .columns(field("shopping_list_id"), field("user_id"), field("invitation_accepted"))
                .values(shoppingList.getId(), JOE.getId(), true)
                .execute();
        List<User> expected = List.of(JOHN, JOE);
        List<User> actual = shoppingListDAO.getMembers(shoppingList.getId());
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
        String oldVersion = shoppingListDAO.getShoppingList(JOHN, shoppingList.getId()).getVersion();
        List<User> actual1 = shoppingListDAO.leaveShoppingList(JOE, JOE, shoppingList.getId());
        String newVersion = shoppingListDAO.getShoppingList(JOHN, shoppingList.getId()).getVersion();
        assertThat(newVersion).isNotEqualTo(oldVersion);
        expected = List.of(JOHN);
        List<User> actual2 = shoppingListDAO.getMembers(shoppingList.getId());
        assertThat(actual1).containsExactlyInAnyOrderElementsOf(expected);
        assertThat(actual2).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testRemoveFromShoppingList() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JOHN, new ShoppingList("", "", "John's shopping list", ""));
        dslContext.insertInto(table("shopping_list_authorization"))
                .columns(field("shopping_list_id"), field("user_id"), field("invitation_accepted"))
                .values(shoppingList.getId(), JOE.getId(), true)
                .execute();
        List<User> expected = List.of(JOHN, JOE);
        List<User> actual = shoppingListDAO.getMembers(shoppingList.getId());
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
        String oldVersion = shoppingListDAO.getShoppingList(JOHN, shoppingList.getId()).getVersion();
        List<User> actual1 = shoppingListDAO.leaveShoppingList(JOHN, JOE, shoppingList.getId());
        String newVersion = shoppingListDAO.getShoppingList(JOHN, shoppingList.getId()).getVersion();
        assertThat(newVersion).isNotEqualTo(oldVersion);
        expected = List.of(JOHN);
        List<User> actual2 = shoppingListDAO.getMembers(shoppingList.getId());
        assertThat(actual1).containsExactlyInAnyOrderElementsOf(expected);
        assertThat(actual2).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void tryToLeaveShoppingListAsOwner() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JOHN, new ShoppingList("", "", "John's shopping list", ""));
        assertThatThrownBy(() -> {
            shoppingListDAO.leaveShoppingList(JOHN, JOHN, shoppingList.getId());
        }).isInstanceOf(ApplicationException.class).hasMessage("Cannot leave ShoppingList.");
        String newVersion = shoppingListDAO.getShoppingList(JOHN, shoppingList.getId()).getVersion();
        assertThat(newVersion).isEqualTo(shoppingList.getVersion());
    }

    @Test
    public void testRemoveFromShoppingListAsNotAuthorizedUser() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JOHN, new ShoppingList("", "", "John's shopping list", ""));
        dslContext.insertInto(table("shopping_list_authorization"))
                .columns(field("shopping_list_id"), field("user_id"), field("invitation_accepted"))
                .values(shoppingList.getId(), JOE.getId(), true)
                .execute();
        assertThatThrownBy(() -> {
            shoppingListDAO.leaveShoppingList(JACK, JOE, shoppingList.getId());
        }).isInstanceOf(ApplicationException.class).hasMessage("Cannot leave ShoppingList.");
    }

    @Test
    public void testGetEnrichedShoppingList() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        EnrichedShoppingList expected = new EnrichedShoppingList(
                shoppingList.getId(),
                shoppingList.getVersion(),
                "Jack's shopping list",
                JACK.getId(),
                List.of(JACK),
                List.of(),
                List.of());
        EnrichedShoppingList actual = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId());
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testGetEnrichedShoppingListWithFurtherMembersAndInvitedUsers() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        shoppingListDAO.invite(JACK, JOHN, shoppingList.getId());
        shoppingListDAO.invite(JACK, JOE, shoppingList.getId());
        shoppingListDAO.acceptInvitation(JOHN, shoppingList.getId());
        String newVersion = shoppingListDAO.getShoppingList(JACK, shoppingList.getId()).getVersion();
        EnrichedShoppingList expected = new EnrichedShoppingList(
                shoppingList.getId(),
                newVersion,
                "Jack's shopping list",
                JACK.getId(),
                List.of(JACK, JOHN),
                List.of(JOE),
                List.of());
        EnrichedShoppingList actual1 = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId());
        assertThat(actual1).isEqualTo(expected);
        EnrichedShoppingList actual2 = shoppingListDAO.getEnrichedShoppingList(JOHN, shoppingList.getId());
        assertThat(actual2).isEqualTo(expected);
    }

    @Test
    public void testTryToGetEnrichedShoppingListAsNotAuthorizedUser() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        shoppingListDAO.invite(JACK, JOHN, shoppingList.getId());
        assertThatThrownBy(() -> {
            shoppingListDAO.getEnrichedShoppingList(JOHN, shoppingList.getId());
        }).isInstanceOf(ApplicationException.class).hasMessage("ShoppingList not found.");
        assertThatThrownBy(() -> {
            shoppingListDAO.getEnrichedShoppingList(JOE, shoppingList.getId());
        }).isInstanceOf(ApplicationException.class).hasMessage("ShoppingList not found.");
    }

    @Test
    public void testGetEnrichedShoppingListWithItems() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        dslContext.insertInto(table("shopping_list_item"))
                .columns(
                        field("id"),
                        field("version"),
                        field("name"),
                        field("created_by"),
                        field("modified_by"),
                        field("state_changed_by"),
                        field("shopping_list_id"),
                        field("sort_order"))
                .values(
                        "item-id-1",
                        "item-version-1",
                        "Bananen",
                        JACK.getId(),
                        JACK.getId(),
                        JACK.getId(),
                        shoppingList.getId(),
                        0)
                .execute();
        dslContext.insertInto(table("shopping_list_item"))
                .columns(
                        field("id"),
                        field("version"),
                        field("name"),
                        field("created_by"),
                        field("modified_by"),
                        field("state_changed_by"),
                        field("shopping_list_id"),
                        field("sort_order"))
                .values(
                        "item-id-2",
                        "item-version-2",
                        "Äpfel",
                        JACK.getId(),
                        JACK.getId(),
                        JACK.getId(),
                        shoppingList.getId(),
                        1)
                .execute();
        List<ShoppingListItem> expected = List.of(
                new ShoppingListItem("item-id-1", "item-version-1", "Bananen", JACK.getId(), JACK.getId(), null, JACK.getId()),
                new ShoppingListItem("item-id-2", "item-version-2", "Äpfel", JACK.getId(), JACK.getId(), null, JACK.getId())
        );
        List<ShoppingListItem> actual = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId()).getItems();
        assertThat(actual).containsExactlyElementsOf(expected);

    }

    @Test
    public void testAddShoppingListItem() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        ShoppingListItem newItem1 = new ShoppingListItem("", "", "Bananen", "", "", "", "");
        EnrichedShoppingList enrichedShoppingList = shoppingListDAO.addShoppingListItem(
                JACK,
                shoppingList.getId(),
                newItem1
        );
        String newItemId1 = enrichedShoppingList.getItems().get(0).getId();
        String newItemVersion1 = enrichedShoppingList.getItems().get(0).getVersion();
        ShoppingListItem expectedItem1 = new ShoppingListItem(newItemId1, newItemVersion1, "Bananen", JACK.getId(), JACK.getId(), null, JACK.getId());
        assertThat(enrichedShoppingList.getItems()).containsExactly(expectedItem1);
        List<Record> recordsInShoppingListItem = dslContext.selectFrom("shopping_list_item").fetch();
        assertThat(recordsInShoppingListItem).hasSize(1);
        assertThat(recordsInShoppingListItem.get(0).getValue("name", String.class)).isEqualTo("Bananen");
        assertThat(recordsInShoppingListItem.get(0).getValue("created_by", String.class)).isEqualTo(JACK.getId());
        assertThat(recordsInShoppingListItem.get(0).getValue("modified_by", String.class)).isEqualTo(JACK.getId());
        assertThat(recordsInShoppingListItem.get(0).getValue("state_changed_by", String.class)).isEqualTo(JACK.getId());
        String itemId1 = recordsInShoppingListItem.get(0).getValue("id", String.class);
        ShoppingListItem newItem2 = new ShoppingListItem("", "", "Äpfel", "", "", "", "");
        enrichedShoppingList = shoppingListDAO.addShoppingListItem(
                JACK,
                shoppingList.getId(),
                newItem2
        );
        String newItemId2 = enrichedShoppingList.getItems().get(1).getId();
        String newItemVersion2 = enrichedShoppingList.getItems().get(1).getVersion();
        ShoppingListItem expectedItem2 = new ShoppingListItem(newItemId2, newItemVersion2, "Äpfel", JACK.getId(), JACK.getId(), null, JACK.getId());
        assertThat(enrichedShoppingList.getItems()).containsExactly(expectedItem1, expectedItem2);
        recordsInShoppingListItem = dslContext.selectFrom("shopping_list_item").where(field("id").ne(itemId1)).fetch();
        assertThat(recordsInShoppingListItem).hasSize(1);
        assertThat(recordsInShoppingListItem.get(0).getValue("name", String.class)).isEqualTo("Äpfel");
        assertThat(recordsInShoppingListItem.get(0).getValue("created_by", String.class)).isEqualTo(JACK.getId());
        assertThat(recordsInShoppingListItem.get(0).getValue("modified_by", String.class)).isEqualTo(JACK.getId());
        assertThat(recordsInShoppingListItem.get(0).getValue("state_changed_by", String.class)).isEqualTo(JACK.getId());
    }

    @Test
    public void testVersionChangesWhenItemIsAdded() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        EnrichedShoppingList enrichedShoppingList = shoppingListDAO.addShoppingListItem(
                JACK,
                shoppingList.getId(),
                new ShoppingListItem("", "", "Bananen", "", "", "", "")
        );
        assertThat(shoppingList.getVersion()).isNotEqualTo(enrichedShoppingList.getVersion());
    }

    @Test
    public void testTryAddItemWithoutAuthorization() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        shoppingListDAO.addShoppingListItem(
                JACK,
                shoppingList.getId(),
                new ShoppingListItem("", "", "Äpfel", "", "", "", "")
        );
        assertThatThrownBy(() -> {
            shoppingListDAO.addShoppingListItem(
                    JOHN,
                    shoppingList.getId(),
                    new ShoppingListItem("", "", "Bananen", "", "", "", "")
            );
        }).isInstanceOf(ApplicationException.class).hasMessage("ShoppingList not found.");
        EnrichedShoppingList enrichedShoppingList = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId());
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
    }

    @Test
    public void testTryAddItemWithoutAuthorizationAfterInvitation() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        shoppingListDAO.invite(JACK, JOHN, shoppingList.getId());
        shoppingListDAO.addShoppingListItem(
                JACK,
                shoppingList.getId(),
                new ShoppingListItem("", "", "Äpfel", "", "", "", "")
        );
        assertThatThrownBy(() -> {
            shoppingListDAO.addShoppingListItem(
                    JOHN,
                    shoppingList.getId(),
                    new ShoppingListItem("", "", "Bananen", "", "", "", "")
            );
        }).isInstanceOf(ApplicationException.class).hasMessage("ShoppingList not found.");
        EnrichedShoppingList enrichedShoppingList = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId());
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
    }

    @Test
    public void testAddItemAsNormalMember() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        shoppingListDAO.addShoppingListItem(
                JACK,
                shoppingList.getId(),
                new ShoppingListItem("", "", "Äpfel", "", "", "", "")
        );
        shoppingListDAO.invite(JACK, JOHN, shoppingList.getId());
        shoppingListDAO.acceptInvitation(JOHN, shoppingList.getId());
        shoppingListDAO.addShoppingListItem(
                JOHN,
                shoppingList.getId(),
                new ShoppingListItem("", "", "Bananen", "", "", "", "")
        );
        EnrichedShoppingList enrichedShoppingList = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId());
        assertThat(enrichedShoppingList.getItems()).hasSize(2);
    }

    @Test
    public void testCreatedByModifiedByStateChangedByWhenAddingItemAsOwner() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        shoppingListDAO.invite(JACK, JOHN, shoppingList.getId());
        shoppingListDAO.acceptInvitation(JOHN, shoppingList.getId());
        shoppingListDAO.addShoppingListItem(
                JACK,
                shoppingList.getId(),
                new ShoppingListItem("", "", "Bananen", "", "", "", "")
        );
        EnrichedShoppingList enrichedShoppingList = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId());
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        ShoppingListItem item = enrichedShoppingList.getItems().get(0);
        assertThat(item.getCreatedBy()).isEqualTo(JACK.getId());
        assertThat(item.getModifiedBy()).isEqualTo(JACK.getId());
        assertThat(item.getStateChangedBy()).isEqualTo(JACK.getId());
    }

    @Test
    public void testCreatedByModifiedByStateChangedByWhenAddingItemAsNormalMember() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        shoppingListDAO.invite(JACK, JOHN, shoppingList.getId());
        shoppingListDAO.acceptInvitation(JOHN, shoppingList.getId());
        shoppingListDAO.addShoppingListItem(
                JOHN,
                shoppingList.getId(),
                new ShoppingListItem("", "", "Bananen", "", "", "", "")
        );
        EnrichedShoppingList enrichedShoppingList = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId());
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        ShoppingListItem item = enrichedShoppingList.getItems().get(0);
        assertThat(item.getCreatedBy()).isEqualTo(JOHN.getId());
        assertThat(item.getModifiedBy()).isEqualTo(JOHN.getId());
        assertThat(item.getStateChangedBy()).isEqualTo(JOHN.getId());
    }

    @Test
    public void testRemoveItem() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        EnrichedShoppingList enrichedShoppingList = shoppingListDAO.addShoppingListItem(
                JACK,
                shoppingList.getId(),
                new ShoppingListItem("", "", "Äpfel", "", "", "", "")
        );
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        enrichedShoppingList = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId());
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        ShoppingListItem item = enrichedShoppingList.getItems().get(0);
        String oldVersion = enrichedShoppingList.getVersion();
        enrichedShoppingList = shoppingListDAO.removeShoppingListItem(JACK, shoppingList.getId(), item);
        String newVersion = enrichedShoppingList.getVersion();
        assertThat(newVersion).isNotEqualTo(oldVersion);
        assertThat(enrichedShoppingList.getItems()).hasSize(0);
        enrichedShoppingList = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId());
        assertThat(enrichedShoppingList.getItems()).hasSize(0);
    }

    @Test
    public void testTryToRemoveItemWithoutAuthorization() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        EnrichedShoppingList enrichedShoppingList = shoppingListDAO.addShoppingListItem(
                JACK,
                shoppingList.getId(),
                new ShoppingListItem("", "", "Äpfel", "", "", "", "")
        );
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        enrichedShoppingList = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId());
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        ShoppingListItem item = enrichedShoppingList.getItems().get(0);
        assertThatThrownBy(() -> {
                    shoppingListDAO.removeShoppingListItem(JOHN, shoppingList.getId(), item);
        }).isInstanceOf(ApplicationException.class).hasMessage("ShoppingList not found.");
        enrichedShoppingList = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId());
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
    }

    @Test
    public void testTryToRemoveItemWithoutAuthorizationAfterInvitation() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        shoppingListDAO.invite(JACK, JOHN, shoppingList.getId());
        EnrichedShoppingList enrichedShoppingList = shoppingListDAO.addShoppingListItem(
                JACK,
                shoppingList.getId(),
                new ShoppingListItem("", "", "Äpfel", "", "", "", "")
        );
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        enrichedShoppingList = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId());
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        ShoppingListItem item = enrichedShoppingList.getItems().get(0);
        assertThatThrownBy(() -> {
            shoppingListDAO.removeShoppingListItem(JOHN, shoppingList.getId(), item);
        }).isInstanceOf(ApplicationException.class).hasMessage("ShoppingList not found.");
        enrichedShoppingList = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId());
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
    }

    @Test
    public void testRemoveItemAsNormalMember() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        shoppingListDAO.invite(JACK, JOHN, shoppingList.getId());
        shoppingListDAO.acceptInvitation(JOHN, shoppingList.getId());
        EnrichedShoppingList enrichedShoppingList = shoppingListDAO.addShoppingListItem(
                JACK,
                shoppingList.getId(),
                new ShoppingListItem("", "", "Äpfel", "", "", "", "")
        );
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        enrichedShoppingList = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId());
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        ShoppingListItem item = enrichedShoppingList.getItems().get(0);
        String oldVersion = enrichedShoppingList.getVersion();
        enrichedShoppingList = shoppingListDAO.removeShoppingListItem(JOHN, shoppingList.getId(), item);
        String newVersion = enrichedShoppingList.getVersion();
        assertThat(newVersion).isNotEqualTo(oldVersion);
        assertThat(enrichedShoppingList.getItems()).hasSize(0);
        enrichedShoppingList = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId());
        assertThat(enrichedShoppingList.getItems()).hasSize(0);
    }

    @Test
    public void testTryToRemoveItemWhenVersionIsOutdated() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        EnrichedShoppingList enrichedShoppingList = shoppingListDAO.addShoppingListItem(
                JACK,
                shoppingList.getId(),
                new ShoppingListItem("", "", "Äpfel", "", "", "", "")
        );
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        ShoppingListItem outdatedItem = enrichedShoppingList.getItems().get(0);
        dslContext.update(table("shopping_list_item"))
                .set(field("version"), "new-version")
                .where(field("id").eq(outdatedItem.getId()))
                .execute();
        assertThatThrownBy(() -> {
            shoppingListDAO.removeShoppingListItem(JACK, shoppingList.getId(), outdatedItem);
        }).isInstanceOf(ApplicationException.class).hasMessage("Cannot remove ShoppingListItem.");
        enrichedShoppingList = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId());
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
    }

    @Test
    public void testAddItemWithMaximalLengthName() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        EnrichedShoppingList enrichedShoppingList = shoppingListDAO.addShoppingListItem(
                JACK,
                shoppingList.getId(),
                new ShoppingListItem("", "", "12345678901234567890123456789012", "", "", "", "")
        );
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        enrichedShoppingList = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId());
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
    }

    @Test
    public void testAddItemWithMinimalLengthName() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        EnrichedShoppingList enrichedShoppingList = shoppingListDAO.addShoppingListItem(
                JACK,
                shoppingList.getId(),
                new ShoppingListItem("", "", "1", "", "", "", "")
        );
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        enrichedShoppingList = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId());
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
    }

    @Test
    public void testTryToAddItemWithTooLongName() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        assertThatThrownBy(() -> {
            shoppingListDAO.addShoppingListItem(
                    JACK,
                    shoppingList.getId(),
                    new ShoppingListItem("", "", "123456789012345678901234567890123", "", "", "", "")
            );
        }).isInstanceOf(ApplicationException.class).hasMessage("Invalid name.");
        EnrichedShoppingList enrichedShoppingList = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId());
        assertThat(enrichedShoppingList.getItems()).hasSize(0);
    }

    @Test
    public void testTryToAddItemWithTooShortName() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        assertThatThrownBy(() -> {
            shoppingListDAO.addShoppingListItem(
                    JACK,
                    shoppingList.getId(),
                    new ShoppingListItem("", "", "", "", "", "", "")
            );
        }).isInstanceOf(ApplicationException.class).hasMessage("Invalid name.");
        EnrichedShoppingList enrichedShoppingList = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId());
        assertThat(enrichedShoppingList.getItems()).hasSize(0);
    }

    @Test
    public void testSetItemToBoughtAsOwner() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        EnrichedShoppingList enrichedShoppingList = shoppingListDAO.addShoppingListItem(
                JACK,
                shoppingList.getId(),
                new ShoppingListItem("", "", "Bananen", "", "", "", "")
        );
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        ShoppingListItem item = enrichedShoppingList.getItems().get(0);
        String oldVersion = enrichedShoppingList.getVersion();
        enrichedShoppingList = shoppingListDAO.setBought(JACK, shoppingList.getId(), item);
        String newVersion = enrichedShoppingList.getVersion();
        assertThat(newVersion).isNotEqualTo(oldVersion);
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        item = enrichedShoppingList.getItems().get(0);
        assertThat(item.getBoughtBy()).isEqualTo(JACK.getId());
        assertThat(item.getStateChangedBy()).isEqualTo(JACK.getId());
    }

    @Test
    public void testSetItemToBoughtAsNormalMember() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        EnrichedShoppingList enrichedShoppingList = shoppingListDAO.addShoppingListItem(
                JACK,
                shoppingList.getId(),
                new ShoppingListItem("", "", "Bananen", "", "", "", "")
        );
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        ShoppingListItem item = enrichedShoppingList.getItems().get(0);
        shoppingListDAO.invite(JACK, JOHN, shoppingList.getId());
        shoppingListDAO.acceptInvitation(JOHN, shoppingList.getId());
        String oldVersion = enrichedShoppingList.getVersion();
        enrichedShoppingList = shoppingListDAO.setBought(JOHN, shoppingList.getId(), item);
        String newVersion = enrichedShoppingList.getVersion();
        assertThat(newVersion).isNotEqualTo(oldVersion);
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        item = enrichedShoppingList.getItems().get(0);
        assertThat(item.getBoughtBy()).isEqualTo(JOHN.getId());
        assertThat(item.getStateChangedBy()).isEqualTo(JOHN.getId());
    }

    @Test
    public void testSetItemToUnboughtAsOwner() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        EnrichedShoppingList enrichedShoppingList = shoppingListDAO.addShoppingListItem(
                JACK,
                shoppingList.getId(),
                new ShoppingListItem("", "", "Bananen", "", "", "", "")
        );
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        ShoppingListItem item = enrichedShoppingList.getItems().get(0);
        enrichedShoppingList = shoppingListDAO.setBought(JACK, shoppingList.getId(), item);
        item = enrichedShoppingList.getItems().get(0);
        String oldVersion = enrichedShoppingList.getVersion();
        enrichedShoppingList = shoppingListDAO.setUnbought(JACK, shoppingList.getId(), item);
        String newVersion = enrichedShoppingList.getVersion();
        assertThat(newVersion).isNotEqualTo(oldVersion);
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        item = enrichedShoppingList.getItems().get(0);
        assertThat(item.getBoughtBy()).isNull();
        assertThat(item.getStateChangedBy()).isEqualTo(JACK.getId());
    }

    @Test
    public void testSetItemToUnboughtAsNormalMember() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        EnrichedShoppingList enrichedShoppingList = shoppingListDAO.addShoppingListItem(
                JACK,
                shoppingList.getId(),
                new ShoppingListItem("", "", "Bananen", "", "", "", "")
        );
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        ShoppingListItem item = enrichedShoppingList.getItems().get(0);
        enrichedShoppingList = shoppingListDAO.setBought(JACK, shoppingList.getId(), item);
        item = enrichedShoppingList.getItems().get(0);
        shoppingListDAO.invite(JACK, JOHN, shoppingList.getId());
        shoppingListDAO.acceptInvitation(JOHN, shoppingList.getId());
        String oldVersion = enrichedShoppingList.getVersion();
        enrichedShoppingList = shoppingListDAO.setUnbought(JOHN, shoppingList.getId(), item);
        String newVersion = enrichedShoppingList.getVersion();
        assertThat(newVersion).isNotEqualTo(oldVersion);
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        item = enrichedShoppingList.getItems().get(0);
        assertThat(item.getBoughtBy()).isNull();
        assertThat(item.getStateChangedBy()).isEqualTo(JOHN.getId());
    }

    @Test
    public void testTrySetItemToBoughtAsOwnerWhenItIsBoughtAlready() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        EnrichedShoppingList enrichedShoppingList = shoppingListDAO.addShoppingListItem(
                JACK,
                shoppingList.getId(),
                new ShoppingListItem("", "", "Bananen", "", "", "", "")
        );
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        final ShoppingListItem item = enrichedShoppingList.getItems().get(0);
        enrichedShoppingList = shoppingListDAO.setBought(JACK, shoppingList.getId(), item);
        String oldVersion = enrichedShoppingList.getVersion();
        assertThatThrownBy(() -> {
                    shoppingListDAO.setBought(JACK, shoppingList.getId(), item);
        }).isInstanceOf(ApplicationException.class).hasMessage("Cannot set ShoppingListItem to state bought.");
        enrichedShoppingList = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId());
        String newVersion = enrichedShoppingList.getVersion();
        assertThat(newVersion).isEqualTo(oldVersion);
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        ShoppingListItem newItem = enrichedShoppingList.getItems().get(0);
        assertThat(newItem.getBoughtBy()).isEqualTo(JACK.getId());
        assertThat(newItem.getStateChangedBy()).isEqualTo(JACK.getId());
    }

    @Test
    public void testTrySetItemToBoughtAsNormalMemberWhenItIsBoughtAlready() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        EnrichedShoppingList enrichedShoppingList = shoppingListDAO.addShoppingListItem(
                JACK,
                shoppingList.getId(),
                new ShoppingListItem("", "", "Bananen", "", "", "", "")
        );
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        final ShoppingListItem item = enrichedShoppingList.getItems().get(0);
        shoppingListDAO.invite(JACK, JOHN, shoppingList.getId());
        shoppingListDAO.acceptInvitation(JOHN, shoppingList.getId());
        enrichedShoppingList = shoppingListDAO.setBought(JACK, shoppingList.getId(), item);
        String oldVersion = enrichedShoppingList.getVersion();
        assertThatThrownBy(() -> {
            shoppingListDAO.setBought(JOHN, shoppingList.getId(), item);
        }).isInstanceOf(ApplicationException.class).hasMessage("Cannot set ShoppingListItem to state bought.");
        enrichedShoppingList = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId());
        String newVersion = enrichedShoppingList.getVersion();
        assertThat(newVersion).isEqualTo(oldVersion);
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        ShoppingListItem newItem = enrichedShoppingList.getItems().get(0);
        assertThat(newItem.getBoughtBy()).isEqualTo(JACK.getId());
        assertThat(newItem.getStateChangedBy()).isEqualTo(JACK.getId());
    }

    @Test
    public void testTrySetItemToUnboughtAsOwnerWhenItIsNotBoughtAlready() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        EnrichedShoppingList enrichedShoppingList = shoppingListDAO.addShoppingListItem(
                JACK,
                shoppingList.getId(),
                new ShoppingListItem("", "", "Bananen", "", "", "", "")
        );
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        final ShoppingListItem item = enrichedShoppingList.getItems().get(0);
        String oldVersion = enrichedShoppingList.getVersion();
        assertThatThrownBy(() -> {
            shoppingListDAO.setUnbought(JACK, shoppingList.getId(), item);
        }).isInstanceOf(ApplicationException.class).hasMessage("Cannot set ShoppingListItem to state unbought.");
        enrichedShoppingList = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId());
        String newVersion = enrichedShoppingList.getVersion();
        assertThat(newVersion).isEqualTo(oldVersion);
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        ShoppingListItem newItem = enrichedShoppingList.getItems().get(0);
        assertThat(newItem.getBoughtBy()).isNull();
        assertThat(newItem.getStateChangedBy()).isEqualTo(JACK.getId());
    }

    @Test
    public void testTrySetItemToUnboughtAsNormalMemberWhenItIsNotBoughtAlready() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        EnrichedShoppingList enrichedShoppingList = shoppingListDAO.addShoppingListItem(
                JACK,
                shoppingList.getId(),
                new ShoppingListItem("", "", "Bananen", "", "", "", "")
        );
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        final ShoppingListItem item = enrichedShoppingList.getItems().get(0);
        shoppingListDAO.invite(JACK, JOHN, shoppingList.getId());
        shoppingListDAO.acceptInvitation(JOHN, shoppingList.getId());
        enrichedShoppingList = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId());
        String oldVersion = enrichedShoppingList.getVersion();
        assertThatThrownBy(() -> {
            shoppingListDAO.setUnbought(JOHN, shoppingList.getId(), item);
        }).isInstanceOf(ApplicationException.class).hasMessage("Cannot set ShoppingListItem to state unbought.");
        enrichedShoppingList = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId());
        String newVersion = enrichedShoppingList.getVersion();
        assertThat(newVersion).isEqualTo(oldVersion);
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        ShoppingListItem newItem = enrichedShoppingList.getItems().get(0);
        assertThat(newItem.getBoughtBy()).isNull();
        assertThat(newItem.getStateChangedBy()).isEqualTo(JACK.getId());
    }

    @Test
    public void testTrySetItemToBoughtWithoutAuthorization() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        EnrichedShoppingList enrichedShoppingList = shoppingListDAO.addShoppingListItem(
                JACK,
                shoppingList.getId(),
                new ShoppingListItem("", "", "Bananen", "", "", "", "")
        );
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        final ShoppingListItem item = enrichedShoppingList.getItems().get(0);
        String oldVersion = enrichedShoppingList.getVersion();
        assertThatThrownBy(() -> {
            shoppingListDAO.setBought(JOHN, shoppingList.getId(), item);
        }).isInstanceOf(ApplicationException.class).hasMessage("ShoppingList not found.");
        enrichedShoppingList = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId());
        String newVersion = enrichedShoppingList.getVersion();
        assertThat(newVersion).isEqualTo(oldVersion);
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        ShoppingListItem newItem = enrichedShoppingList.getItems().get(0);
        assertThat(newItem.getBoughtBy()).isNull();
        assertThat(newItem.getStateChangedBy()).isEqualTo(JACK.getId());
    }

    @Test
    public void testTrySetItemToUnboughtWithoutAuthorization() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList("", "", "Jack's shopping list", ""));
        EnrichedShoppingList enrichedShoppingList = shoppingListDAO.addShoppingListItem(
                JACK,
                shoppingList.getId(),
                new ShoppingListItem("", "", "Bananen", "", "", "", "")
        );
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        final ShoppingListItem item = enrichedShoppingList.getItems().get(0);
        enrichedShoppingList = shoppingListDAO.setBought(JACK, shoppingList.getId(), item);
        String oldVersion = enrichedShoppingList.getVersion();
        assertThatThrownBy(() -> {
            shoppingListDAO.setBought(JOHN, shoppingList.getId(), item);
        }).isInstanceOf(ApplicationException.class).hasMessage("ShoppingList not found.");
        enrichedShoppingList = shoppingListDAO.getEnrichedShoppingList(JACK, shoppingList.getId());
        String newVersion = enrichedShoppingList.getVersion();
        assertThat(newVersion).isEqualTo(oldVersion);
        assertThat(enrichedShoppingList.getItems()).hasSize(1);
        ShoppingListItem newItem = enrichedShoppingList.getItems().get(0);
        assertThat(newItem.getBoughtBy()).isEqualTo(JACK.getId());
        assertThat(newItem.getStateChangedBy()).isEqualTo(JACK.getId());
    }


}
