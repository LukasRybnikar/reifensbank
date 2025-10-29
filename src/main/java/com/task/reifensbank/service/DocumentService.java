package com.task.reifensbank.service;

import com.task.reifensbank.entity.Document;
import com.task.reifensbank.entity.User;
import com.task.reifensbank.exceptions.ReifensbankRuntimeException;
import com.task.reifensbank.repository.DocumentRepository;
import com.task.reifensbank.repository.UserRepository;
import com.task.reifensbank.service.storage.StorageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final StorageService storage;

    public Document getByPublicId(UUID id) {
        return documentRepository.findByPublicId(id)
                .orElseThrow(ReifensbankRuntimeException::new);
    }

    @Transactional
    public Document create(MultipartFile file, String name, String extension) throws Exception {

        String username = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : null;

        log.debug("Creating document: name='{}', extension='{}', username='{}'", name, extension, username);
        log.trace("Incoming file details: originalName='{}', size={} bytes, contentType={}", file.getOriginalFilename(), file.getSize(), file.getContentType());

        User uploadedBy = null;
        if (username != null) {
            uploadedBy = userRepository.findByUsername(username).orElse(null);
            log.trace("Uploader resolved: {}", (uploadedBy != null) ? uploadedBy.getUsername() : "anonymous/null");
        }

        UUID publicId = UUID.randomUUID();
        String objectKey = storage.buildObjectKey(publicId.toString(), extension);
        log.trace("Generated identifiers: publicId={}, objectKey='{}'", publicId, objectKey);

        log.debug("Uploading object to storage: key='{}'", objectKey);
        storage.put(objectKey, file);
        log.debug("Upload finished: key='{}'", objectKey);

        try {
            Document doc = new Document();
            doc.setPublicId(publicId);
            doc.setFilename(name);
            doc.setContentType(extension);
            doc.setSizeBytes(file.getSize());
            doc.setStoragePath(objectKey);
            doc.setUploadedBy(uploadedBy);
            doc.setCreatedAt(OffsetDateTime.now());
            doc.setUpdatedAt(OffsetDateTime.now());

            log.debug("Persisting document entity to DB: publicId={}", publicId);
            Document saved = documentRepository.save(doc);
            log.debug("Document persisted: id={}, publicId={}", saved.getId(), saved.getPublicId());
            log.trace("Persisted entity snapshot: filename='{}', sizeBytes={}, contentType='{}', storagePath='{}'", saved.getFilename(), saved.getSizeBytes(), saved.getContentType(), saved.getStoragePath());

            return documentRepository.save(doc);

        } catch (RuntimeException ex) {
            log.error("DB persist failed for publicId={}, attempting storage cleanup for key='{}'. Reason: {}", publicId, objectKey, ex.getMessage(), ex);
            try {
                storage.delete(objectKey);
                log.debug("Storage cleanup successful: key='{}'", objectKey);
            } catch (Exception cleanupEx) {
                log.error("Storage cleanup failed: key='{}'. Reason: {}", objectKey, cleanupEx.getMessage(), cleanupEx);
            }
            throw new ReifensbankRuntimeException();
        }
    }

    @Transactional
    public Document updateMetadata(UUID id, com.task.reifensbank.model.DocumentsUpdateMetadataRequest req) {
        try {
            log.debug("Starting metadata update for document: publicId={}", id);
            log.trace("Incoming request payload: name='{}', type='{}'", req.getName(), req.getType());

            Document doc = getByPublicId(id);
            log.trace("Loaded document entity: id={}, filename='{}', contentType='{}', updatedAt={}",
                    doc.getId(), doc.getFilename(), doc.getContentType(), doc.getUpdatedAt());

            boolean updated = false;
            if (Objects.nonNull(req.getName())) {
                log.debug("Updating document name from '{}' → '{}'", doc.getFilename(), req.getName());
                doc.setFilename(req.getName());
                updated = true;
            }
            if (Objects.nonNull(req.getType())) {
                log.debug("Updating document contentType from '{}' → '{}'", doc.getContentType(), req.getType());
                doc.setContentType(req.getType());
                updated = true;
            }

            if (!updated) {
                log.debug("No metadata fields provided for update — skipping save for publicId={}", id);
                return doc;
            }

            doc.setUpdatedAt(OffsetDateTime.now());
            log.trace("Entity before save: filename='{}', contentType='{}', updatedAt={}",
                    doc.getFilename(), doc.getContentType(), doc.getUpdatedAt());

            Document saved = documentRepository.save(doc);
            log.debug("Document metadata updated successfully: id={}, publicId={}, filename='{}', contentType='{}'",
                    saved.getId(), saved.getPublicId(), saved.getFilename(), saved.getContentType());

            return saved;
        } catch (Exception e) {
            log.error("Metadata update failed for {}: {}", id, e.getMessage(), e);
            throw new ReifensbankRuntimeException();
        }
    }
}
