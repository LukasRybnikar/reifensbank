package com.task.reifensbank.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "document")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "public_id", nullable = false)
    private UUID publicId;

    @NotNull
    @Column(name = "filename", nullable = false, columnDefinition = "text")
    private String filename;

    @NotNull
    @Column(name = "content_type", nullable = false, columnDefinition = "text")
    private String contentType;

    @NotNull
    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @NotNull
    @Column(name = "storage_path", nullable = false, columnDefinition = "text")
    private String storagePath;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "uploaded_by", nullable = true)
    private User uploadedBy;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;


    @ManyToMany(mappedBy = "documents", fetch = FetchType.LAZY)
    private Set<Protocol> protocols = new LinkedHashSet<>();
}
