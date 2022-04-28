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
    public void testGetInvitations() {
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
        List<User> actual1 = shoppingListDAO.getInvitations("id-1");
        List<User> actual2 = shoppingListDAO.getInvitations("id-2");
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
        List<User> actual = shoppingListDAO.invite(JOHN, "id-1");
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
        actual = shoppingListDAO.getInvitations("id-1");
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
        expected = List.of(JOHN, JOE);
        actual = shoppingListDAO.invite(JOE, "id-1");
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
        actual = shoppingListDAO.getInvitations("id-1");
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testTryToInviteTwice() {
        dslContext.insertInto(table("shopping_list"))
                .columns(field("id"), field("version"), field("name"), field("owner"))
                .values("id-1", "version-1", "list-name-1", "id-jack")
                .execute();
        shoppingListDAO.invite(JOHN, "id-1");
        assertThatThrownBy(() -> {
            shoppingListDAO.invite(JOHN, "id-1");
        }).isInstanceOf(ApplicationException.class).hasMessage("Cannot invite user to ShoppingList.");
    }

    @Test
    public void testWithdrawInvitation() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList(null, null, "Jack's shopping list", null));
        shoppingListDAO.invite(JOHN, shoppingList.getId());
        shoppingListDAO.invite(JOE, shoppingList.getId());
        List<User> expected = List.of(JOHN, JOE);
        List<User> actual = shoppingListDAO.getInvitations(shoppingList.getId());
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
        shoppingListDAO.withdrawInvitation(JOHN, shoppingList.getId());
        expected = List.of(JOE);
        actual = shoppingListDAO.getInvitations(shoppingList.getId());
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
        shoppingListDAO.withdrawInvitation(JOE, shoppingList.getId());
        expected = List.of();
        actual = shoppingListDAO.getInvitations(shoppingList.getId());
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testTryToWithdrawInvitationThatDoesNotExist() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList(null, null, "Jack's shopping list", null));
        assertThatThrownBy(() -> {
            shoppingListDAO.withdrawInvitation(JOHN, shoppingList.getId());
        }).isInstanceOf(ApplicationException.class).hasMessage("Cannot withdraw invitation because it was not found.");
    }

    @Test
    public void testTryToInviteTheOwner() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList(null, null, "Jack's shopping list", null));
        assertThatThrownBy(() -> {
            shoppingListDAO.invite(JACK, shoppingList.getId());
        }).isInstanceOf(ApplicationException.class).hasMessage("Cannot invite user to ShoppingList.");
    }

    @Test
    public void testAcceptInvitation() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList(null, null, "Jack's shopping list", null));
        shoppingListDAO.invite(JOHN, shoppingList.getId());
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
        assertThatThrownBy(() -> {
            shoppingListDAO.acceptInvitation(JOHN, shoppingList.getId());
        }).isInstanceOf(ApplicationException.class).hasMessage("Invitation not found.");
    }

    @Test
    public void testRejectInvitation() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList(null, null, "Jack's shopping list", null));
        shoppingListDAO.invite(JOHN, shoppingList.getId());
        shoppingListDAO.invite(JOE, shoppingList.getId());
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
        shoppingListDAO.invite(JOHN, shoppingList.getId());
        shoppingListDAO.invite(JOE, shoppingList.getId());
        assertThatThrownBy(() -> {
            shoppingListDAO.rejectInvitation(JACK, shoppingList.getId());
        }).isInstanceOf(ApplicationException.class).hasMessage("Cannot reject invitation.");
    }

    @Test
    public void testGetShoppingListsAfterAcceptingInvitation() {
        ShoppingList shoppingList1 = shoppingListDAO.addShoppingList(JACK, new ShoppingList(null, null, "Jack's shopping list", null));
        ShoppingList shoppingList2 = shoppingListDAO.addShoppingList(JACK, new ShoppingList(null, null, "Jack's other shopping list", null));
        shoppingListDAO.invite(JOHN, shoppingList1.getId());
        shoppingListDAO.invite(JOHN, shoppingList2.getId());
        shoppingListDAO.acceptInvitation(JOHN, shoppingList1.getId());
        shoppingListDAO.acceptInvitation(JOHN, shoppingList2.getId());
        List<ShoppingList> expected = List.of(shoppingList1, shoppingList2);
        List<ShoppingList> actual = shoppingListDAO.getShoppingLists(JOHN);
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testGetShoppingListsAfterAcceptingOneInvitationAndRejectingTheOtherOne() {
        ShoppingList shoppingList1 = shoppingListDAO.addShoppingList(JACK, new ShoppingList(null, null, "Jack's shopping list", null));
        ShoppingList shoppingList2 = shoppingListDAO.addShoppingList(JACK, new ShoppingList(null, null, "Jack's other shopping list", null));
        shoppingListDAO.invite(JOHN, shoppingList1.getId());
        shoppingListDAO.invite(JOHN, shoppingList2.getId());
        shoppingListDAO.acceptInvitation(JOHN, shoppingList1.getId());
        shoppingListDAO.rejectInvitation(JOHN, shoppingList2.getId());
        List<ShoppingList> expected = List.of(shoppingList1);
        List<ShoppingList> actual = shoppingListDAO.getShoppingLists(JOHN);
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testGetShoppingListAfterAcceptingInvitation() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList(null, null, "Jack's shopping list", null));
        shoppingListDAO.invite(JOHN, shoppingList.getId());
        shoppingListDAO.acceptInvitation(JOHN, shoppingList.getId());
        ShoppingList actual = shoppingListDAO.getShoppingList(JOHN, shoppingList.getId());
        assertThat(actual).isEqualTo(shoppingList);
    }

    @Test
    public void testTryToGetShoppingListWithoutAcceptingInvitation() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList(null, null, "Jack's shopping list", null));
        shoppingListDAO.invite(JOHN, shoppingList.getId());
        assertThatThrownBy(() -> {
            shoppingListDAO.getShoppingList(JOHN, shoppingList.getId());
        }).isInstanceOf(ApplicationException.class).hasMessage("ShoppingList not found.");
    }

    @Test
    public void testTryToDeleteShoppingListAfterAcceptingInvitation() {
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(JACK, new ShoppingList(null, null, "Jack's shopping list", null));
        shoppingListDAO.invite(JOHN, shoppingList.getId());
        shoppingListDAO.acceptInvitation(JOHN, shoppingList.getId());
        assertThatThrownBy(() -> {
            shoppingListDAO.deleteShoppingList(JOHN, shoppingList.getId());
        }).isInstanceOf(ApplicationException.class).hasMessage("Cannot delete ShoppingList.");
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
        shoppingListDAO.invite(Jim, shoppingList.getId());
        userDAO.deleteUser(Jim.getId(), UserDAOTest.makeAuth(Jim.getId(), "jims-password"));
        List<User> invitedUsers = shoppingListDAO.getInvitations(shoppingList.getId());
        assertThat(invitedUsers).hasSize(0);
    }

    @Test
    public void testDeleteUserAccountWhenInvitedOthersToAShoppingList() {
        User Jim = userDAO.addUser(new User("", "", "Jim", "jims-password"));
        ShoppingList shoppingList = shoppingListDAO.addShoppingList(Jim, new ShoppingList("", "", "Jim's shopping list", ""));
        shoppingListDAO.invite(JOHN, shoppingList.getId());
        userDAO.deleteUser(Jim.getId(), UserDAOTest.makeAuth(Jim.getId(), "jims-password"));
        List<User> invitedUsers = shoppingListDAO.getInvitations(shoppingList.getId());
        assertThat(invitedUsers).hasSize(0);
    }

}
