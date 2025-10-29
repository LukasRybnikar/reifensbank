package com.task.reifensbank.service;

import com.task.reifensbank.entity.Document;
import com.task.reifensbank.entity.Protocol;
import com.task.reifensbank.entity.User;
import com.task.reifensbank.enums.ProtocolStatusEnum;
import com.task.reifensbank.exceptions.ReifensbankHttpException;
import com.task.reifensbank.exceptions.ReifensbankRuntimeException;
import com.task.reifensbank.model.ProtocolCreate;
import com.task.reifensbank.model.ProtocolState;
import com.task.reifensbank.model.ProtocolStateUpdate;
import com.task.reifensbank.model.ProtocolUpdate;
import com.task.reifensbank.repository.DocumentRepository;
import com.task.reifensbank.repository.ProtocolRepository;
import com.task.reifensbank.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProtocolServiceTest {

    @Mock
    ProtocolRepository protocolRepository;
    @Mock
    DocumentRepository documentRepository;
    @Mock
    UserRepository userRepository;

    @InjectMocks
    ProtocolService service;

    @AfterEach
    void clearCtx() {
        SecurityContextHolder.clearContext();
    }

    // ---------- getByPublicId ----------

    @Test
    void getByPublicId_whenMissing_throws404() {
        UUID id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        when(protocolRepository.findByPublicId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByPublicId(id))
                .isInstanceOf(ReifensbankHttpException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------- create ----------

    @Test
    void create_happyPath_authenticated_setsCreator_andSaves_docsPresent() {
        // auth user
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("john", "N/A")
        );
        User john = new User();
        john.setId(10L);
        john.setUsername("john");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(john));

        // docs
        UUID d1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID d2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Document doc1 = new Document();
        doc1.setId(1L);
        doc1.setPublicId(d1);
        Document doc2 = new Document();
        doc2.setId(2L);
        doc2.setPublicId(d2);
        when(documentRepository.findByPublicId(d1)).thenReturn(Optional.of(doc1));
        when(documentRepository.findByPublicId(d2)).thenReturn(Optional.of(doc2));

        // request
        ProtocolCreate req = new ProtocolCreate();
        req.setState(ProtocolState.NEW);
        req.setDocumentIds(java.util.List.of(d1, d2));

        // repo save returns same entity
        when(protocolRepository.save(any(Protocol.class))).thenAnswer(inv -> {
            Protocol p = inv.getArgument(0);
            p.setId(99L);
            return p;
        });

        Protocol result = service.create(req);

        ArgumentCaptor<Protocol> cap = ArgumentCaptor.forClass(Protocol.class);
        verify(protocolRepository).save(cap.capture());
        Protocol savedArg = cap.getValue();

        assertThat(savedArg.getCreatedBy()).isEqualTo(john);
        assertThat(savedArg.getUpdatedBy()).isEqualTo(john);
        assertThat(savedArg.getStatus()).isEqualTo(ProtocolStatusEnum.NEW);
        assertThat(savedArg.getDocuments()).extracting("publicId").containsExactlyInAnyOrder(d1, d2);

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getPublicId()).isNotNull();
    }

    @Test
    void create_happyPath_unauthenticated_creatorNull() {
        UUID d1 = UUID.fromString("33333333-3333-3333-3333-333333333333");
        when(documentRepository.findByPublicId(d1)).thenReturn(Optional.of(new Document()));

        ProtocolCreate req = new ProtocolCreate();
        req.setDocumentIds(java.util.List.of(d1));

        when(protocolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Protocol p = service.create(req);

        assertThat(p.getCreatedBy()).isNull();
        assertThat(p.getUpdatedBy()).isNull();
    }

    @Test
    void create_whenNoDocuments_400() {
        ProtocolCreate req = new ProtocolCreate(); // docIds null

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ReifensbankHttpException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);

        req.setDocumentIds(java.util.List.of()); // empty
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ReifensbankHttpException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_whenAnyDocumentMissing_400() {
        UUID existing = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UUID missing = UUID.fromString("55555555-5555-5555-5555-555555555555");

        when(documentRepository.findByPublicId(existing)).thenReturn(Optional.of(new Document()));
        when(documentRepository.findByPublicId(missing)).thenReturn(Optional.empty());

        ProtocolCreate req = new ProtocolCreate();
        req.setDocumentIds(java.util.List.of(existing, missing));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ReifensbankHttpException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_whenRepoSaveFails_wraps500() {
        UUID d1 = UUID.fromString("66666666-6666-6666-6666-666666666666");
        when(documentRepository.findByPublicId(d1)).thenReturn(Optional.of(new Document()));

        ProtocolCreate req = new ProtocolCreate();
        req.setDocumentIds(java.util.List.of(d1));

        when(protocolRepository.save(any())).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ReifensbankRuntimeException.class);
    }

    // ---------- updateAll ----------

    @Test
    void updateAll_happyPath_replacesDocs_setsUpdater_andState() {
        UUID pid = UUID.fromString("77777777-7777-7777-7777-777777777777");

        Protocol existing = new Protocol();
        existing.setId(1L);
        existing.setPublicId(pid);
        existing.setStatus(ProtocolStatusEnum.NEW);
        existing.setDocuments(new java.util.LinkedHashSet<>());
        when(protocolRepository.findByPublicId(pid)).thenReturn(Optional.of(existing));

        UUID d1 = UUID.fromString("88888888-8888-8888-8888-888888888888");
        UUID d2 = UUID.fromString("99999999-9999-9999-9999-999999999999");
        Document doc1 = new Document();
        doc1.setPublicId(d1);
        Document doc2 = new Document();
        doc2.setPublicId(d2);
        when(documentRepository.findByPublicId(d1)).thenReturn(Optional.of(doc1));
        when(documentRepository.findByPublicId(d2)).thenReturn(Optional.of(doc2));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anna", "N/A")
        );
        User anna = new User();
        anna.setUsername("anna");
        when(userRepository.findByUsername("anna")).thenReturn(Optional.of(anna));

        ProtocolUpdate req = new ProtocolUpdate();
        req.setState(ProtocolState.PREPARE_FOR_SHIPMENT);
        req.setDocumentIds(java.util.List.of(d1, d2));

        when(protocolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Protocol updated = service.updateAll(pid, req);

        assertThat(updated.getStatus()).isEqualTo(ProtocolStatusEnum.PREPARE_FOR_SHIPMENT);
        assertThat(updated.getUpdatedBy()).isEqualTo(anna);
        assertThat(updated.getDocuments()).extracting("publicId").containsExactlyInAnyOrder(d1, d2);
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    @Test
    void updateAll_whenNoDocs_400() {
        UUID pid = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        when(protocolRepository.findByPublicId(pid)).thenReturn(Optional.of(new Protocol()));

        ProtocolUpdate req = new ProtocolUpdate(); // null/empty
        assertThatThrownBy(() -> service.updateAll(pid, req))
                .isInstanceOf(ReifensbankHttpException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);

        req.setDocumentIds(java.util.List.of());
        assertThatThrownBy(() -> service.updateAll(pid, req))
                .isInstanceOf(ReifensbankHttpException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateAll_whenAnyDocumentMissing_400() {
        UUID pid = UUID.fromString("12121212-1212-1212-1212-121212121212");
        when(protocolRepository.findByPublicId(pid)).thenReturn(Optional.of(new Protocol()));

        UUID ok = UUID.fromString("13131313-1313-1313-1313-131313131313");
        UUID missing = UUID.fromString("14141414-1414-1414-1414-141414141414");

        when(documentRepository.findByPublicId(ok)).thenReturn(Optional.of(new Document()));
        when(documentRepository.findByPublicId(missing)).thenReturn(Optional.empty());

        ProtocolUpdate req = new ProtocolUpdate();
        req.setState(ProtocolState.NEW);
        req.setDocumentIds(java.util.List.of(ok, missing));

        assertThatThrownBy(() -> service.updateAll(pid, req))
                .isInstanceOf(ReifensbankHttpException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateAll_whenRepoSaveFails_wraps500() {
        UUID pid = UUID.fromString("15151515-1515-1515-1515-151515151515");
        Protocol p = new Protocol();
        p.setPublicId(pid);
        p.setDocuments(new java.util.LinkedHashSet<>());
        when(protocolRepository.findByPublicId(pid)).thenReturn(Optional.of(p));

        UUID d1 = UUID.fromString("16161616-1616-1616-1616-161616161616");
        when(documentRepository.findByPublicId(d1)).thenReturn(Optional.of(new Document()));

        ProtocolUpdate req = new ProtocolUpdate();
        req.setState(ProtocolState.NEW);
        req.setDocumentIds(java.util.List.of(d1));

        when(protocolRepository.save(any())).thenThrow(new RuntimeException("db fail"));

        assertThatThrownBy(() -> service.updateAll(pid, req))
                .isInstanceOf(ReifensbankRuntimeException.class);
    }

    // ---------- updateState ----------

    @Test
    void updateState_happyPath_setsStatus_andUpdater() {
        UUID pid = UUID.fromString("17171717-1717-1717-1717-171717171717");

        Protocol p = new Protocol();
        p.setPublicId(pid);
        when(protocolRepository.findByPublicId(pid)).thenReturn(Optional.of(p));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("kate", "N/A")
        );
        User kate = new User();
        kate.setUsername("kate");
        when(userRepository.findByUsername("kate")).thenReturn(Optional.of(kate));

        ProtocolStateUpdate req = new ProtocolStateUpdate();
        req.setState(ProtocolState.CANCELED);

        when(protocolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Protocol updated = service.updateState(pid, req);

        assertThat(updated.getStatus()).isEqualTo(ProtocolStatusEnum.CANCELED);
        assertThat(updated.getUpdatedBy()).isEqualTo(kate);
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    @Test
    void updateState_whenRepoSaveFails_wraps500() {
        UUID pid = UUID.fromString("18181818-1818-1818-1818-181818181818");
        when(protocolRepository.findByPublicId(pid)).thenReturn(Optional.of(new Protocol()));

        ProtocolStateUpdate req = new ProtocolStateUpdate();
        req.setState(ProtocolState.NEW);

        when(protocolRepository.save(any())).thenThrow(new RuntimeException("db"));

        assertThatThrownBy(() -> service.updateState(pid, req))
                .isInstanceOf(ReifensbankRuntimeException.class);
    }
}
