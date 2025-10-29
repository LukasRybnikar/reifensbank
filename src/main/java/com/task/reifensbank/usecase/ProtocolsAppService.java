package com.task.reifensbank.usecase;

import com.task.reifensbank.entity.Protocol;
import com.task.reifensbank.exceptions.ReifensbankRuntimeException;
import com.task.reifensbank.mappers.ProtocolMappers;
import com.task.reifensbank.model.ProtocolStateUpdate;
import com.task.reifensbank.model.ProtocolUpdate;
import com.task.reifensbank.service.ProtocolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProtocolsAppService {

    private final ProtocolService protocolService;

    public ResponseEntity<com.task.reifensbank.model.Protocol> create(com.task.reifensbank.model.ProtocolCreate req) {
        try {
            log.debug("Starting protocol creation: docs={}", req.getDocumentIds());
            Protocol saved = protocolService.create(req);
            var body = ProtocolMappers.toModel(saved);
            URI location = URI.create("/protocols/" + saved.getPublicId());
            log.debug("Protocol created: id={}, publicId={}, code={}, location={}", saved.getId(), saved.getPublicId(), saved.getCode(), location);
            return ResponseEntity.created(location).body(body);
        } catch (Exception e) {
            log.error("Failed to create protocol: '. Reason: {}", e.getMessage(), e);
            throw new ReifensbankRuntimeException();
        }
    }

    public ResponseEntity<com.task.reifensbank.model.Protocol> getById(UUID id) {
        try {
            log.debug("Fetching protocol: id={}", id);
            Protocol p = protocolService.getByPublicId(id);
            return ResponseEntity.ok(ProtocolMappers.toModel(p));
        } catch (Exception e) {
            log.error("Get protocol failed for {}: {}", id, e.getMessage(), e);
            throw new ReifensbankRuntimeException();
        }
    }

    public ResponseEntity<com.task.reifensbank.model.Protocol> updateAll(UUID id, ProtocolUpdate req) {
        try {
            log.debug("Updating protocol (full): id={}, state={}, docs={}", id, req.getState(), req.getDocumentIds());
            Protocol updated = protocolService.updateAll(id, req);
            return ResponseEntity.ok(ProtocolMappers.toModel(updated));
        } catch (Exception e) {
            log.error("Update protocol failed for {}: {}", id, e.getMessage(), e);
            throw new ReifensbankRuntimeException();
        }
    }

    public ResponseEntity<com.task.reifensbank.model.Protocol> updateState(UUID id, ProtocolStateUpdate req) {
        try {
            log.debug("Updating protocol state: id={}, state={}", id, req.getState());
            Protocol updated = protocolService.updateState(id, req);
            return ResponseEntity.ok(ProtocolMappers.toModel(updated));
        } catch (Exception e) {
            log.error("Update protocol state failed for {}: {}", id, e.getMessage(), e);
            throw new ReifensbankRuntimeException();
        }
    }
}
