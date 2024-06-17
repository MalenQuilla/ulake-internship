package org.usth.ict.ulake.textr.services;

import io.quarkus.scheduler.Scheduled;
import org.apache.lucene.document.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.usth.ict.ulake.common.model.LakeHttpResponse;
import org.usth.ict.ulake.common.model.user.AuthModel;
import org.usth.ict.ulake.common.service.CoreService;
import org.usth.ict.ulake.common.service.UserService;
import org.usth.ict.ulake.textr.models.IndexFiles;
import org.usth.ict.ulake.textr.models.IndexingStatus;
import org.usth.ict.ulake.textr.models.payloads.responses.IndexResponse;
import org.usth.ict.ulake.textr.repositories.IndexFilesRepository;
import org.usth.ict.ulake.textr.services.engines.IndexSearchEngineV2;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.io.InputStream;
import java.util.List;

@ApplicationScoped
public class SchedulerService {
    
    private static boolean isIndexing = false;
    
    Logger logger = LoggerFactory.getLogger(SchedulerService.class);
    
    @Autowired
    IndexFilesRepository indexFilesRepo;
    
    @Inject
    IndexSearchEngineV2 indexSearchEngine;
    
    @Inject
    @RestClient
    CoreService coreService;
    
    @Inject
    @RestClient
    UserService userService;
    
    @ConfigProperty(name = "textr.scheduled.username")
    String username;
    
    @ConfigProperty(name = "textr.scheduled.password")
    String password;
    
    // @Scheduled(cron = "{textr.scheduled.index}")
    @Scheduled(every = "600s")
    void index() {
        if (isIndexing) {
            logger.info("A cron job is already indexing");
            return;
        }
        
        // Evaluate the quality of previous indices
        this.maintainIndex();
        
        logger.info("Daily indexing...");
        
        List<IndexFiles> scheduledFiles;
        int page = 0;
        
        do {
            isIndexing = true;
            Pageable pageable = PageRequest.of(page, 50, Sort.by("size").ascending());
            IndexingStatus[] status = {IndexingStatus.STATUS_SCHEDULED, IndexingStatus.STATUS_FAILED};
            
            scheduledFiles = indexFilesRepo.findAllByStatusIn(List.of(status), pageable);
            
            if (!scheduledFiles.isEmpty()) {
                indexPage(scheduledFiles);
            }
            page += 1;
        } while (!scheduledFiles.isEmpty());
        isIndexing = false;
        logger.info("Daily indexing finished");
    }
    
    private void maintainIndex() {
        logger.info("Daily maintenance...");
        
        List<IndexFiles> indexedFiles;
        int page = 0;
        
        do {
            Pageable pageable = PageRequest.of(page, 50);
            
            indexedFiles = indexFilesRepo.findAllByStatus(IndexingStatus.STATUS_INDEXED, pageable);
            for (IndexFiles sf : indexedFiles) {
                String cid = sf.getCoreId();
                try {
                    if (indexSearchEngine.notIndexed(cid)) {
                        indexFilesRepo.updateStatusByFileId(sf.getFileId(), IndexingStatus.STATUS_SCHEDULED);
                        logger.info("Schedule file {} successfully.", cid);
                    }
                } catch (Exception e) {
                    logger.error("Maintenance failed at cid {}: ", cid, e);
                }
            }
            page += 1;
        } while (!indexedFiles.isEmpty());
        logger.info("Daily maintenance finished.");
    }
    
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    void indexPage(List<IndexFiles> scheduledFiles) {
        for (IndexFiles sf : scheduledFiles) {
            String cid = sf.getCoreId();
            try {
                if (sf.getStatus() == IndexingStatus.STATUS_FAILED || indexSearchEngine.notIndexed(cid)) {
                    // Login as admin textrService and grant access to core service
                    AuthModel authModel = new AuthModel(username, password);
                    LakeHttpResponse<Object> response = userService.getToken(authModel);
                    
                    if (response.getCode() != 200) {
                        logger.error("Textr service has no permission to access User and Core services: {}, {}",
                                     response.getMsg(), response.getResp());
                        return;
                    }
                    
                    String bearer = "bearer " + response.getResp();
                    try (InputStream stream = coreService.objectDataByFileId(sf.getFileId(), bearer)) {
                        Document document = indexSearchEngine.getDocument(cid, stream);
                        IndexResponse indexResponse = indexSearchEngine.indexDoc(document);
                        indexSearchEngine.commit();
                        
                        logger.info("Index file {} successfully. Total indexed: {} files", cid,
                                    indexResponse.getIndexed());
                    }
                } else {
                    logger.info("File {} is already indexed", cid);
                }
                indexFilesRepo.updateStatusByFileId(sf.getFileId(), IndexingStatus.STATUS_INDEXED);
            } catch (Exception e) {
                logger.error("Index file failed at cid {}: ", cid, e);
                indexFilesRepo.updateStatusByFileId(sf.getFileId(), IndexingStatus.STATUS_FAILED);
            }
        }
    }
}
