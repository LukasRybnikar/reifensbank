package com.task.reifensbank.controller;

import com.task.reifensbank.api.DocumentsApi;
import com.task.reifensbank.model.Document;
import com.task.reifensbank.model.DocumentsUpdateMetadataRequest;
import com.task.reifensbank.usecase.DocumentsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

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
}
