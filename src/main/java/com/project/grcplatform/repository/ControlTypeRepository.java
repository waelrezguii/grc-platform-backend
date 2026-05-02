package com.project.grcplatform.repository;

import com.project.grcplatform.model.ControlType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ControlTypeRepository extends JpaRepository<ControlType, String> {
    Optional<ControlType> findByName(String name);
    boolean existsByName(String name);
}
