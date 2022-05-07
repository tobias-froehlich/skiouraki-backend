package org.example;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.impl.QOM;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;
import static org.jooq.impl.SQLDataType.BOOLEAN;
import static org.jooq.impl.SQLDataType.INTEGER;
import static org.jooq.impl.SQLDataType.VARCHAR;

public class Migrator {
    private final DSLContext dslContext;
    private final List<MigrationStep> migrationSteps;
    private boolean migrationStopped;

    public Migrator(DSLContext dslContext) {
        this.migrationStopped = false;
        this.dslContext = dslContext;
        migrationSteps = new ArrayList<>();
        migrationSteps.add(new MigrationStep("Creating user table.", cxt -> {
            cxt.createTableIfNotExists("user_account")
                    .column("id", VARCHAR(36))
                    .column("version", VARCHAR(36))
                    .column("name", VARCHAR(16))
                    .column("normalized_name", VARCHAR(32))
                    .column("hashed_password", VARCHAR(44))
                    .column("salt", VARCHAR(24))
                    .execute();
        }));
        migrationSteps.add(new MigrationStep("Adding Primary key to user table.", ctx -> {
            ctx.alterTable("user_account")
                    .alterColumn("id")
                    .setNotNull()
                    .execute();
            ctx.alterTable("user_account")
                    .add(
                            DSL.constraint("pk_user").primaryKey("id")
                    ).execute();
        }));
        migrationSteps.add(new MigrationStep("Adding Unique constraint to user name.", ctx -> {
            ctx.alterTable("user_account")
                    .add(
                            DSL.constraint("unique_constraint_user_name").unique("name")
                    ).execute();
            ctx.alterTable("user_account")
                    .add(
                            DSL.constraint("unique_normalized_name").unique("normalized_name")
                    ).execute();
        }));
        migrationSteps.add(new MigrationStep("Adding shopping list table.", ctx -> {
            ctx.createTableIfNotExists("shopping_list")
                    .column("id", VARCHAR(36))
                    .column("version", VARCHAR(36))
                    .column("name", VARCHAR(32))
                    .column("owner", VARCHAR(36))
                    .execute();
        }));
        migrationSteps.add(new MigrationStep("Adding primary key to shopping list table.", ctx -> {
            ctx.alterTable("shopping_list")
                    .alterColumn("id")
                    .setNotNull()
                    .execute();
            ctx.alterTable("shopping_list")
                    .add(
                            DSL.constraint("pk_shopping_list").primaryKey("id")
                    ).execute();
        }));
        migrationSteps.add(new MigrationStep("Adding foreign key to shoppingList owner.", ctx -> {
            ctx.alterTable("shopping_list")
                    .add(
                            DSL.constraint("fk_shopping_list_owner").foreignKey("owner").references("user_account", "id")
                    )
                    .execute();
        }));
        migrationSteps.add(new MigrationStep("Shopping list authorization N to M table.", ctx -> {
            ctx.createTableIfNotExists("shopping_list_authorization")
                    .column("shopping_list_id", VARCHAR(36))
                    .column("user_id", VARCHAR(36))
                    .column("invitation_accepted", BOOLEAN)
                    .execute();
            ctx.alterTable("shopping_list_authorization")
                    .add(
                            DSL.constraint("fk_shopping_list_authorization_shopping_list").foreignKey("shopping_list_id").references("shopping_list", "id")
                    )
                    .execute();
            ctx.alterTable("shopping_list_authorization")
                    .add(
                            DSL.constraint("fk_shopping_list_authorization_user").foreignKey("user_id").references("user_account", "id")
                    )
                    .execute();
            ctx.alterTable("shopping_list_authorization")
                    .add(
                            DSL.constraint("unique_shopping_list_authorization").unique("shopping_list_id", "user_id")
                    )
                    .execute();
        }));
        migrationSteps.add(new MigrationStep("Shopping list item table", ctx -> {
            ctx.createTableIfNotExists("shopping_list_item")
                    .column("id", VARCHAR(36))
                    .column("version", VARCHAR(36))
                    .column("name", VARCHAR(32))
                    .column("created_by", VARCHAR(36))
                    .column("modified_by", VARCHAR(36))
                    .column("bought_by", VARCHAR(36))
                    .column("state_changed_by", VARCHAR(36))
                    .column("shopping_list_id", VARCHAR(36))
                    .column("sort_order", INTEGER)
                    .execute();
            ctx.alterTable("shopping_list_item")
                    .alterColumn("id")
                    .setNotNull()
                    .execute();
            ctx.alterTable("shopping_list_item")
                    .add(
                            DSL.constraint("pk_shopping_list_item").primaryKey("id")
                    )
                    .execute();
            ctx.alterTable("shopping_list_item")
                    .add(
                            DSL.constraint("fk_shopping_list_item_shopping_list").foreignKey("shopping_list_id").references("shopping_list", "id")
                    )
                    .execute();
        }));
//        migrationSteps.add(new MigrationStep("Shopping list item 1:N table", ctx -> {
//            ctx.createTableIfNotExists("shopping_list_shopping_list_item")
//                    .column("shopping_list_id", VARCHAR(36))
//                    .column("item_id", VARCHAR(36))
//                    .column("sort_order", INTEGER)
//                    .execute();
//            ctx.alterTable("shopping_list_shopping_list_item")
//                    .add(
//                            DSL.constraint("fk_shopping_list_shopping_list_item_shopping_list").foreignKey("shopping_list_id").references("shopping_list", "id")
//                    )
//                    .execute();
//            ctx.alterTable("shopping_list_shopping_list_item")
//                    .add(
//                            DSL.constraint("fk_shopping_list_shopping_list_item_item").foreignKey("item_id").references("shopping_list_item", "id")
//                    )
//                    .execute();
//        }));
    }

