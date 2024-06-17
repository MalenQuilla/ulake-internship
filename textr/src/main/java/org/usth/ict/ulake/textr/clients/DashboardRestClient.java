package org.usth.ict.ulake.textr.clients;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.usth.ict.ulake.common.model.LakeHttpResponse;
import org.usth.ict.ulake.common.model.dashboard.FileFormModel;
import org.usth.ict.ulake.common.model.folder.FileModel;
import org.usth.ict.ulake.common.service.LakeServiceExceptionMapper;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/api")
@RegisterRestClient(configKey = "dashboard-api")
@RegisterProvider(value = LakeServiceExceptionMapper.class)
@Produces(MediaType.APPLICATION_JSON)
public interface DashboardRestClient {
    @POST
    @Path("/file")
    @Schema(description = "upload new file")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    Uni<LakeHttpResponse<FileModel>> newFile(
            @HeaderParam("Authorization") String bearer, @MultipartForm FileFormModel file);
}

