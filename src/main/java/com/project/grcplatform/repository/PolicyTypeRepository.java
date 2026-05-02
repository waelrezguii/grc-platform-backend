package com.project.grcplatform.repository;

import com.project.grcplatform.model.PolicyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PolicyTypeRepository extends JpaRepository<PolicyType, String> {
    Optional<PolicyType> findByName(String name);
    boolean existsByName(String name);
}
