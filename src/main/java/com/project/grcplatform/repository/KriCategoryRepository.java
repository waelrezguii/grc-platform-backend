package com.project.grcplatform.repository;

import com.project.grcplatform.model.KriCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KriCategoryRepository extends JpaRepository<KriCategory, String> {
    Optional<KriCategory> findByName(String name);
    boolean existsByName(String name);
}
