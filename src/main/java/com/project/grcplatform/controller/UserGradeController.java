package com.project.grcplatform.controller;

import com.project.grcplatform.model.UserGrade;
import com.project.grcplatform.service.UserGradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
public class UserGradeController {

    private final UserGradeService userGradeService;

    @GetMapping
    public List<UserGrade> getAll() {
        return userGradeService.getAll();
    }

    @PostMapping
    public UserGrade create(@RequestBody UserGrade grade) {
        return userGradeService.create(grade);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserGrade update(@PathVariable String id, @RequestBody UserGrade grade) {
        return userGradeService.update(id, grade);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        userGradeService.delete(id);
    }
}