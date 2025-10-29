package com.task.reifensbank.repository;

import com.task.reifensbank.entity.Document;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    @EntityGraph(attributePaths = "uploadedBy")
    Optional<Document> findByPublicId(UUID publicId);
}
