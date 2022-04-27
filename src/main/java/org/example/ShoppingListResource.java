package org.example;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("shopping-list")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ShoppingListResource {
    private final UserDAO userDAO;
    private final ShoppingListDAO shoppingListDAO;

    public ShoppingListResource(UserDAO userDAO, ShoppingListDAO shoppingListDAO) {
        this.userDAO = userDAO;
        this.shoppingListDAO = shoppingListDAO;
    }

    @GET
    @Path("get/{id}")
    public ShoppingList getShoppingList(@PathParam("id") String id, @HeaderParam("Authorization") String auth) {
        User authenticatedUser = userDAO.getUserByAuth(auth);
        ShoppingList shoppingList = shoppingListDAO.getShoppingList(id);
        if (!shoppingList.getOwner().equals(authenticatedUser.getId())) {
            throw new ApplicationException("Not authorized.");
        }
        return shoppingList;
    }

    @GET
    @Path("get-own")
    public List<ShoppingList> getOwnShoppingLists(@HeaderParam("Authorization") String auth) {
        User authenticatedUser = userDAO.getUserByAuth(auth);
        return shoppingListDAO.getOwnShoppingLists(authenticatedUser);
    }

    @POST
    @Path("add")
    public ShoppingList addShoppingList(ShoppingList shoppingList, @HeaderParam("Authorization") String auth) {
        User authenticatedUser = userDAO.getUserByAuth(auth);
        return shoppingListDAO.addShoppingList(authenticatedUser, shoppingList);
    }

    @POST
    @Path("rename")
    public ShoppingList renameShoppingList(ShoppingList shoppingList, @HeaderParam("Authorization") String auth) {
        User authenticatedUser = userDAO.getUserByAuth(auth);
        return shoppingListDAO.renameShoppingList(authenticatedUser, shoppingList.getId(), shoppingList.getName());
    }

    @DELETE
    @Path("delete/{id}")
    public ShoppingList deleteShoppingList(@PathParam("id") String id, @HeaderParam("Authorization") String auth) {
        User authenticatedUser = userDAO.getUserByAuth(auth);
        return shoppingListDAO.deleteShoppingList(authenticatedUser, id);
    }


}
