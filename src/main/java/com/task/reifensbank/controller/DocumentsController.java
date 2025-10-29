package com.task.reifensbank.controller;

import com.task.reifensbank.api.DocumentsApi;
import com.task.reifensbank.model.Document;
import com.task.reifensbank.model.DocumentContentInfo;
import com.task.reifensbank.model.DocumentsUpdateMetadataRequest;
import com.task.reifensbank.usecase.DocumentsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class DocumentsController implements DocumentsApi {

    private final DocumentsAppService documentsAppService;

    @Override
    public ResponseEntity<com.task.reifensbank.model.Document> documentsCreate(
            MultipartFile file,
            String name,
            String type
    ) {
        return documentsAppService.create(file, name, type);
    }

    @Override
    public ResponseEntity<Document> documentsUpdateMetadata(UUID id, DocumentsUpdateMetadataRequest documentsUpdateMetadataRequest) {
        return documentsAppService.updateMetadata(id, documentsUpdateMetadataRequest);
    }

    @Override
    public ResponseEntity<Document> documentsReplaceContent(UUID id, MultipartFile file) {
        return documentsAppService.replaceContent(id, file);
    }

    @Override
    public ResponseEntity<Void> documentsDelete(UUID id) {
        return documentsAppService.delete(id);
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented yet");
    }

    @Override
    public ResponseEntity<Resource> documentsDownloadContent(UUID id) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented yet");
    }

    @Override
    public ResponseEntity<DocumentContentInfo> documentsGetContentInfo(UUID id) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented yet");
    }
}
