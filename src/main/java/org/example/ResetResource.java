package org.example;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("reset")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ResetResource {

    private final Migrator migrator;

    public ResetResource(Migrator migrator) {
        this.migrator = migrator;
    }

    @GET
    @Path("is-reset-active")
    public String isResetActive() {
        System.out.println("Is reset active?");
        return "Yes";
    }

    @POST
    @Path("reset")
    public void reset() {
        migrator.reset();
        migrator.migrate();
    }

}
