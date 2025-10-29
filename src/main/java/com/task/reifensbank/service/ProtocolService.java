package com.task.reifensbank.service;

import com.task.reifensbank.entity.Document;
import com.task.reifensbank.entity.Protocol;
import com.task.reifensbank.entity.User;
import com.task.reifensbank.enums.ProtocolStatusEnum;
import com.task.reifensbank.exceptions.ReifensbankHttpException;
import com.task.reifensbank.exceptions.ReifensbankRuntimeException;
import com.task.reifensbank.repository.DocumentRepository;
import com.task.reifensbank.repository.ProtocolRepository;
import com.task.reifensbank.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProtocolService {

    private final ProtocolRepository protocolRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    public Protocol getByPublicId(UUID id) {
        return protocolRepository.findByPublicId(id)
                .orElseThrow(() -> new ReifensbankHttpException(HttpStatus.NOT_FOUND, "Protocol not found"));
    }

    @Transactional
    public Protocol create(com.task.reifensbank.model.ProtocolCreate req) {
        if (req.getDocumentIds() == null || req.getDocumentIds().isEmpty()) {
            throw new ReifensbankHttpException(HttpStatus.BAD_REQUEST, "At least one document must be provided");
        }

        Set<Document> docs = new LinkedHashSet<>();
        Set<UUID> missing = new LinkedHashSet<>();

        for (UUID docId : req.getDocumentIds()) {
            documentRepository.findByPublicId(docId).ifPresentOrElse(
                    docs::add,
                    () -> missing.add(docId)
            );
        }

        if (!missing.isEmpty()) {
            throw new ReifensbankHttpException(HttpStatus.BAD_REQUEST, "Unknown document IDs: " + missing);
        }

        String username = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : null;

        User creator = null;
        if (username != null) {
            creator = userRepository.findByUsername(username).orElse(null);
        }

        try {
            Protocol p = new Protocol();
            p.setPublicId(UUID.randomUUID());
            p.setCode(generateCode());
            p.setStatus(req.getState() != null
                    ? ProtocolStatusEnum.valueOf(req.getState().name())
                    : ProtocolStatusEnum.NEW);
            p.setCreatedBy(creator);
            p.setUpdatedBy(creator);
            p.setCreatedAt(OffsetDateTime.now());
            p.setUpdatedAt(OffsetDateTime.now());
            p.getDocuments().addAll(docs);

            Protocol saved = protocolRepository.save(p);
            log.debug("Protocol created: id={}, publicId={}, code={}, docs={}", saved.getId(), saved.getPublicId(), saved.getCode(), docs.size());
            return saved;
        } catch (RuntimeException ex) {
            log.error("Protocol persist failed: {}", ex.getMessage(), ex);
            throw new ReifensbankRuntimeException();
        }
    }

    @Transactional
    public Protocol updateAll(UUID id, com.task.reifensbank.model.ProtocolUpdate req) {
        Protocol p = getByPublicId(id);

        if (req.getDocumentIds() == null || req.getDocumentIds().isEmpty()) {
            throw new ReifensbankHttpException(HttpStatus.BAD_REQUEST, "At least one document must be provided");
        }

        Set<Document> docs = new LinkedHashSet<>();
        Set<UUID> missing = new LinkedHashSet<>();

        for (UUID docId : req.getDocumentIds()) {
            documentRepository.findByPublicId(docId).ifPresentOrElse(
                    docs::add,
                    () -> missing.add(docId)
            );
        }

        if (!missing.isEmpty()) {
            throw new ReifensbankHttpException(HttpStatus.BAD_REQUEST, "Unknown document IDs: " + missing);
        }

        String username = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : null;

        User updater = null;
        if (username != null) {
            updater = userRepository.findByUsername(username).orElse(null);
        }

        try {
            p.setStatus(ProtocolStatusEnum.valueOf(req.getState().name()));
            p.getDocuments().clear();
            p.getDocuments().addAll(docs);
            p.setUpdatedBy(updater);
            p.setUpdatedAt(OffsetDateTime.now());
            return protocolRepository.save(p);
        } catch (RuntimeException ex) {
            log.error("Update protocol failed: {}", ex.getMessage(), ex);
            throw new ReifensbankRuntimeException();
        }
    }

    @Transactional
    public Protocol updateState(UUID id, com.task.reifensbank.model.ProtocolStateUpdate req) {
        Protocol p = getByPublicId(id);

        String username = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : null;

        User updater = null;
        if (username != null) {
            updater = userRepository.findByUsername(username).orElse(null);
        }

        try {
            p.setStatus(ProtocolStatusEnum.valueOf(req.getState().name()));
            p.setUpdatedBy(updater);
            p.setUpdatedAt(OffsetDateTime.now());
            return protocolRepository.save(p);
        } catch (RuntimeException ex) {
            log.error("Update protocol state failed: {}", ex.getMessage(), ex);
            throw new ReifensbankRuntimeException();
        }
    }

    private String generateCode() {
        return "PR-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
