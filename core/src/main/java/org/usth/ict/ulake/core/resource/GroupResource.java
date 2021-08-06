package org.usth.ict.ulake.core.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usth.ict.ulake.core.backend.impl.OpenIO;
import org.usth.ict.ulake.core.model.LakeGroup;
import org.usth.ict.ulake.core.model.LakeHttpResponse;
import org.usth.ict.ulake.core.persistence.GroupRepository;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/group")
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
public class GroupResource {
    private static final Logger log = LoggerFactory.getLogger(GroupResource.class);

    @Inject
    OpenIO fs;

    @Inject
    GroupRepository groupRepo;

    //@Inject
    LakeHttpResponse lakeResponse = new LakeHttpResponse();

    @GET
    public List<LakeGroup> all() {
        return groupRepo.listAll();
    }

    @GET
    @Path("/group/{id}")
    public LakeGroup one(@PathParam("id") Long id) {
        fs.ls(String.valueOf(id));
        return groupRepo.findById(id);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public String post() {
        return lakeResponse.toString(200);
    }

    @GET
    @Path("/group/list/{path}")
    public LakeGroup listByPath(@PathParam("path") String path) {
        log.info("{}: {}", path, fs.ls(path));
        return null;
    }
}