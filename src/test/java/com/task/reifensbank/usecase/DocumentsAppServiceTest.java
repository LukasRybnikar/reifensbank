package com.task.reifensbank.usecase;

import com.task.reifensbank.entity.Document;
import com.task.reifensbank.exceptions.ReifensbankRuntimeException;
import com.task.reifensbank.mappers.DocumentMappers;
import com.task.reifensbank.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentsAppServiceTest {

    @Mock
    private DocumentService documentService;

    @InjectMocks
    private DocumentsAppService appService;

    @Test
    void create_happyPath_returns201WithLocationAndMappedBody() throws Exception {

        UUID publicId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        Document entity = new Document();
        entity.setId(123L);
        entity.setPublicId(publicId);
        entity.setFilename("sample.pdf");

        MultipartFile file = new MockMultipartFile("file", "sample.pdf", "application/pdf", "x".getBytes());

        when(documentService.create(file, "sample", "pdf")).thenReturn(entity);

        var mapped = new com.task.reifensbank.model.Document();
        mapped.setId(publicId);
        mapped.setName("sample.pdf");
        mapped.setType("pdf");

        try (MockedStatic<DocumentMappers> mapperMock = Mockito.mockStatic(DocumentMappers.class)) {
            mapperMock.when(() -> DocumentMappers.toModel(entity)).thenReturn(mapped);


            ResponseEntity<com.task.reifensbank.model.Document> resp =
                    appService.create(file, "sample", "pdf");

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(resp.getHeaders().getLocation()).isNotNull();
            assertThat(resp.getHeaders().getLocation().toString())
                    .isEqualTo("/documents/" + publicId);

            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getId()).isEqualTo(publicId);
            assertThat(resp.getBody().getName()).isEqualTo("sample.pdf");
            assertThat(resp.getBody().getType()).isEqualTo("pdf");

            verify(documentService, times(1)).create(file, "sample", "pdf");
            mapperMock.verify(() -> DocumentMappers.toModel(entity), times(1));
        }
    }

    @Test
    void create_whenDocumentServiceThrows_wrapsIntoReifensbankRuntimeException() throws Exception {

        MultipartFile file = new MockMultipartFile("file", "x.pdf", "application/pdf", "x".getBytes());
        when(documentService.create(any(), any(), any()))
                .thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> appService.create(file, "x", "pdf"))
                .isInstanceOf(ReifensbankRuntimeException.class);

        verify(documentService, times(1)).create(file, "x", "pdf");
    }

    @Test
    void create_forwardsParametersExactly() throws Exception {

        MultipartFile file = new MockMultipartFile("file", "sample.pdf", "application/pdf", "x".getBytes());

        Document entity = new Document();
        entity.setPublicId(UUID.randomUUID());
        entity.setFilename("whatever.pdf");
        when(documentService.create(any(), any(), any())).thenReturn(entity);

        try (MockedStatic<DocumentMappers> mapperMock = Mockito.mockStatic(DocumentMappers.class)) {
            mapperMock.when(() -> DocumentMappers.toModel(any())).thenReturn(new com.task.reifensbank.model.Document());

            appService.create(file, "myName", "pdf");

            ArgumentCaptor<MultipartFile> fileCap = ArgumentCaptor.forClass(MultipartFile.class);
            ArgumentCaptor<String> nameCap = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> typeCap = ArgumentCaptor.forClass(String.class);

            verify(documentService).create(fileCap.capture(), nameCap.capture(), typeCap.capture());

            assertThat(fileCap.getValue().getOriginalFilename()).isEqualTo("sample.pdf");
            assertThat(nameCap.getValue()).isEqualTo("myName");
            assertThat(typeCap.getValue()).isEqualTo("pdf");
        }
    }

    @Test
    void updateMetadata_happyPath_returns200WithMappedBody() {
        UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");

        var req = new com.task.reifensbank.model.DocumentsUpdateMetadataRequest();
        req.setName("New-Name");
        req.setType("pdf");

        Document entity = new Document();
        entity.setId(456L);
        entity.setPublicId(id);
        entity.setFilename("New-Name.pdf");
        entity.setContentType("application/pdf");

        when(documentService.updateMetadata(id, req)).thenReturn(entity);

        var mapped = new com.task.reifensbank.model.Document();
        mapped.setId(id);
        mapped.setName("New-Name.pdf");
        mapped.setType("pdf");

        try (MockedStatic<DocumentMappers> mapperMock = Mockito.mockStatic(DocumentMappers.class)) {
            mapperMock.when(() -> DocumentMappers.toModel(entity)).thenReturn(mapped);

            ResponseEntity<com.task.reifensbank.model.Document> resp =
                    appService.updateMetadata(id, req);

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getId()).isEqualTo(id);
            assertThat(resp.getBody().getName()).isEqualTo("New-Name.pdf");
            assertThat(resp.getBody().getType()).isEqualTo("pdf");

            verify(documentService, times(1)).updateMetadata(id, req);
            mapperMock.verify(() -> DocumentMappers.toModel(entity), times(1));
        }
    }

    @Test
    void updateMetadata_whenServiceThrows_wrapsIntoReifensbankRuntimeException() {
        UUID id = UUID.fromString("33333333-3333-3333-3333-333333333333");
        var req = new com.task.reifensbank.model.DocumentsUpdateMetadataRequest();
        req.setName("X");

        when(documentService.updateMetadata(any(UUID.class), any(com.task.reifensbank.model.DocumentsUpdateMetadataRequest.class)))
                .thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> appService.updateMetadata(id, req))
                .isInstanceOf(ReifensbankRuntimeException.class);

        verify(documentService, times(1)).updateMetadata(id, req);
    }

    @Test
    void updateMetadata_forwardsParametersExactly() {
        UUID id = UUID.fromString("44444444-4444-4444-4444-444444444444");
        var req = new com.task.reifensbank.model.DocumentsUpdateMetadataRequest();
        req.setName("ExactName");
        req.setType("txt");

        Document entity = new Document();
        entity.setPublicId(id);
        when(documentService.updateMetadata(any(), any())).thenReturn(entity);

        try (MockedStatic<DocumentMappers> mapperMock = Mockito.mockStatic(DocumentMappers.class)) {
            mapperMock.when(() -> DocumentMappers.toModel(any()))
                    .thenReturn(new com.task.reifensbank.model.Document());

            appService.updateMetadata(id, req);

            ArgumentCaptor<UUID> idCap = ArgumentCaptor.forClass(UUID.class);
            ArgumentCaptor<com.task.reifensbank.model.DocumentsUpdateMetadataRequest> reqCap =
                    ArgumentCaptor.forClass(com.task.reifensbank.model.DocumentsUpdateMetadataRequest.class);

            verify(documentService).updateMetadata(idCap.capture(), reqCap.capture());

            assertThat(idCap.getValue()).isEqualTo(id);
            assertThat(reqCap.getValue().getName()).isEqualTo("ExactName");
            assertThat(reqCap.getValue().getType()).isEqualTo("txt");
        }
    }

    @Test
    void replaceContent_happyPath_returns200WithMappedBody() {
        UUID id = UUID.fromString("55555555-5555-5555-5555-555555555555");

        // Simulate uploaded file
        MultipartFile file = new MockMultipartFile("file", "new.pdf", "application/pdf", "data".getBytes());

        // Mock the entity returned by the service
        Document entity = new Document();
        entity.setId(789L);
        entity.setPublicId(id);
        entity.setFilename("whatever.pdf");
        entity.setContentType("application/pdf");
        entity.setSizeBytes((long) file.getSize());
        entity.setStoragePath("bucket/key");

        when(documentService.replaceContent(id, file)).thenReturn(entity);

        // Mock the mapper
        com.task.reifensbank.model.Document mapped = new com.task.reifensbank.model.Document();
        mapped.setId(id);
        mapped.setName("whatever.pdf");
        mapped.setType("pdf");

        try (MockedStatic<DocumentMappers> mapperMock = Mockito.mockStatic(DocumentMappers.class)) {
            mapperMock.when(() -> DocumentMappers.toModel(entity)).thenReturn(mapped);

            // Execute
            ResponseEntity<com.task.reifensbank.model.Document> resp =
                    appService.replaceContent(id, file);

            // Validate response
            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().getId()).isEqualTo(id);
            assertThat(resp.getBody().getName()).isEqualTo("whatever.pdf");
            assertThat(resp.getBody().getType()).isEqualTo("pdf");

            // Verify mocks
            verify(documentService, times(1)).replaceContent(id, file);
            mapperMock.verify(() -> DocumentMappers.toModel(entity), times(1));
        }
    }

    @Test
    void replaceContent_whenServiceThrows_wrapsIntoReifensbankRuntimeException() {
        UUID id = UUID.fromString("66666666-6666-6666-6666-666666666666");
        MultipartFile file = new MockMultipartFile("file", "x.pdf", "application/pdf", "x".getBytes());

        when(documentService.replaceContent(any(UUID.class), any(MultipartFile.class)))
                .thenThrow(new RuntimeException("boom"));

        // Expect custom runtime exception
        assertThatThrownBy(() -> appService.replaceContent(id, file))
                .isInstanceOf(ReifensbankRuntimeException.class);

        verify(documentService, times(1)).replaceContent(id, file);
    }

    @Test
    void replaceContent_forwardsParametersExactly() {
        UUID id = UUID.fromString("77777777-7777-7777-7777-777777777777");
        MultipartFile file = new MockMultipartFile("file", "new.pdf", "application/pdf", "data".getBytes());

        // Return dummy entity so the call completes
        Document entity = new Document();
        entity.setPublicId(id);
        when(documentService.replaceContent(any(), any())).thenReturn(entity);

        try (MockedStatic<DocumentMappers> mapperMock = Mockito.mockStatic(DocumentMappers.class)) {
            mapperMock.when(() -> DocumentMappers.toModel(any()))
                    .thenReturn(new com.task.reifensbank.model.Document());

            // Execute method
            appService.replaceContent(id, file);

            // Capture arguments passed to service
            ArgumentCaptor<UUID> idCap = ArgumentCaptor.forClass(UUID.class);
            ArgumentCaptor<MultipartFile> fileCap = ArgumentCaptor.forClass(MultipartFile.class);

            verify(documentService).replaceContent(idCap.capture(), fileCap.capture());

            // Assert exact forwarding of params
            assertThat(idCap.getValue()).isEqualTo(id);
            assertThat(fileCap.getValue().getOriginalFilename()).isEqualTo("new.pdf");
            assertThat(fileCap.getValue().getContentType()).isEqualTo("application/pdf");
            assertThat(fileCap.getValue().getSize()).isEqualTo(4);
        }
    }

    @Test
    void delete_happyPath_returns204_andDelegatesToService() {
        // Arrange
        UUID id = UUID.fromString("55555555-5555-5555-5555-555555555555");

        // Act
        ResponseEntity<Void> resp = appService.delete(id);

        // Assert
        assertThat(resp.getStatusCodeValue()).isEqualTo(204);
        verify(documentService, times(1)).delete(id);
    }

    @Test
    void delete_whenServiceThrows_wrapsIntoReifensbankRuntimeException() {
        // Arrange
        UUID id = UUID.fromString("66666666-6666-6666-6666-666666666666");
        doThrow(new ReifensbankRuntimeException()).when(documentService).delete(id);

        // Act + Assert
        assertThatThrownBy(() -> appService.delete(id))
                .isInstanceOf(ReifensbankRuntimeException.class);

        verify(documentService).delete(id);
    }


}
