package org.usth.ict.ulake.textr.controllers;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.usth.ict.ulake.common.model.dashboard.FileFormModel;
import org.usth.ict.ulake.common.model.folder.FileModel;
import org.usth.ict.ulake.textr.models.IndexingStatus;
import org.usth.ict.ulake.textr.models.payloads.responses.DocumentResponse;
import org.usth.ict.ulake.textr.services.IndexFilesService;
import org.usth.ict.ulake.textr.services.ServiceResponseBuilder;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.util.List;

@Path("/indices")
@RolesAllowed({"User", "Admin"})
@ApplicationScoped
public class IndexFilesController {
    
    @Inject
    IndexFilesService service;
    
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "upload and schedule a file")
    public Uni<Response> upload(@HeaderParam("Authorization") String bearer,
                               @MultipartForm FileFormModel fileFormModel) {
        Uni<ServiceResponseBuilder<?>> builder = service.upload(bearer, fileFormModel);
        
        return builder.onItem().transform(ServiceResponseBuilder::build);
    }
    
    @GET
    @Path("/scheduled")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "get all scheduled files of user")
    public Response scheduled(@DefaultValue("0") @QueryParam("page") int page,
                              @DefaultValue("50") @QueryParam("size") int size) {
        IndexingStatus[] statuses = {IndexingStatus.STATUS_FAILED, IndexingStatus.STATUS_SCHEDULED};
        ServiceResponseBuilder<List<FileModel>> builder = service.listAllByStatuses(List.of(statuses),
                                                                                    page, size);
        
        return builder.build();
    }
    
    @GET
    @Path("/indexed")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "get all indexed files of user")
    public Response indexed(@DefaultValue("0") @QueryParam("page") int page,
                            @DefaultValue("50") @QueryParam("size") int size) {
        ServiceResponseBuilder<List<FileModel>> builder = service.listAllByStatus(IndexingStatus.STATUS_INDEXED,
                                                                                  page, size);
        
        return builder.build();
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "list all indexed and scheduled files of user")
    public Response get(@DefaultValue("0") @QueryParam("page") int page,
                        @DefaultValue("50") @QueryParam("size") int size) {
        IndexingStatus[] statuses = {IndexingStatus.STATUS_INDEXED, IndexingStatus.STATUS_SCHEDULED};
        ServiceResponseBuilder<List<FileModel>> builder = service.listAllByStatuses(List.of(statuses), page, size);
        
        return builder.build();
    }
    
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "delete a file of user by file id")
    public Response delete(@HeaderParam("Authorization") String bearer, @PathParam("id") Long fid) {
        ServiceResponseBuilder<?> builder = service.delete(bearer, fid);
        
        return builder.build();
    }
    
    @POST
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "re-index a file of user by file id")
    public Response reindex(@PathParam("id") Long fid) {
        ServiceResponseBuilder<?> builder = service.reindex(fid);
        
        return builder.build();
    }
    
    @GET
    @Path("/{term}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "search for files")
    public Response search(@PathParam("term") String term) {
        ServiceResponseBuilder<List<DocumentResponse>> builder = service.search(term);
        
        return builder.build();
    }
    
    @GET
    @Path("/{fid}/data")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "download file by file id")
    public Uni<Response> download(@PathParam("fid") Long fid) {
        ServiceResponseBuilder<StreamingOutput> builder = service.download(fid);
        
        if (builder.getStatus() != 200)
            return builder.buildUni();
        
        return Uni.createFrom().item(Response.ok(builder.getResp())
                                             .header("Content-Disposition", "attachment")
                                             .build());
    }
}
