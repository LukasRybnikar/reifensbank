package com.task.reifensbank.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "authority")
public class Authority {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "code_name", nullable = false, length = Integer.MAX_VALUE)
    private String codeName;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @ManyToMany(mappedBy = "authorities", fetch = FetchType.LAZY)
    private Set<Role> roles = new LinkedHashSet<>();
}
