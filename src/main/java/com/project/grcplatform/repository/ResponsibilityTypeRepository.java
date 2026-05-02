package com.project.grcplatform.repository;

import com.project.grcplatform.model.ResponsibilityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResponsibilityTypeRepository extends JpaRepository<ResponsibilityType, String> {
    Optional<ResponsibilityType> findByName(String name);
    boolean existsByName(String name);
}
