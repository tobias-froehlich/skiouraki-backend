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
        User authenticatedUser = userDAO.authenticate(auth);
        if (!shoppingListDAO.isUserAuthorizedForShoppingList(authenticatedUser, id)) {
            throw new ApplicationException("Not authorized.");
        }
        ShoppingList shoppingList = shoppingListDAO.getShoppingList(authenticatedUser,id);

        return shoppingList;
    }

    @GET
    @Path("get-own")
    public List<ShoppingList> getOwnShoppingLists(@HeaderParam("Authorization") String auth) {
        User authenticatedUser = userDAO.authenticate(auth);
        return shoppingListDAO.getOwnShoppingLists(authenticatedUser);
    }

    @GET
    @Path("get")
    public List<ShoppingList> getShoppingLists(@HeaderParam("Authorization") String auth) {
        User authenticatedUser = userDAO.authenticate(auth);
        return shoppingListDAO.getShoppingLists(authenticatedUser);
    }

    @POST
    @Path("add")
    public ShoppingList addShoppingList(ShoppingList shoppingList, @HeaderParam("Authorization") String auth) {
        User authenticatedUser = userDAO.authenticate(auth);
        return shoppingListDAO.addShoppingList(authenticatedUser, shoppingList);
    }

    @POST
    @Path("rename")
    public ShoppingList renameShoppingList(ShoppingList shoppingList, @HeaderParam("Authorization") String auth) {
        User authenticatedUser = userDAO.authenticate(auth);
        return shoppingListDAO.renameShoppingList(authenticatedUser, shoppingList.getId(), shoppingList.getName());
    }

    @DELETE
    @Path("delete/{id}")
    public ShoppingList deleteShoppingList(@PathParam("id") String id, @HeaderParam("Authorization") String auth) {
        User authenticatedUser = userDAO.authenticate(auth);
        return shoppingListDAO.deleteShoppingList(authenticatedUser, id);
    }

    @POST
    @Path("invite/{shopping-list-id}/{user-id}")
    public List<User> invite(@PathParam("shopping-list-id") String shoppingListId, @PathParam("user-id") String userId, @HeaderParam("Authorization") String auth) {
        User authenticatedUser = userDAO.authenticate(auth);
        ShoppingList shoppingList = shoppingListDAO.getShoppingList(authenticatedUser, shoppingListId);
        if (!shoppingList.getOwner().equals(authenticatedUser.getId())) {
            throw new ApplicationException("Not authorized.");
        }
        User invitedUser = userDAO.getUser(userId);
        return shoppingListDAO.invite(authenticatedUser, invitedUser, shoppingListId);
    }
    
    @POST
    @Path("withdraw-invitation/{shopping-list-id}/{user-id}")
    public List<User> withdrawInvitation(@PathParam("shopping-list-id") String shoppingListId, @PathParam("user-id") String userId, @HeaderParam("Authorization") String auth) {
        User authenticatedUser = userDAO.authenticate(auth);
        ShoppingList shoppingList = shoppingListDAO.getShoppingList(authenticatedUser, shoppingListId);
        if (!shoppingList.getOwner().equals(authenticatedUser.getId())) {
            throw new ApplicationException("Not authorized.");
        }
        User withdrawnUser = userDAO.getUser(userId);
        return shoppingListDAO.withdrawInvitation(authenticatedUser, withdrawnUser, shoppingListId);
    }

    @GET
    @Path("get-invitations-by-shopping-list/{shopping-list-id}")
    public List<User> getInvitationByShoppingList(@PathParam("shopping-list-id") String shoppingListId, @HeaderParam("Authorization") String auth) {
        User authenticatedUser = userDAO.authenticate(auth);
        return shoppingListDAO.getInvitationsByShoppingList(authenticatedUser, shoppingListId);
    }

    @GET
    @Path("get-invitations/")
    public List<ShoppingList> getInvitationByUser(@HeaderParam("Authorization") String auth) {
        User authenticatedUser = userDAO.authenticate(auth);
        return shoppingListDAO.getInvitationsByUser(authenticatedUser);
    }

    @POST
    @Path("accept-invitation/{shopping-list-id}")
    public void acceptInvitation(@PathParam("shopping-list-id") String shoppingListId, @HeaderParam("Authorization") String auth) {
        User authenticatedUser = userDAO.authenticate(auth);
        shoppingListDAO.acceptInvitation(authenticatedUser, shoppingListId);
    }

    @POST
    @Path("reject-invitation/{shopping-list-id}")
    public void rejectInvitation(@PathParam("shopping-list-id") String shoppingListId, @HeaderParam("Authorization") String auth) {
        User authenticatedUser = userDAO.authenticate(auth);
        shoppingListDAO.rejectInvitation(authenticatedUser, shoppingListId);
    }

    @POST
    @Path("leave-shopping-list/{shopping-list-id}")
    public void leaveShoppingList(@PathParam("shopping-list-id") String shoppingListId, @HeaderParam("Authorization") String auth) {
        User authenticatedUser = userDAO.authenticate(auth);
        shoppingListDAO.leaveShoppingList(authenticatedUser, authenticatedUser, shoppingListId);
    }

    @POST
    @Path("remove-user-from-shopping-list/{shopping-list-id}/{user-id}")
    public List<User> removeUserFromShoppingList(@PathParam("shopping-list-id") String shoppingListId, @PathParam("user-id") String userId, @HeaderParam("Authorization") String auth) {
        User authenticatedUser = userDAO.authenticate(auth);
        User user = userDAO.getUser(userId);
        return shoppingListDAO.leaveShoppingList(authenticatedUser, user, shoppingListId);
    }

    @GET
    @Path("get-enriched/{shopping-list-id}")
    public EnrichedShoppingList getEnriched(@PathParam("shopping-list-id") String shoppingListId, @HeaderParam("Authorization") String auth) {
        User authenticatedUser = userDAO.authenticate(auth);
        return shoppingListDAO.getEnrichedShoppingList(authenticatedUser, shoppingListId);
    }

}
