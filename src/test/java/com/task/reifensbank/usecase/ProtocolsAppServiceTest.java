package com.task.reifensbank.usecase;

import com.task.reifensbank.entity.Protocol;
import com.task.reifensbank.exceptions.ReifensbankHttpException;
import com.task.reifensbank.exceptions.ReifensbankRuntimeException;
import com.task.reifensbank.mappers.ProtocolMappers;
import com.task.reifensbank.model.ProtocolCreate;
import com.task.reifensbank.model.ProtocolState;
import com.task.reifensbank.model.ProtocolStateUpdate;
import com.task.reifensbank.model.ProtocolUpdate;
import com.task.reifensbank.service.ProtocolService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProtocolsAppServiceTest {

    @Mock
    private ProtocolService protocolService;

    @InjectMocks
    private ProtocolsAppService appService;

    // -------------------- CREATE --------------------

    @Test
    void create_happyPath_returns201AndMappedBody() {
        UUID publicId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        ProtocolCreate req = new ProtocolCreate();
        req.setDocumentIds(List.of(UUID.randomUUID()));
        req.setState(ProtocolState.NEW);

        Protocol entity = new Protocol();
        entity.setId(1L);
        entity.setPublicId(publicId);

        when(protocolService.create(req)).thenReturn(entity);

        com.task.reifensbank.model.Protocol mapped = new com.task.reifensbank.model.Protocol();
        mapped.setId(publicId);
        mapped.setDocumentIds(req.getDocumentIds());
        mapped.setState(ProtocolState.NEW);

        try (MockedStatic<ProtocolMappers> mapperMock = Mockito.mockStatic(ProtocolMappers.class)) {
            mapperMock.when(() -> ProtocolMappers.toModel(entity)).thenReturn(mapped);

            ResponseEntity<com.task.reifensbank.model.Protocol> resp = appService.create(req);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(resp.getHeaders().getLocation()).isEqualTo(URI.create("/protocols/" + publicId));
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getId()).isEqualTo(publicId);
            assertThat(resp.getBody().getState()).isEqualTo(ProtocolState.NEW);

            verify(protocolService).create(req);
            mapperMock.verify(() -> ProtocolMappers.toModel(entity));
        }
    }

    @Test
    void create_whenServiceThrowsHttp400_isPropagated() {
        ProtocolCreate req = new ProtocolCreate();
        req.setDocumentIds(List.of(UUID.randomUUID()));
        when(protocolService.create(req))
                .thenThrow(new ReifensbankHttpException(HttpStatus.BAD_REQUEST, "At least one document must be provided"));

        assertThatThrownBy(() -> appService.create(req))
                .isInstanceOf(ReifensbankHttpException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);

        verify(protocolService).create(req);
    }

    @Test
    void create_whenServiceThrowsUnexpected_wrapsIntoRuntime500() {
        ProtocolCreate req = new ProtocolCreate();
        req.setDocumentIds(List.of(UUID.randomUUID()));
        when(protocolService.create(any())).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> appService.create(req))
                .isInstanceOf(ReifensbankRuntimeException.class);

        verify(protocolService).create(req);
    }

    // -------------------- GET BY ID --------------------

    @Test
    void getById_happyPath_returns200AndMappedBody() {
        UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");

        Protocol entity = new Protocol();
        entity.setId(2L);
        entity.setPublicId(id);

        when(protocolService.getByPublicId(id)).thenReturn(entity);

        com.task.reifensbank.model.Protocol mapped = new com.task.reifensbank.model.Protocol();
        mapped.setId(id);
        mapped.setState(ProtocolState.NEW);

        try (MockedStatic<ProtocolMappers> mapperMock = Mockito.mockStatic(ProtocolMappers.class)) {
            mapperMock.when(() -> ProtocolMappers.toModel(entity)).thenReturn(mapped);

            ResponseEntity<com.task.reifensbank.model.Protocol> resp = appService.getById(id);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getId()).isEqualTo(id);

            verify(protocolService).getByPublicId(id);
            mapperMock.verify(() -> ProtocolMappers.toModel(entity));
        }
    }

    @Test
    void getById_whenServiceThrows404_isPropagated() {
        UUID id = UUID.randomUUID();
        when(protocolService.getByPublicId(id))
                .thenThrow(new ReifensbankHttpException(HttpStatus.NOT_FOUND, "Protocol not found"));

        assertThatThrownBy(() -> appService.getById(id))
                .isInstanceOf(ReifensbankHttpException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getById_whenServiceThrowsUnexpected_wrapsIntoRuntime500() {
        UUID id = UUID.randomUUID();
        when(protocolService.getByPublicId(id)).thenThrow(new RuntimeException("not found"));

        assertThatThrownBy(() -> appService.getById(id))
                .isInstanceOf(ReifensbankRuntimeException.class);
    }

    // -------------------- UPDATE ALL --------------------

    @Test
    void updateAll_happyPath_returns200AndMappedBody() {
        UUID id = UUID.fromString("33333333-3333-3333-3333-333333333333");

        ProtocolUpdate req = new ProtocolUpdate();
        req.setState(ProtocolState.PREPARE_FOR_SHIPMENT);
        req.setDocumentIds(List.of(UUID.randomUUID()));

        Protocol entity = new Protocol();
        entity.setId(3L);
        entity.setPublicId(id);

        when(protocolService.updateAll(id, req)).thenReturn(entity);

        com.task.reifensbank.model.Protocol mapped = new com.task.reifensbank.model.Protocol();
        mapped.setId(id);
        mapped.setState(ProtocolState.PREPARE_FOR_SHIPMENT);

        try (MockedStatic<ProtocolMappers> mapperMock = Mockito.mockStatic(ProtocolMappers.class)) {
            mapperMock.when(() -> ProtocolMappers.toModel(entity)).thenReturn(mapped);

            ResponseEntity<com.task.reifensbank.model.Protocol> resp = appService.updateAll(id, req);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getId()).isEqualTo(id);
            assertThat(resp.getBody().getState()).isEqualTo(ProtocolState.PREPARE_FOR_SHIPMENT);

            verify(protocolService).updateAll(id, req);
            mapperMock.verify(() -> ProtocolMappers.toModel(entity));
        }
    }

    @Test
    void updateAll_whenServiceThrowsHttp400_isPropagated() {
        UUID id = UUID.randomUUID();
        ProtocolUpdate req = new ProtocolUpdate();
        req.setDocumentIds(List.of()); // invalid

        when(protocolService.updateAll(any(), any()))
                .thenThrow(new ReifensbankHttpException(HttpStatus.BAD_REQUEST, "At least one document must be provided"));

        assertThatThrownBy(() -> appService.updateAll(id, req))
                .isInstanceOf(ReifensbankHttpException.class)
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateAll_whenServiceThrowsUnexpected_wrapsIntoRuntime500() {
        UUID id = UUID.randomUUID();
        ProtocolUpdate req = new ProtocolUpdate();

        when(protocolService.updateAll(any(), any())).thenThrow(new RuntimeException("fail"));

        assertThatThrownBy(() -> appService.updateAll(id, req))
                .isInstanceOf(ReifensbankRuntimeException.class);
    }

    // -------------------- UPDATE STATE --------------------

    @Test
    void updateState_happyPath_returns200AndMappedBody() {
        UUID id = UUID.fromString("44444444-4444-4444-4444-444444444444");
        ProtocolStateUpdate req = new ProtocolStateUpdate();
        req.setState(ProtocolState.CANCELED);

        Protocol entity = new Protocol();
        entity.setId(4L);
        entity.setPublicId(id);

        when(protocolService.updateState(id, req)).thenReturn(entity);

        com.task.reifensbank.model.Protocol mapped = new com.task.reifensbank.model.Protocol();
        mapped.setId(id);
        mapped.setState(ProtocolState.CANCELED);

        try (MockedStatic<ProtocolMappers> mapperMock = Mockito.mockStatic(ProtocolMappers.class)) {
            mapperMock.when(() -> ProtocolMappers.toModel(entity)).thenReturn(mapped);

            ResponseEntity<com.task.reifensbank.model.Protocol> resp = appService.updateState(id, req);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getId()).isEqualTo(id);
            assertThat(resp.getBody().getState()).isEqualTo(ProtocolState.CANCELED);

            verify(protocolService).updateState(id, req);
            mapperMock.verify(() -> ProtocolMappers.toModel(entity));
        }
    }

    @Test
    void updateState_whenServiceThrowsHttp409_isPropagated() {
        UUID id = UUID.randomUUID();
        ProtocolStateUpdate req = new ProtocolStateUpdate();
        req.setState(ProtocolState.CANCELED);

        when(protocolService.updateState(any(), any()))
                .thenThrow(new ReifensbankHttpException(HttpStatus.CONFLICT, "Illegal state change"));

        assertThatThrownBy(() -> appService.updateState(id, req))
                .isInstanceOf(ReifensbankHttpException.class)
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void updateState_whenServiceThrowsUnexpected_wrapsIntoRuntime500() {
        UUID id = UUID.randomUUID();
        ProtocolStateUpdate req = new ProtocolStateUpdate();

        when(protocolService.updateState(any(), any())).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> appService.updateState(id, req))
                .isInstanceOf(ReifensbankRuntimeException.class);
    }
}
