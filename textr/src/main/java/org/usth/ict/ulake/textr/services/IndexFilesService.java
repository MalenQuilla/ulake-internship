package org.usth.ict.ulake.textr.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.usth.ict.ulake.common.model.LakeHttpResponse;
import org.usth.ict.ulake.common.model.dashboard.FileFormModel;
import org.usth.ict.ulake.common.model.folder.FileModel;
import org.usth.ict.ulake.common.service.DashboardService;
import org.usth.ict.ulake.textr.clients.FileRestClient;
import org.usth.ict.ulake.textr.models.IndexFiles;
import org.usth.ict.ulake.textr.models.IndexingStatus;
import org.usth.ict.ulake.textr.models.payloads.responses.DocumentResponse;
import org.usth.ict.ulake.textr.repositories.IndexFilesRepository;
import org.usth.ict.ulake.textr.services.engines.IndexSearchEngineV2;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@Transactional(Transactional.TxType.REQUIRED)
public class IndexFilesService {
    
    Logger logger = LoggerFactory.getLogger(IndexFilesService.class);
    
    @Autowired
    IndexFilesRepository indexFilesRepo;
    
    @Inject
    IndexSearchEngineV2 indexSearchEngine;
    
    @Inject
    @RestClient
    DashboardService dashboardService;
    
    @Inject
    @RestClient
    FileRestClient fileService;
    
    @Inject
    ObjectMapper mapper;
    
    @Inject
    JsonWebToken jwt;
    
    public ServiceResponseBuilder<?> upload(String bearer, FileFormModel form) {
        LakeHttpResponse<FileModel> response = dashboardService.newFile(bearer, form);
        
        if (response.getCode() != 200)
            return new ServiceResponseBuilder<>(response.getCode(), response.getMsg(), response.getResp());
        
        FileModel fileModel = response.getResp();
        String coreId = fileModel.cid;
        Long fileId = fileModel.id;
        
        IndexFiles indexFiles = new IndexFiles(coreId, fileId, IndexingStatus.STATUS_SCHEDULED);
        indexFilesRepo.save(indexFiles);
        
        return new ServiceResponseBuilder<>(200, "File scheduled");
    }
    
    public ServiceResponseBuilder<List<FileModel>> listAllByStatus(IndexingStatus status, int page, int size) {
        String bearer = "bearer " + jwt.getRawToken();
        
        Pageable pageable = PageRequest.of(page, size);
        
        List<IndexFiles> list = indexFilesRepo.findAllByStatus(status, pageable);
        
        return getFilesInfo(bearer, list);
    }
    
    private ServiceResponseBuilder<List<FileModel>> getFilesInfo(String bearer, List<IndexFiles> list) {
        if (list == null || list.isEmpty())
            return new ServiceResponseBuilder<>(404, "No results found");
        
        StringBuilder queryBuilder = new StringBuilder();
        for (IndexFiles indexFiles : list) {
            queryBuilder.append(indexFiles.getFileId()).append(",");
        }
        
        LakeHttpResponse<List<FileModel>> response = fileService.fileList(bearer, queryBuilder.toString());
        
        if (response.getCode() != 200)
            return new ServiceResponseBuilder<>(response.getCode(), response.getMsg(), response.getResp());
        
        var type = new TypeReference<List<FileModel>>() {};
        var files = mapper.convertValue(response.getResp(), type);
        
        return new ServiceResponseBuilder<>(200, files);
    }
    
    public ServiceResponseBuilder<List<FileModel>> listAllByStatuses(List<IndexingStatus> statuses, int page,
                                                                     int size) {
        String bearer = "bearer " + jwt.getRawToken();
        
        Pageable pageable = PageRequest.of(page, size);
        
        List<IndexFiles> list = indexFilesRepo.findAllByStatusIn(statuses, pageable);
        
        return getFilesInfo(bearer, list);
    }
    
