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
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = {
        "logging.level.com.task.reifensbank.usecase.DocumentsAppService=WARN"
})
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

        com.task.reifensbank.model.Document mapped = new com.task.reifensbank.model.Document();
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
}
