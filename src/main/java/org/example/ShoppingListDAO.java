package org.example;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

public class ShoppingListDAO {
    private final DSLContext dslContext;

    public ShoppingListDAO(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    public ShoppingList getShoppingList(User authenticatedUser, String id) {
        List<ShoppingList> shoppingLists = dslContext.select().from("shopping_list")
                .join("shopping_list_authorization")
                .on(field("id").eq(field("shopping_list_id")))
                .where(field("id").eq(id))
                .and(field("user_id").eq(authenticatedUser.getId()))
                .and(field("invitation_accepted").eq(true))
                .fetch(new ShoppingListMapper());
        if (shoppingLists.size() == 0) {
            throw new ApplicationException("ShoppingList not found.");
        }
        return shoppingLists.get(0);
    }

    public List<ShoppingList> getOwnShoppingLists(User authenticatedUser) {
        return dslContext.selectFrom("shopping_list")
                .where(field("owner").eq(authenticatedUser.getId()))
                .fetch(new ShoppingListMapper());
    }

    public List<ShoppingList> getShoppingLists(User authenticatedUser) {
        return dslContext.select().from("shopping_list")
                .join("shopping_list_authorization")
                .on(field("id").eq(field("shopping_list_id")))
                .where(field("user_id").eq(authenticatedUser.getId()))
                .and(field("invitation_accepted").eq(true))
                .fetch(new ShoppingListMapper());
    }

    public ShoppingList addShoppingList(User authenticatedUser, ShoppingList shoppingList) {
        String id = UUID.randomUUID().toString();
        String version = UUID.randomUUID().toString();
        String owner = authenticatedUser.getId();
        if (!ShoppingList.isNameValid(shoppingList.getName())) {
            throw new ApplicationException("Invalid name.");
        }
        try {
            dslContext.transaction(configuration -> {
                DSL.using(configuration).insertInto(table("shopping_list"))
                        .columns(field("id"), field("version"), field("name"), field("owner"))
                        .values(id, version, shoppingList.getName(), owner)
                        .execute();
                DSL.using(configuration).insertInto(table("shopping_list_authorization"))
                        .columns(field("shopping_list_id"), field("user_id"), field("invitation_accepted"))
                        .values(id, owner, true)
                        .execute();
            });
        } catch (DataAccessException e) {
            throw new ApplicationException("Cannot create ShoppingList.");
        }
        return getShoppingList(authenticatedUser, id);
    }

    public ShoppingList renameShoppingList(User authenticatedUser, String id, String newName) {
        if (!ShoppingList.isNameValid(newName)) {
            throw new ApplicationException("Invalid name.");
        }
        String newVersion = UUID.randomUUID().toString();
        try {
            int count = dslContext.update(table("shopping_list"))
                    .set(field("version"), newVersion)
                    .set(field("name"), newName)
                    .where(field("id").eq(id))
                    .and(field("owner").eq(authenticatedUser.getId()))
                    .execute();
            if (count == 0) {
                throw new ApplicationException("Cannot rename ShoppingList.");
            }
        } catch (DataAccessException e) {
            throw new ApplicationException("Cannot rename ShoppingList.");
        }
        return getShoppingList(authenticatedUser, id);
    }

    public ShoppingList deleteShoppingList(User authenticatedUser, String id) {
        ShoppingList shoppingList = getShoppingList(authenticatedUser, id);
        try {
            dslContext.transaction(configuration -> {
                DSL.using(configuration).deleteFrom(table("shopping_list_authorization"))
                        .where(field("shopping_list_id").eq(id))
                        .execute();
                int count = DSL.using(configuration).deleteFrom(table("shopping_list"))
                        .where(field("id").eq(id))
                        .and(field("owner").eq(authenticatedUser.getId()))
                        .execute();
                if (count == 0) {
                    throw new ApplicationException("Cannot delete ShoppingList.");
                }
            });
        } catch (DataAccessException e) {
            throw new ApplicationException("Cannot delete ShoppingList.");
        }
        return shoppingList;
    }

    public List<User> getInvitationsByShoppingList(User authenticatedUser, String shoppingListId) {
        return dslContext.select()
                .from("shopping_list_authorization")
                .join("user_account")
                .on(field("user_id").equal(field("user_account.id")))
                .join("shopping_list")
                .on(field("shopping_list_id").equal(field("shopping_list.id")))
                .where(field("shopping_list_id").eq(shoppingListId))
                .and(field("invitation_accepted").eq(false))
                .and(field("owner").eq(authenticatedUser.getId()))
                .fetch(new UserDAO.UserMapper())
                .stream().map(userFromDb -> new User(userFromDb.getId(), userFromDb.getVersion(), userFromDb.getName(), null))
                .collect(Collectors.toList());
    }

    public List<ShoppingList> getInvitationsByUser(User user) {
        return dslContext.select()
                .from("shopping_list")
                .join("shopping_list_authorization")
                .on(field("id").eq(field("shopping_list_id")))
                .where(field("user_id").eq(user.getId()))
                .and(field("invitation_accepted").eq(false))
                .fetch(new ShoppingListMapper());
    }

    public List<User> getMembers(String shoppingListId) {
        return dslContext.select()
                .from("shopping_list_authorization")
                .join("user_account")
                .on(field("user_id").equal(field("id")))
                .where(field("shopping_list_id").eq(shoppingListId))
                .and(field("invitation_accepted").eq(true))
                .fetch(new UserDAO.UserMapper())
                .stream().map(userFromDb -> new User(userFromDb.getId(), userFromDb.getVersion(), userFromDb.getName(), null))
                .collect(Collectors.toList());
    }
    public boolean isUserAuthorizedForShoppingList(User user, String shoppingListId) {
        Integer count = dslContext.selectCount().from(table("shopping_list_authorization"))
                .where(field("shopping_list_id").eq(shoppingListId))
                .and(field("user_id").eq(user.getId()))
                .and(field("invitation_accepted").eq(true))
                .fetchOne(0, Integer.class);
        return (count != null) && (count > 0);
    }

    public List<User> invite(User authenticatedUser, User invitedUser, String shoppingListId) {
        try {
            dslContext.insertInto(table("shopping_list_authorization"))
                    .columns(field("shopping_list_id"), field("user_id"), field("invitation_accepted"))
                    .values(shoppingListId, invitedUser.getId(), false)
                    .execute();

        } catch (DataAccessException e) {
            throw new ApplicationException("Cannot invite user to ShoppingList.");
        }
            dslContext.update(table("shopping_list"))
                .set(field("version"), UUID.randomUUID().toString())
                .execute();
        return getInvitationsByShoppingList(authenticatedUser, shoppingListId);
    }

    public List<User> withdrawInvitation(User authenticatedUser, User user, String shoppingListId) {
        try {
            int count = dslContext.deleteFrom(table("shopping_list_authorization"))
                    .where(field("shopping_list_id").eq(shoppingListId))
                    .and(field("user_id").eq(user.getId()))
                    .and(field("invitation_accepted").eq(false))
                    .execute();
            if (count == 0) {
                throw new ApplicationException("Cannot withdraw invitation because it was not found.");
            }
            dslContext.update(table("shopping_list"))
                    .set(field("version"), UUID.randomUUID().toString())
                    .execute();
        } catch (DataAccessException e) {
            throw new ApplicationException("Cannot withdraw invitation.");
        }
        return getInvitationsByShoppingList(authenticatedUser, shoppingListId);
    }

    public void acceptInvitation(User user, String shoppingListId) {
        try {
            int count = dslContext.update(table("shopping_list_authorization"))
                    .set(field("invitation_accepted"), true)
                    .where(field("shopping_list_id").eq(shoppingListId))
                    .and(field("user_id").eq(user.getId()))
                    .execute();
            if (count == 0) {
                throw new ApplicationException("Invitation not found.");
            }
            dslContext.update(table("shopping_list"))
                    .set(field("version"), UUID.randomUUID().toString())
                    .execute();
        } catch (DataAccessException e) {
            throw new ApplicationException("Cannot accept invitation.");
        }
    }

    public void rejectInvitation(User user, String shoppingListId) {
        try {
            int count = dslContext.delete(table("shopping_list_authorization"))
                    .where(field("shopping_list_id").eq(shoppingListId))
                    .and(field("user_id").eq(user.getId()))
                    .and(field("invitation_accepted").eq(false))
                    .execute();
            if (count == 0) {
                throw new ApplicationException("Cannot reject invitation.");
            }
            dslContext.update(table("shopping_list"))
                    .set(field("version"), UUID.randomUUID().toString())
                    .execute();
        } catch (DataAccessException e) {
            throw new ApplicationException("Cannot reject invitation.");
        }
    }

    public List<User> leaveShoppingList(User authenticatedUser, User userToLeave, String shoppingListId) {
        try {
            int count = 0;
            if (authenticatedUser.getId().equals(userToLeave.getId())) {
                count = dslContext.delete(table("shopping_list_authorization"))
                        .where(field("shopping_list_id").eq(shoppingListId))
                        .and(field("user_id").eq(userToLeave.getId()))
                        .and(field("invitation_accepted").eq(true))
                        .and(field("user_id").notIn(
                                        dslContext.select(field("owner"))
                                                .from("shopping_list")
                                                .where(field("id").eq(shoppingListId))
                                )
                        )
                        .execute();
                if (count == 0) {
                    throw new ApplicationException("Cannot leave ShoppingList.");
                }
                dslContext.update(table("shopping_list"))
                        .set(field("version"), UUID.randomUUID().toString())
                        .execute();
            } else {
                count = dslContext.delete(table("shopping_list_authorization"))
                        .where(field("shopping_list_id").eq(shoppingListId))
                        .and(field("user_id").eq(userToLeave.getId()))
                        .and(field("invitation_accepted").eq(true))
                        .and(field("user_id").notIn(
                                        dslContext.select(field("owner"))
                                                .from("shopping_list")
                                                .where(field("id").eq(shoppingListId))
                                )
                        )
                        .and(field("shopping_list_id").in(
                                        dslContext.select(field("id"))
                                                .from("shopping_List")
                                                .where(field("owner").eq(authenticatedUser.getId()))
                                )
                        )
                        .execute();
            }
            if (count == 0) {
                throw new ApplicationException("Cannot leave ShoppingList.");
            }
            dslContext.update(table("shopping_list"))
                    .set(field("version"), UUID.randomUUID().toString())
                    .execute();
        } catch (DataAccessException e){
                throw new ApplicationException("Cannot leave ShoppingList.");
        }
        return getMembers(shoppingListId);
    }

    public EnrichedShoppingList getEnrichedShoppingList(User authenticatedUser, String shoppingListId) {
        ShoppingList shoppingList = getShoppingList(authenticatedUser, shoppingListId);
        List<User> members = dslContext.select()
                .from("shopping_list_authorization")
                .join("user_account")
                .on(field("shopping_list_authorization.user_id").eq(field("user_account.id")))
                .where(field("shopping_list_id").eq(shoppingListId))
                .and(field("invitation_accepted").eq(true))
                .fetch(new UserDAO.UserMapper())
                .stream().map(userFromDb -> new User(userFromDb.getId(), userFromDb.getVersion(), userFromDb.getName(), null))
                .collect(Collectors.toList());
        List<User> invitedUsers = dslContext.select()
                .from("shopping_list_authorization")
                .join("user_account")
                .on(field("shopping_list_authorization.user_id").eq(field("user_account.id")))
                .where(field("shopping_list_id").eq(shoppingListId))
                .and(field("invitation_accepted").eq(false))
                .fetch(new UserDAO.UserMapper())
                .stream().map(userFromDb -> new User(userFromDb.getId(), userFromDb.getVersion(), userFromDb.getName(), null))
                .collect(Collectors.toList());
        return new EnrichedShoppingList(
                shoppingList.getId(),
                shoppingList.getVersion(),
                shoppingList.getName(),
                shoppingList.getOwner(),
                members,
                invitedUsers
        );
    }

    public static class ShoppingListMapper implements RecordMapper<Record, ShoppingList> {
        @Override
        public ShoppingList map(Record record) {
            return new ShoppingList(
                    record.getValue("id", String.class),
                    record.getValue("version", String.class),
                    record.getValue("name", String.class),
                    record.getValue("owner", String.class)
            );
        }
    }

}
