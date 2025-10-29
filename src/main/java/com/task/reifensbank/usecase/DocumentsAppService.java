package com.task.reifensbank.usecase;

import com.task.reifensbank.entity.Document;
import com.task.reifensbank.exceptions.ReifensbankHttpException;
import com.task.reifensbank.exceptions.ReifensbankRuntimeException;
import com.task.reifensbank.mappers.DocumentMappers;
import com.task.reifensbank.model.DocumentsUpdateMetadataRequest;
import com.task.reifensbank.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentsAppService {

    private final DocumentService documentService;

    public ResponseEntity<com.task.reifensbank.model.Document> create(MultipartFile file,
                                                                      String name,
                                                                      String type) {

        //TODO: scnaner for mallware detection in files

        if (Objects.isNull(file) || file.isEmpty()) {
            throw new ReifensbankHttpException(HttpStatus.BAD_REQUEST, "File must be provided and non-empty");
        }
        if (Objects.isNull(name) || name.isBlank()) {
            throw new ReifensbankHttpException(HttpStatus.BAD_REQUEST, "Name must be provided");
        }
        if (Objects.isNull(type) || type.isBlank()) {
            throw new ReifensbankHttpException(HttpStatus.BAD_REQUEST, "Type must be provided");
        }

        try {
            log.debug("Starting document creation: name='{}', type='{}'", name, type);
            log.debug("Incoming file: originalName='{}', size={} bytes, contentType={}", file.getOriginalFilename(), file.getSize(), file.getContentType());

            Document saved = documentService.create(file, name, type);

            log.trace("Entity after persistence: id={}, publicId={}, filename={}", saved.getId(), saved.getPublicId(), saved.getFilename());

            com.task.reifensbank.model.Document body = DocumentMappers.toModel(saved);
            URI location = URI.create("/documents/" + saved.getPublicId());

            log.debug("Document created: id={}, publicId={}, location={}",
                    saved.getId(), saved.getPublicId(), location);

            return ResponseEntity.created(location).body(body);
        } catch (ReifensbankHttpException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create document: name='{}', type='{}'. Reason: {}", name, type, e.getMessage(), e);
            throw new ReifensbankRuntimeException();
        }
    }

    public ResponseEntity<com.task.reifensbank.model.Document> updateMetadata(UUID id, DocumentsUpdateMetadataRequest req) {
        if (Objects.isNull(id)) {
            throw new ReifensbankHttpException(HttpStatus.BAD_REQUEST, "Document id must be provided");
        }
        if (Objects.isNull(req) || (Objects.isNull(req.getName()) && Objects.isNull(req.getType()))) {
            throw new ReifensbankHttpException(HttpStatus.BAD_REQUEST, "No fields to update");
        }

        try {
            log.debug("Starting metadata update for document: id={}, request={}", id, req);
            Document updated = documentService.updateMetadata(id, req);
            log.trace("Entity after metadata update: id={}, filename='{}', dontentType='{}'", updated.getId(), updated.getFilename(), updated.getContentType());

            var body = DocumentMappers.toModel(updated);
            log.debug("Metadata update successful: id={}, publicId={}", updated.getId(), updated.getPublicId());
            return ResponseEntity.ok(body);
        } catch (ReifensbankHttpException e) {
            throw e;
        } catch (Exception e) {
            log.error("Update metadata failed for {}: {}", id, e.getMessage(), e);
            throw new ReifensbankRuntimeException();
        }
    }

    public ResponseEntity<com.task.reifensbank.model.Document> replaceContent(UUID id, MultipartFile file) {
        if (Objects.isNull(id)) {
            throw new ReifensbankHttpException(HttpStatus.BAD_REQUEST, "Document id must be provided");
        }
        if (Objects.isNull(file) || file.isEmpty()) {
            throw new ReifensbankHttpException(HttpStatus.BAD_REQUEST, "File must be provided and non-empty");
        }

        try {
            log.debug("Starting content replacement for document: id={}, fileName='{}'", id, file.getOriginalFilename());
            log.debug("Incoming file details: size={} bytes, contentType={}", file.getSize(), file.getContentType());

            Document updated = documentService.replaceContent(id, file);

            log.trace("Entity after content replacement: id={}, publicId={}, storagePath='{}', sizeBytes={}", updated.getId(), updated.getPublicId(), updated.getStoragePath(), updated.getSizeBytes());

            var body = DocumentMappers.toModel(updated);
            log.debug("Content replacement successful: id={}, publicId={}", updated.getId(), updated.getPublicId());

            return ResponseEntity.ok(body);
        } catch (ReifensbankHttpException e) {
            throw e;
        } catch (Exception e) {
            log.error("Content replacement failed for {}: {}", id, e.getMessage(), e);
            throw new ReifensbankRuntimeException();
        }
    }

    public ResponseEntity<Void> delete(UUID id) {
        if (Objects.isNull(id)) {
            throw new ReifensbankHttpException(HttpStatus.BAD_REQUEST, "Document id must be provided");
        }

        try {
            log.debug("Deleting document: id={}", id);
            documentService.delete(id);
            log.debug("Document deleted: id={}", id);
            return ResponseEntity.noContent().build();
        } catch (ReifensbankHttpException e) {
            throw e;
        } catch (Exception e) {
            log.error("Delete failed for {}: {}", id, e.getMessage(), e);
            throw new ReifensbankRuntimeException();
        }
    }
}