    public void reset() {
        dslContext.dropTableIfExists("migration").execute();
        dslContext.dropTableIfExists("shopping_list_shopping_list_item").execute();
        dslContext.dropTableIfExists("shopping_list_item").execute();
        dslContext.dropTableIfExists("shopping_list_authorization").execute();
        dslContext.dropTableIfExists("shopping_list").execute();
        dslContext.dropTableIfExists("user_account").execute();
    }

    public void migrate() {
        dslContext.createTableIfNotExists("migration")
                .column("number", INTEGER)
                .execute();
        for (int i = 0; i < migrationSteps.size(); i++) {
            Integer count = dslContext
                    .selectCount()
                    .from("migration")
                    .where(field("number").eq(i))
                    .fetchOne(0, Integer.class);
            boolean carriedOut = count != null && count > 0;
            if (carriedOut) {
                System.out.println("Migration #" + i + " (" + migrationSteps.get(i).getDescription() + ") was carried out already. Skip it.");
            } else {
                System.out.println("Migration #" + i + " (" + migrationSteps.get(i).getDescription() + ") was not carried out. Perform migration.");
                try {
                    migrationSteps.get(i).getCallback().performStep(dslContext);
                } catch (DataAccessException e) {
                    e.printStackTrace();
                    migrationStopped = true;
                    System.out.println("Migration stopped because step #" + i + " failed.");
                }

            }
            if (migrationStopped) {
                break;
            }
            if (!carriedOut) {
                dslContext.insertInto(table("migration"), field("number"))
                        .values(i)
                        .execute();
            }
        }
    }

    private static class MigrationStep {
        private final String description;
        private final MigrationStepCallback callback;

        public MigrationStep(String description, MigrationStepCallback callback) {
            this.description = description;
            this.callback = callback;
        }

        public String getDescription() {
            return description;
        }

        public MigrationStepCallback getCallback() {
            return callback;
        }
    }

    private interface MigrationStepCallback {
        void performStep(DSLContext dslContext) throws DataAccessException;
    }
}
