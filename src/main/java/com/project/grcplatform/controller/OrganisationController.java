package com.project.grcplatform.controller;

import com.project.grcplatform.dto.OrganisationDTO;
import com.project.grcplatform.service.OrganisationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organisations")
@RequiredArgsConstructor
public class OrganisationController {

    private final OrganisationService organisationService;

    @GetMapping
    public List<OrganisationDTO> getAll() {
        return organisationService.getAll();
    }

    @GetMapping("/roots")
    public List<OrganisationDTO> getRoots() {
        return organisationService.getRoots();
    }

    @GetMapping("/{id}/children")
    public List<OrganisationDTO> getChildren(@PathVariable String id) {
        return organisationService.getChildren(id);
    }

    @PostMapping
    public OrganisationDTO create(@RequestBody OrganisationDTO dto) {
        return organisationService.create(dto);
    }

    @PatchMapping("/{id}")
    public OrganisationDTO update(@PathVariable String id, @RequestBody OrganisationDTO dto) {
        return organisationService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        organisationService.delete(id);
    }
}
