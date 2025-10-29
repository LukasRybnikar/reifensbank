package com.task.reifensbank.repository;

import com.task.reifensbank.entity.Document;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    @EntityGraph(attributePaths = "uploadedBy")
    Optional<Document> findByPublicId(UUID publicId);

    @Query("""
              select (count(p) > 0)
              from Document d
              join d.protocols p
              where d.publicId = :id
            """)
    boolean isAttachedToAnyProtocol(@Param("id") UUID id);
}
