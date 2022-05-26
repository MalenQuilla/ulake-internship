package org.usth.ict.ulake.common.service;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.usth.ict.ulake.common.model.LakeHttpResponse;
import org.usth.ict.ulake.common.model.core.GroupObjectModel;
import org.usth.ict.ulake.common.model.core.ObjectFormModel;

@Path("/api")
@RegisterRestClient(configKey = "core-api")
@Produces(MediaType.APPLICATION_JSON)
public interface CoreService {
    @GET
    @Path("/object")
    @Schema(description = "list all object of user")
    public LakeHttpResponse objectList(
        @HeaderParam("Authorization") String bearer);

    @GET
    @Path("/object/{cid}")
    @Schema(description = "get object information")
    public LakeHttpResponse objectInfo(
        @PathParam("cid") String cid,
        @HeaderParam("Authorization") String bearer);

    @GET
    @Path("/object/{cid}/data")
    @Schema(description = "load binary object from core")
    public InputStream objectData(
        @PathParam("cid") String cid,
        @HeaderParam("Authorization") String bearer);

    @POST
    @Path("/object")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Schema(description = "upload new object")
    public LakeHttpResponse newObject(
        @HeaderParam("Authorization") String bearer,
        @MultipartForm ObjectFormModel output);

    @GET
    @Path("/group/{id}")
    @Schema(description = "get group info")
    public LakeHttpResponse groupInfo(
        @PathParam("id") Long id,
        @HeaderParam("Authorization") String bearer);

    @POST
    @Path("/group")
    @Schema(description = "create new group")
    public LakeHttpResponse newGroup(
        @RequestBody GroupObjectModel entity,
        @HeaderParam("Authorization") String bearer);

    @PUT
    @Path("/group/{id}")
    @Schema(description = "update group")
    public LakeHttpResponse updateGroup(
        @PathParam("id") Long id,
        @RequestBody GroupObjectModel newEntity,
        @HeaderParam("Authorization") String bearer);

    @DELETE
    @Path("/group/{id}")
    @Schema(description = "delete group")
    public LakeHttpResponse delGroup(
        @PathParam("id") Long id,
        @HeaderParam("Authorization") String bearer);


}