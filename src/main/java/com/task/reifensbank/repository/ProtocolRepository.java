package com.task.reifensbank.repository;

import com.task.reifensbank.entity.Protocol;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProtocolRepository extends JpaRepository<Protocol, Long> {
    @EntityGraph(attributePaths = "documents")
    Optional<Protocol> findByPublicId(UUID publicId);
}
