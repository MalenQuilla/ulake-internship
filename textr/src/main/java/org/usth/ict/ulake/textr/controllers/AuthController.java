package org.usth.ict.ulake.textr.controllers;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.usth.ict.ulake.common.model.user.AuthModel;
import org.usth.ict.ulake.textr.services.ServiceResponseBuilder;
import org.usth.ict.ulake.textr.services.AuthService;

import javax.annotation.security.PermitAll;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/auth")
@PermitAll
@ApplicationScoped
public class AuthController {
    
    @Inject
    AuthService service;
    
    @POST
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "login to textr")
    public Response login(AuthModel authModel) {
        ServiceResponseBuilder<?> serviceResponseBuilder = service.login(authModel);
        
        return serviceResponseBuilder.build();
    }
}
