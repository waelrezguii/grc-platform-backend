package com.project.grcplatform.repository;

import com.project.grcplatform.model.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentTypeRepository extends JpaRepository<DocumentType, String> {
    Optional<DocumentType> findByName(String name);
    boolean existsByName(String name);
}
