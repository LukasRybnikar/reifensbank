package com.task.reifensbank.service;

import com.task.reifensbank.entity.Document;
import com.task.reifensbank.entity.User;
import com.task.reifensbank.exceptions.ReifensbankHttpException;
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
import org.springframework.http.HttpStatus;
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

    // ---------- CREATE ----------

    @Test
    void create_happyPath_withAuthenticatedUser_persists_andUploads_andReturnsSaved() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("john", "N/A")
        );

        User john = new User();
        john.setId(42L);
        john.setUsername("john");
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(john));

        String objectKey = "documents/11111111-1111-1111-1111-111111111111.pdf";
        when(storage.buildObjectKey(anyString(), eq("pdf"))).thenReturn(objectKey);

        byte[] bytes = "dummy".getBytes();
        MultipartFile file = new MockMultipartFile("file", "sample.pdf", "application/pdf", bytes);

        ArgumentCaptor<Document> toSave = ArgumentCaptor.forClass(Document.class);
        Document saved = new Document();
        saved.setId(100L);
        saved.setPublicId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        saved.setFilename("myName");
        saved.setContentType("pdf");
        saved.setSizeBytes((long) bytes.length);
        saved.setStoragePath(objectKey);
        saved.setUploadedBy(john);

        when(documentRepository.save(any(Document.class))).thenReturn(saved);

        Document result = service.create(file, "myName", "pdf");

        verify(storage).buildObjectKey(anyString(), eq("pdf"));
        verify(storage).put(eq(objectKey), eq(file));

        verify(documentRepository, times(1)).save(toSave.capture());
        Document firstSavedArg = toSave.getValue();
        assertThat(firstSavedArg.getFilename()).isEqualTo("myName");
        assertThat(firstSavedArg.getContentType()).isEqualTo("pdf");
        assertThat(firstSavedArg.getSizeBytes()).isEqualTo(bytes.length);
        assertThat(firstSavedArg.getStoragePath()).isEqualTo(objectKey);
        assertThat(firstSavedArg.getUploadedBy()).isEqualTo(john);
        assertThat(firstSavedArg.getPublicId()).isNotNull();

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getPublicId()).isEqualTo(saved.getPublicId());
        assertThat(result.getStoragePath()).isEqualTo(objectKey);

        InOrder inOrder = inOrder(storage, documentRepository);
        inOrder.verify(storage).put(eq(objectKey), eq(file));
        inOrder.verify(documentRepository).save(any(Document.class));
    }

    @Test
    void create_withNoAuthentication_setsUploadedByNull() throws Exception {
        String objectKey = "documents/22222222-2222-2222-2222-222222222222.pdf";
        when(storage.buildObjectKey(anyString(), eq("pdf"))).thenReturn(objectKey);

        MultipartFile file = new MockMultipartFile("file", "sample.pdf", "application/pdf", "x".getBytes());

        ArgumentCaptor<Document> toSave = ArgumentCaptor.forClass(Document.class);
        Document saved = new Document();
        saved.setId(101L);
        saved.setPublicId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        saved.setStoragePath(objectKey);
        when(documentRepository.save(any(Document.class))).thenReturn(saved);

        Document result = service.create(file, "noAuthName", "pdf");

        verify(documentRepository).save(toSave.capture());
        Document savedArg = toSave.getValue();
        assertThat(savedArg.getUploadedBy()).isNull();
        assertThat(result.getStoragePath()).isEqualTo(objectKey);
    }

    @Test
    void create_whenStoragePutFails_throws503() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "x".getBytes());
        when(storage.buildObjectKey(anyString(), eq("pdf"))).thenReturn("bucket/key");
        doThrow(new Exception("minio down")).when(storage).put(anyString(), any(MultipartFile.class));

        assertThatThrownBy(() -> service.create(file, "n", "pdf"))
                .isInstanceOf(ReifensbankHttpException.class)
                .extracting("status").isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        verify(documentRepository, never()).save(any());
    }

    @Test
    void create_whenDbSaveThrows_cleansUpStorage_andWraps500() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("john", "N/A")
        );
        when(userRepository.findByUsername("john")).thenReturn(Optional.empty());

        String objectKey = "documents/33333333-3333-3333-3333-333333333333.pdf";
        when(storage.buildObjectKey(anyString(), eq("pdf"))).thenReturn(objectKey);

        MultipartFile file = new MockMultipartFile("file", "sample.pdf", "application/pdf", "x".getBytes());
        when(documentRepository.save(any(Document.class))).thenThrow(new RuntimeException("DB is down"));

        assertThatThrownBy(() -> service.create(file, "x", "pdf"))
                .isInstanceOf(ReifensbankRuntimeException.class);

        verify(storage).delete(objectKey);
    }

    // ---------- REPLACE CONTENT ----------

    @Test
    void replaceContent_happyPath_uploadsToStorageAndSavesEntity() throws Exception {
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

        Document result = service.replaceContent(id, file);

        verify(storage, times(1)).put(eq("bucket/path/oldkey"), eq(file));
        verify(documentRepository, times(1)).save(any(Document.class));

        assertThat(result.getPublicId()).isEqualTo(id);
        assertThat(result.getSizeBytes()).isEqualTo(file.getSize());
        assertThat(result.getUpdatedAt()).isNotNull();

        InOrder inOrder = inOrder(storage, documentRepository);
        inOrder.verify(storage).put(eq("bucket/path/oldkey"), eq(file));
        inOrder.verify(documentRepository).save(any(Document.class));
    }

    @Test
    void replaceContent_whenStoragePutFails_throws503_andNoDbSave() throws Exception {
        UUID id = UUID.fromString("99999999-9999-9999-9999-999999999999");
        MultipartFile file = new MockMultipartFile("file", "new.pdf", "application/pdf", "data".getBytes());

        Document existing = new Document();
        existing.setId(11L);
        existing.setPublicId(id);
        existing.setStoragePath("bucket/path/key");

        when(documentRepository.findByPublicId(id)).thenReturn(Optional.of(existing));
        doThrow(new Exception("storage down")).when(storage).put(anyString(), any(MultipartFile.class));

        assertThatThrownBy(() -> service.replaceContent(id, file))
                .isInstanceOf(ReifensbankHttpException.class)
                .extracting("status").isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void replaceContent_whenDocumentNotFound_throws404() throws Exception {
        UUID id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        MultipartFile file = new MockMultipartFile("file", "x.pdf", "application/pdf", "x".getBytes());

        when(documentRepository.findByPublicId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.replaceContent(id, file))
                .isInstanceOf(ReifensbankHttpException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);

        verifyNoInteractions(storage);
        verify(documentRepository, never()).save(any(Document.class));
    }

    // ---------- DELETE ----------

    @Test
    void delete_happyPath_deletesStorageThenDb() throws Exception {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Document doc = new Document();
        doc.setId(1L);
        doc.setPublicId(id);
        doc.setStoragePath("bucket/key");

        when(documentRepository.findByPublicId(id)).thenReturn(Optional.of(doc));
        when(documentRepository.isAttachedToAnyProtocol(id)).thenReturn(false);

        service.delete(id);

        InOrder inOrder = inOrder(storage, documentRepository);
        inOrder.verify(storage).delete("bucket/key");
        inOrder.verify(documentRepository).delete(doc);
    }

    @Test
    void delete_whenNotFound_throws404_andNoStorageOrDb() throws Exception {
        UUID id = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(documentRepository.findByPublicId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ReifensbankHttpException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);

        verifyNoInteractions(storage);
        verify(documentRepository, never()).delete(any());
    }

    @Test
    void delete_whenAttachedToProtocol_throws409_andNoDeletes() throws Exception {
        UUID id = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Document doc = new Document();
        doc.setPublicId(id);
        doc.setStoragePath("bucket/key");

        when(documentRepository.findByPublicId(id)).thenReturn(Optional.of(doc));
        when(documentRepository.isAttachedToAnyProtocol(id)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ReifensbankHttpException.class)
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);

        verifyNoInteractions(storage);
        verify(documentRepository, never()).delete(any());
    }

    @Test
    void delete_whenStorageDeleteFails_throws503_andNoDbDelete() throws Exception {
        UUID id = UUID.fromString("44444444-4444-4444-4444-444444444444");
        Document doc = new Document();
        doc.setPublicId(id);
        doc.setStoragePath("bucket/key");

        when(documentRepository.findByPublicId(id)).thenReturn(Optional.of(doc));
        when(documentRepository.isAttachedToAnyProtocol(id)).thenReturn(false);
        doThrow(new Exception("S3 down")).when(storage).delete("bucket/key");

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ReifensbankHttpException.class)
                .extracting("status").isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        verify(documentRepository, never()).delete(any());
    }
}
