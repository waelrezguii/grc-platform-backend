package com.project.grcplatform.repository;

import com.project.grcplatform.model.Organisation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganisationRepository extends JpaRepository<Organisation, String> {

    List<Organisation> findByParentIsNull();

    List<Organisation> findByParent_Id(String parentId);
}
