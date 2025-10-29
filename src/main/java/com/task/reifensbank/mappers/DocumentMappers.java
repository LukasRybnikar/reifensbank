package com.task.reifensbank.mappers;

import com.task.reifensbank.entity.Document;


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
}
