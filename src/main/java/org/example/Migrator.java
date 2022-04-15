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
                    .column("hashed_password", VARCHAR(64))
                    .column("salt", VARCHAR(64))
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
    }

    public void reset() {
        dslContext.dropTableIfExists("migration").execute();
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
