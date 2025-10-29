package com.task.reifensbank.repository;

import com.task.reifensbank.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = {"roles", "roles.authorities"})
    Optional<User> findByUsername(String username);
}