    public ServiceResponseBuilder<List<DocumentResponse>> search(String query) {
        List<DocumentResponse> documentResponses;
        try {
            documentResponses = indexSearchEngine.searchDoc(query);
        } catch (Exception e) {
            logger.error("Search doc failed: ", e);
            return new ServiceResponseBuilder<>(500, "Search failed: " + e.getMessage());
        }
        
        List<IndexFiles> list = new ArrayList<>();
        for (DocumentResponse docResp : documentResponses)
            list.add(docResp.getDocument());
        
        String bearer = "bearer " + jwt.getRawToken();
        ServiceResponseBuilder<List<FileModel>> serviceResponseBuilder = this.getFilesInfo(bearer, list);
        
        if (serviceResponseBuilder.getStatus() != 200)
            return new ServiceResponseBuilder<>(serviceResponseBuilder.getStatus(), serviceResponseBuilder.getMsg());
        
        List<FileModel> fileModels = serviceResponseBuilder.getResp();
        
        for (int i = 0; i < fileModels.size(); i++) {
            documentResponses.get(i).setFileModel(fileModels.get(i));
        }
        return new ServiceResponseBuilder<>(200, documentResponses);
    }
    
    public ServiceResponseBuilder<?> delete(String bearer, Long id) {
        IndexFiles indexFile = indexFilesRepo.findById(id).orElse(null);
        
        if (indexFile == null)
            return new ServiceResponseBuilder<>(404, "File not found");
        
        if (this.nonUpdatable(indexFile.getStatus(), IndexingStatus.STATUS_IGNORED))
            return new ServiceResponseBuilder<>(400, "File already deleted");
        
        Long fid = indexFile.getFileId();
        LakeHttpResponse<FileModel> response = fileService.deleteFile(bearer, fid);
        
        if (response.getCode() != 200)
            return new ServiceResponseBuilder<>(response.getCode(), response.getMsg(), response.getResp());
        
        indexFilesRepo.updateStatusById(id, IndexingStatus.STATUS_IGNORED);
        return new ServiceResponseBuilder<>(200, "File deleted");
    }
    
    private boolean nonUpdatable(IndexingStatus status, IndexingStatus targetStatus) {
        if (targetStatus == IndexingStatus.STATUS_INDEXED)
            return true;
        
        // Update from ignored to scheduled
        if (targetStatus == IndexingStatus.STATUS_SCHEDULED && status != IndexingStatus.STATUS_IGNORED)
            return true;
        
        // Update from scheduled and indexed to ignored
        return targetStatus == IndexingStatus.STATUS_IGNORED && status == IndexingStatus.STATUS_IGNORED;
    }
    
    public ServiceResponseBuilder<?> restore(Long id) {
        IndexFiles indexFile = indexFilesRepo.findById(id).orElse(null);
        
        if (indexFile == null)
            return new ServiceResponseBuilder<>(404, "File not found");
        
        if (this.nonUpdatable(indexFile.getStatus(), IndexingStatus.STATUS_SCHEDULED))
            return new ServiceResponseBuilder<>(400, "File already restored");
        
        indexFilesRepo.updateStatusById(id, IndexingStatus.STATUS_SCHEDULED);
        return new ServiceResponseBuilder<>(200, "File restored");
    }
    
    public ServiceResponseBuilder<?> reindex(Long id) {
        IndexFiles indexFile = indexFilesRepo.findByIdAndStatus(id, IndexingStatus.STATUS_INDEXED).orElse(null);
        
        if (indexFile == null)
            return new ServiceResponseBuilder<>(400, "File not found or scheduled");
        
        String cid = indexFile.getCoreId();
        
        try {
            indexSearchEngine.deleteDoc(cid);
            indexFilesRepo.updateStatusById(id, IndexingStatus.STATUS_SCHEDULED);
        } catch (Exception e) {
            logger.error("Reindex doc failed: ", e);
            return new ServiceResponseBuilder<>(500, "File reindex failed" + e.getMessage());
        }
        return new ServiceResponseBuilder<>(200, "File re-indexing in progress");
    }
}