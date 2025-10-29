package com.task.reifensbank.mappers;

import com.task.reifensbank.entity.Document;
import com.task.reifensbank.model.DocumentContentInfo;

import java.time.OffsetDateTime;


public final class DocumentMappers {
    private DocumentMappers() {
    }

    public static com.task.reifensbank.model.Document toModel(Document e) {
        com.task.reifensbank.model.Document m = new com.task.reifensbank.model.Document();
        m.setId(e.getPublicId());
        m.setName(e.getFilename());
        m.setType(e.getContentType());
        m.setCreatedBy(e.getUploadedBy().getUsername());
        m.setCreatedAt(e.getCreatedAt());
        return m;
    }

    public static DocumentContentInfo toContentInfo(Document e,
                                                    String extension,
                                                    String checksumSha256,
                                                    OffsetDateTime uploadedAt) {
        DocumentContentInfo c = new DocumentContentInfo();
        c.setDocumentId(e.getPublicId());
        c.setFileName(e.getFilename());
        c.setExtension(extension);
        c.setMimeType(e.getContentType());
        c.setSizeBytes(e.getSizeBytes());
        c.setChecksumSha256(checksumSha256);
        c.setUploadedAt(uploadedAt != null ? uploadedAt : e.getCreatedAt());
        return c;
    }
}
