package com.project.grcplatform.service;

import com.project.grcplatform.constant.AuditAction;
import com.project.grcplatform.constant.AuditEntityType;
import com.project.grcplatform.model.UserGrade;
import com.project.grcplatform.repository.UserGradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserGradeService {

    private final UserGradeRepository userGradeRepository;
    private final AuditService auditService;

    public List<UserGrade> getAll() {
        return userGradeRepository.findAll();
    }

    public UserGrade create(UserGrade grade) {
        UserGrade saved = userGradeRepository.save(grade);

        auditService.log(
                AuditAction.USER_CREATED,
                AuditEntityType.USER,
                saved.getId(),
                "UserGrade created: " + saved.getName()
        );

        return saved;
    }

    public UserGrade update(String id, UserGrade grade) {
        UserGrade existing = userGradeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("UserGrade not found"));

        Map<String, Object> oldValues = Map.of(
                "name",        existing.getName()        != null ? existing.getName()        : "",
                "description", existing.getDescription() != null ? existing.getDescription() : "",
                "orderLevel",  existing.getOrderLevel()  != null ? existing.getOrderLevel()  : 0
        );

        existing.setName(grade.getName());
        existing.setDescription(grade.getDescription());
        existing.setOrderLevel(grade.getOrderLevel());

        UserGrade saved = userGradeRepository.save(existing);

        Map<String, Object> newValues = Map.of(
                "name",        saved.getName()        != null ? saved.getName()        : "",
                "description", saved.getDescription() != null ? saved.getDescription() : "",
                "orderLevel",  saved.getOrderLevel()  != null ? saved.getOrderLevel()  : 0
        );

        auditService.log(
                AuditAction.USER_UPDATED,
                AuditEntityType.USER,
                id,
                "UserGrade updated: " + saved.getName(),
                oldValues,
                newValues
        );

        return saved;
    }

    public void delete(String id) {
        userGradeRepository.findById(id).ifPresent(grade ->
                auditService.log(
                        AuditAction.USER_DELETED,
                        AuditEntityType.USER,
                        id,
                        "UserGrade deleted: " + grade.getName()
                )
        );
        userGradeRepository.deleteById(id);
    }
}