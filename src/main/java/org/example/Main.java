package org.example;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashSet;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws Exception {

        Connection connection = null;
        if (System.getenv("LOCAL_DB_URL") != null) {
            String dbUrl = System.getenv("LOCAL_DB_URL");
            String username = System.getenv("LOCAL_DB_USERNAME");
            String password = System.getenv("LOCAL_DB_PASSWORD");
            connection = DriverManager.getConnection(dbUrl, username, password);
        }
        else {
            URI dbUri = new URI(System.getenv("DATABASE_URL"));
            String[] usernamePassword = dbUri.getUserInfo().split(":");
            String username = usernamePassword[0];
            String password = usernamePassword[1];
            System.out.println("username: " + username);
            System.out.println("password: " + password);
            String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ":" + dbUri.getPort() + dbUri.getPath();
            System.out.println(dbUrl);
            connection = DriverManager.getConnection(dbUrl, username, password);
            System.out.println("Connected to database.");
        }

        DSLContext dslContext = DSL.using(connection, SQLDialect.POSTGRES);
//        dslContext.settings().setRenderQuotedNames(RenderQuotedNames.ALWAYS);
//        dslContext.settings().setRenderNameCase(RenderNameCase.LOWER);
        Migrator migrator = new Migrator(dslContext);
        migrator.reset();
        migrator.migrate();

        Server server = new Server(Integer.parseInt(System.getenv("PORT")));

        ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        handler.setContextPath("/");

        final UserDAO userDAO = new UserDAO(dslContext);
        final UserResource userResource = new UserResource(userDAO, migrator);

        ResourceConfig resourceConfig = new ResourceConfig();
        Set<Object> instances = new HashSet<>();
        instances.add(userResource);
        resourceConfig.registerInstances(instances);
        resourceConfig.register(new ApplicationExceptionMapper());
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(new ContainerResponseFilter() {
                                    @Override
                                    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
                                        responseContext.getHeaders().add("Access-Control-Allow-origin", "*");
                                        responseContext.getHeaders().add("Access-Control-Allow-headers",
                                                "Origin, content-type, accept, authorization");
                                        responseContext.getHeaders().add("Access-Control-Allow-Methods",
                                                "GET, POST, PUT, DELETE, OPTIONS, HEAD");
                                    }
                                });

                handler.addServlet(new ServletHolder(new ServletContainer(resourceConfig)), "/*");

        server.setHandler(handler);
        server.start();
        server.join();
    }
}
