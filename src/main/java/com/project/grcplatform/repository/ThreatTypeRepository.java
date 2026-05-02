package com.project.grcplatform.repository;

import com.project.grcplatform.model.ThreatType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ThreatTypeRepository extends JpaRepository<ThreatType, String> {
    Optional<ThreatType> findByName(String name);
    boolean existsByName(String name);
}
