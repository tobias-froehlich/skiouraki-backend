package org.example;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Base64;
import java.util.List;

@Path("user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    private final UserDAO userDAO;
    private final Migrator migrator;

    public UserResource(UserDAO userDAO, Migrator migrator) {
        this.userDAO = userDAO;
        this.migrator = migrator;
    }

    @POST
    @Path("reset")
    public void reset() {
        System.out.println("reset-1");
        migrator.reset();
        System.out.println("reset-2");
        migrator.migrate();
        System.out.println("reset-3");
    }

    @GET
    @Path("get-all")
    public List<User> getAll() {
        System.out.println("app-user/get-all is called!");
        return userDAO.getAllAppUsers();
    }

    @GET
    @Path("get/{id}")
    public User getUser(@PathParam("id") String id, @HeaderParam("Authorization") String auth) throws ApplicationException {
        User user = userDAO.getUser(id, auth);
        return user;
    }

    @GET
    @Path("get-by-name/{name}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getUserIdByName(@PathParam("name") String name) throws ApplicationException {
        System.out.println("get-by-name " + name);
        return userDAO.getUserIdByName(name);
    }

    @POST
    @Path("add")
    public User addUser(User user) {
        System.out.println("post " + user);
        return userDAO.addUser(user);
    }

    @POST
    @Path("update/{id}")
    public User updateUser(@PathParam("id") String id, @HeaderParam("Authorization") String auth, User user) {
        if (!id.equals(user.getId())) {
            throw new ApplicationException("The user cannot be updated because the id in the path is different from the id in the body.");
        }
        return userDAO.updateUser(user, auth);
    }

}
