package org.example;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.exception.DataAccessException;

import javax.xml.crypto.Data;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.selectFrom;
import static org.jooq.impl.DSL.table;

public class ShoppingListDAO {
    private final DSLContext dslContext;

    public ShoppingListDAO(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    public ShoppingList getShoppingList(String id) {
        List<ShoppingList> shoppingLists = dslContext.selectFrom("shopping_list")
                .where(field("id").eq(id))
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

    public ShoppingList addShoppingList(User authenticatedUser, ShoppingList shoppingList) {
        String id = UUID.randomUUID().toString();
        String version = UUID.randomUUID().toString();
        String owner = authenticatedUser.getId();
        if (!ShoppingList.isNameValid(shoppingList.getName())) {
            throw new ApplicationException("Invalid name.");
        }
        try {
            dslContext.insertInto(table("shopping_list"))
                    .columns(field("id"), field("version"), field("name"), field("owner"))
                    .values(id, version, shoppingList.getName(), owner)
                    .execute();
        } catch (DataAccessException e) {
            throw new ApplicationException("Cannot create ShoppingList.");
        }
        return getShoppingList(id);
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
        return getShoppingList(id);
    }

    public ShoppingList deleteShoppingList(User authenticatedUser, String id) {
        ShoppingList shoppingList = getShoppingList(id);
        try {
            int count = dslContext.deleteFrom(table("shopping_list"))
                    .where(field("id").eq(id))
                    .and(field("owner").eq(authenticatedUser.getId()))
                    .execute();
            if (count == 0) {
                throw new ApplicationException("Cannot delete ShoppingList.");
            }
        } catch (DataAccessException e) {
            throw new ApplicationException("Cannot delete ShoppingList.");
        }
        return shoppingList;
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
