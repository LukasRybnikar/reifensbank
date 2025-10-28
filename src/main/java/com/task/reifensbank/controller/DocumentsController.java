package com.task.reifensbank.controller;

import com.task.reifensbank.api.DocumentsApi;
import com.task.reifensbank.model.Document;
import com.task.reifensbank.model.DocumentContentInfo;
import com.task.reifensbank.model.DocumentsUpdateMetadataRequest;
import com.task.reifensbank.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class DocumentsController implements DocumentsApi {

    private final LogService logService;

    @Override
    public ResponseEntity<Document> documentsCreate(
            MultipartFile file,
            String name,
            String type
    ) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    public ResponseEntity<Resource> documentsDownloadContent(
            UUID id
    ) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    public ResponseEntity<DocumentContentInfo> documentsGetContentInfo(
            UUID id
    ) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    public ResponseEntity<Document> documentsReplaceContent(
            UUID id,
            MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    public ResponseEntity<Document> documentsUpdateMetadata(
            UUID id,
            DocumentsUpdateMetadataRequest documentsUpdateMetadataRequest
    ) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
