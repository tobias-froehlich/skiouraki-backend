package org.example;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.jooq.impl.DSL.table;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestWithDB {

    protected DSLContext dslContext;

    @BeforeAll
    public void beforeAll() {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:h2:memFS:test;DATABASE_TO_LOWER=TRUE;", "sa", "");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        dslContext = DSL.using(connection, SQLDialect.POSTGRES);
        Migrator migrator = new Migrator(dslContext);
        migrator.migrate();
    }

    @BeforeEach
    public void beforeEach() {
        dslContext.deleteFrom(table("shopping_list")).execute();
        dslContext.deleteFrom(table("user_account")).execute();
    }
}
