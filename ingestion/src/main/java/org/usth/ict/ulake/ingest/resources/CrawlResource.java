package org.usth.ict.ulake.ingest.resources;

import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.usth.ict.ulake.ingest.model.Policy;
import org.usth.ict.ulake.ingest.model.macro.FetchConfig;
import org.usth.ict.ulake.ingest.services.CrawlSvc;

@Path("/crawl")
public class CrawlResource {
    @Inject
    CrawlSvc svc;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RolesAllowed({"User", "Admin"})
    @Operation(summary = "call crawl process")
    @Transactional
    public Map<String, Object> crawl(
        @HeaderParam("Authorization") String token,
        @Parameter(description = "(FETCH to dry run, DOWNLOAD to crawl data")
        @QueryParam("mode") FetchConfig mode,
        @Parameter(description = "folder to store crawled files")
        @QueryParam("folderId") Long folderId,
        @Parameter(description = "brief-description of crawl process")
        @QueryParam("desc") String desc,
        @RequestBody(description = "instruction of crawl") Policy policy) {
        return svc.runCrawl(policy, mode, folderId, desc);
    }
}
