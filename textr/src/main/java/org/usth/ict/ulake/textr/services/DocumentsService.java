package org.usth.ict.ulake.textr.services;

import org.apache.lucene.document.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.usth.ict.ulake.textr.models.Documents;
import org.usth.ict.ulake.textr.models.EDocStatus;
import org.usth.ict.ulake.textr.models.payloads.requests.MultipartBody;
import org.usth.ict.ulake.textr.models.ScheduledDocuments;
import org.usth.ict.ulake.textr.models.payloads.responses.IndexResponse;
import org.usth.ict.ulake.textr.models.payloads.responses.MessageResponse;
import org.usth.ict.ulake.textr.repositories.DocumentsRepository;
import org.usth.ict.ulake.textr.repositories.ScheduledDocumentsRepository;
import org.usth.ict.ulake.textr.services.engines.IndexSearchEngineV2;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.io.*;
import java.util.List;

@ApplicationScoped
@Transactional(Transactional.TxType.REQUIRED)
public class DocumentsService {

    @ConfigProperty(name = "textr.documents.dataDir")
    String dataDir;

    @Inject
    IndexSearchEngineV2 indexSearchEngine;

    @Autowired
    DocumentsRepository documentsRepository;

    @Autowired
    ScheduledDocumentsRepository scheduledDocumentsRepository;

    public MessageResponse upload(MultipartBody multipartBody) {
        if (documentsRepository.existsByNameAndStatus(multipartBody.getFilename(), EDocStatus.STATUS_STORED))
            return new MessageResponse(400, "File already uploaded");

        IndexResponse indexResponse;
        Documents documents = null;

        File file = new File(dataDir + multipartBody.getFilename());
        try {
            InputStream inputStream = multipartBody.getFile();
            OutputStream outputStream = new FileOutputStream(file, false);
            inputStream.transferTo(outputStream);
            inputStream.close();

            documents = new Documents(multipartBody.getFilename(), dataDir, EDocStatus.STATUS_STORED);
            documentsRepository.save(documents);

            Document doc = indexSearchEngine.getDocument(documents.getId(), file);
            indexResponse = indexSearchEngine.indexDoc(doc);
            indexSearchEngine.commit();
        } catch (Exception e) {
            if (file.delete())
                documentsRepository.delete(documents);
            return new MessageResponse(500, "File upload failed: " + e.getMessage()
                    + ". Rolled-back");
        }
        return new MessageResponse(200, indexResponse.getIndexed() + " files uploaded and indexed in database");
    }

    public List<Documents> listAllByStatus(EDocStatus status) throws RuntimeException {
        List<Documents> documents = documentsRepository.findAllByStatus(status);

        if (documents.isEmpty())
            throw new RuntimeException("No documents found");

        return documents;
    }

    public MessageResponse updateStatusById(Long id, EDocStatus status) {
        Documents doc = documentsRepository.findById(id).orElse(null);

        if (doc == null)
            return new MessageResponse(404, "No document found");

        if(doc.getStatus() == status)
            return new MessageResponse(400, "Document status already set: " + status.name());

        if (status == EDocStatus.STATUS_DELETED) {
            try {
                setDeleted(doc);
            } catch (Exception e) {
                return new MessageResponse(500, "File deletion failed: " + e.getMessage());
            }
        } else {
            try {
                setRestored(doc);
            } catch (Exception e) {
                return new MessageResponse(500, "File restoration failed: " + e.getMessage());
            }
        }
        documentsRepository.updateStatusByDocument(doc, status);

        return new MessageResponse(200, "Document status updated: " + status.name());
    }

    private void setDeleted(Documents doc) throws RuntimeException {
        String docName = doc.getName();

        File file = new File(dataDir + docName);
        File targetFile = new File(dataDir + "deleted/" + docName);

        if (!file.renameTo(targetFile))
            throw new RuntimeException("Unable to move file");

        ScheduledDocuments scheduledDocuments = new ScheduledDocuments(doc);
        scheduledDocumentsRepository.save(scheduledDocuments);
    }

    private void setRestored(Documents doc) throws RuntimeException {
        String docName = doc.getName();

        File file = new File(dataDir + "deleted/" + docName);
        File targetFile = new File(dataDir + docName);

        if(!file.renameTo(targetFile))
            throw new RuntimeException("Unable to move file");

        scheduledDocumentsRepository.deleteByDocId(doc.getId());
    }
}
