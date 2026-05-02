package com.project.grcplatform.repository;

import com.project.grcplatform.model.ControlCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ControlCategoryRepository extends JpaRepository<ControlCategory, String> {
    Optional<ControlCategory> findByName(String name);
    boolean existsByName(String name);
}
