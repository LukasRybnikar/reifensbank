package com.task.reifensbank.service;

import com.task.reifensbank.entity.Document;
import com.task.reifensbank.entity.User;
import com.task.reifensbank.exceptions.ReifensbankRuntimeException;
import com.task.reifensbank.repository.DocumentRepository;
import com.task.reifensbank.repository.UserRepository;
import com.task.reifensbank.service.storage.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    DocumentRepository documentRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    StorageService storage;

    @InjectMocks
    DocumentService service;

    @AfterEach
    void tearDownSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_happyPath_withAuthenticatedUser_persists_andUploads_andReturnsSaved() throws Exception {
        // arrange
        // Simulate authenticated user
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("john", "N/A")
        );

        User john = new User();
        john.setId(42L);
        john.setUsername("john");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(john));

        // Deterministic object key from storage
        String objectKey = "documents/11111111-1111-1111-1111-111111111111.pdf";
        when(storage.buildObjectKey(anyString(), eq("pdf"))).thenReturn(objectKey);

        // File to upload
        byte[] bytes = "dummy".getBytes();
        MultipartFile file = new MockMultipartFile("file", "sample.pdf", "application/pdf", bytes);

        // Repository returns saved entity (note: service currently calls save twice)
        ArgumentCaptor<Document> toSave = ArgumentCaptor.forClass(Document.class);
        Document saved = new Document();
        saved.setId(100L);
        UUID publicIdAssigned = UUID.fromString("11111111-1111-1111-1111-111111111111");
        saved.setPublicId(publicIdAssigned);
        saved.setFilename("myName");
        saved.setContentType("pdf");
        saved.setSizeBytes((long) bytes.length);
        saved.setStoragePath(objectKey);
        saved.setUploadedBy(john);

        when(documentRepository.save(any(Document.class))).thenReturn(saved, saved);

        // act
        Document result = service.create(file, "myName", "pdf");

        // assert
        // Storage interactions
        verify(storage).buildObjectKey(anyString(), eq("pdf"));
        verify(storage).put(eq(objectKey), eq(file));

        // DB interactions — currently twice due to the double-save in your method
        verify(documentRepository, times(2)).save(toSave.capture());

        // Inspect the document that was attempted to be saved
        Document firstSavedArg = toSave.getAllValues().get(0);
        assertThat(firstSavedArg.getFilename()).isEqualTo("myName");
        // NOTE: your service sets contentType to the *extension* value ("pdf")
        assertThat(firstSavedArg.getContentType()).isEqualTo("pdf");
        assertThat(firstSavedArg.getSizeBytes()).isEqualTo(bytes.length);
        assertThat(firstSavedArg.getStoragePath()).isEqualTo(objectKey);
        assertThat(firstSavedArg.getUploadedBy()).isEqualTo(john);
        assertThat(firstSavedArg.getPublicId()).isNotNull();

        // Return value is whatever repository returned
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getPublicId()).isEqualTo(publicIdAssigned);
        assertThat(result.getStoragePath()).isEqualTo(objectKey);

        // Order: put to storage before save (recommended)
        InOrder inOrder = inOrder(storage, documentRepository);
        inOrder.verify(storage).put(eq(objectKey), eq(file));
        inOrder.verify(documentRepository, atLeastOnce()).save(any(Document.class));
    }

    @Test
    void create_withNoAuthentication_setsUploadedByNull() throws Exception {
        // arrange (no auth context)
        String objectKey = "documents/22222222-2222-2222-2222-222222222222.pdf";
        when(storage.buildObjectKey(anyString(), eq("pdf"))).thenReturn(objectKey);

        MultipartFile file = new MockMultipartFile("file", "sample.pdf", "application/pdf", "x".getBytes());

        ArgumentCaptor<Document> toSave = ArgumentCaptor.forClass(Document.class);
        Document saved = new Document();
        saved.setId(101L);
        saved.setPublicId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        saved.setStoragePath(objectKey);
        when(documentRepository.save(any(Document.class))).thenReturn(saved, saved);

        // act
        Document result = service.create(file, "noAuthName", "pdf");

        // assert
        verify(documentRepository, atLeastOnce()).save(toSave.capture());
        Document savedArg = toSave.getValue();
        assertThat(savedArg.getUploadedBy()).isNull();
        assertThat(result.getStoragePath()).isEqualTo(objectKey);
    }

    @Test
    void create_whenDbSaveThrows_cleansUpStorage_andWrapsException() throws Exception {
        // arrange
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("john", "N/A")
        );
        when(userRepository.findByUsername("john")).thenReturn(Optional.empty());

        String objectKey = "documents/33333333-3333-3333-3333-333333333333.pdf";
        when(storage.buildObjectKey(anyString(), eq("pdf"))).thenReturn(objectKey);

        MultipartFile file = new MockMultipartFile("file", "sample.pdf", "application/pdf", "x".getBytes());

        when(documentRepository.save(any(Document.class))).thenThrow(new RuntimeException("DB is down"));

        // act + assert
        assertThatThrownBy(() -> service.create(file, "x", "pdf"))
                .isInstanceOf(ReifensbankRuntimeException.class);

        // cleanup attempted
        verify(storage).delete(objectKey);
    }

    @Test
    void replaceContent_happyPath_uploadsToStorageAndSavesEntity() throws Exception {
        // Arrange
        UUID id = UUID.fromString("88888888-8888-8888-8888-888888888888");
        MultipartFile file = new MockMultipartFile("file", "new.pdf", "application/pdf", "data".getBytes());

        Document existing = new Document();
        existing.setId(10L);
        existing.setPublicId(id);
        existing.setFilename("old.pdf");
        existing.setContentType("application/pdf");
        existing.setSizeBytes(123L);
        existing.setStoragePath("bucket/path/oldkey");

        when(documentRepository.findByPublicId(id)).thenReturn(Optional.of(existing));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Document result = service.replaceContent(id, file);

        // Assert storage interaction (upload to same key)
        verify(storage, times(1)).put(eq("bucket/path/oldkey"), eq(file));

        // Assert DB save happened
        verify(documentRepository, times(1)).save(any(Document.class));

        // Assert entity changes
        assertThat(result.getPublicId()).isEqualTo(id);
        assertThat(result.getSizeBytes()).isEqualTo(file.getSize());
        assertThat(result.getUpdatedAt()).isNotNull();

        // Optional: verify order (upload before save)
        InOrder inOrder = inOrder(storage, documentRepository);
        inOrder.verify(storage).put(eq("bucket/path/oldkey"), eq(file));
        inOrder.verify(documentRepository).save(any(Document.class));
    }

    @Test
    void replaceContent_whenStoragePutFails_wrapsAndDoesNotSave() throws Exception {
        // Arrange
        UUID id = UUID.fromString("99999999-9999-9999-9999-999999999999");
        MultipartFile file = new MockMultipartFile("file", "new.pdf", "application/pdf", "data".getBytes());

        Document existing = new Document();
        existing.setId(11L);
        existing.setPublicId(id);
        existing.setStoragePath("bucket/path/key");

        when(documentRepository.findByPublicId(id)).thenReturn(Optional.of(existing));
        doThrow(new RuntimeException("storage down")).when(storage).put(anyString(), any(MultipartFile.class));

        // Act + Assert
        assertThatThrownBy(() -> service.replaceContent(id, file))
                .isInstanceOf(ReifensbankRuntimeException.class);

        // No DB save when storage fails
        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void replaceContent_whenDocumentNotFound_throwsReifensbankRuntimeException() throws Exception {
        UUID id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        MultipartFile file = new MockMultipartFile("file", "x.pdf", "application/pdf", "x".getBytes());

        when(documentRepository.findByPublicId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.replaceContent(id, file))
                .isInstanceOf(ReifensbankRuntimeException.class);

        // no storage nor DB interaction
        verifyNoInteractions(storage);
        verify(documentRepository, never()).save(any(Document.class));
    }


}
