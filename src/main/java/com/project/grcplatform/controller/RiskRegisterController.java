package com.project.grcplatform.controller;

import com.project.grcplatform.constant.TreatmentStrategy;
import com.project.grcplatform.dto.*;
import com.project.grcplatform.service.RiskRegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/risk-register")
@RequiredArgsConstructor
public class RiskRegisterController {

    private final RiskRegisterService service;

    /**
     * GET /api/risk-register
     * Registre consolidé de tous les risques (scénarios VALIDATED)
     * avec leur appréciation, plan de traitement et statut global.
     *
     * Filtres disponibles :
     *   assetId          → filtrer par actif
     *   threatId         → filtrer par menace
     *   rawRiskLevel     → LOW | MEDIUM | HIGH | CRITICAL
     *   globalStatus     → OPEN | BEING_TREATED | TREATED | ACCEPTED
     *   treatmentStrategy → REDUCE | ACCEPT | TRANSFER | AVOID
     */
    @GetMapping
    public ResponseEntity<Page<RiskRegisterEntryDTO>> getRegister(
            @RequestParam(required = false) String assetId,
            @RequestParam(required = false) String threatId,
            @RequestParam(required = false) String rawRiskLevel,
            @RequestParam(required = false) String globalStatus,
            @RequestParam(required = false) TreatmentStrategy treatmentStrategy,
            Pageable pageable) {
        return ResponseEntity.ok(
                service.getRegister(assetId, threatId, rawRiskLevel, globalStatus, treatmentStrategy, pageable));
    }

    /**
     * GET /api/risk-register/{scenarioId}
     * Vue consolidée d'un risque spécifique.
     */
    @GetMapping("/{scenarioId}")
    public ResponseEntity<RiskRegisterEntryDTO> getEntry(@PathVariable String scenarioId) {
        return ResponseEntity.ok(service.getRegisterEntry(scenarioId));
    }

    /**
     * GET /api/risk-register/summary
     * Statistiques agrégées : totaux, niveaux, stratégies, top actifs, taux de réduction.
     */
    @GetMapping("/summary")
    public ResponseEntity<RiskRegisterSummaryDTO> getSummary() {
        return ResponseEntity.ok(service.getSummary());
    }

    /**
     * GET /api/risk-register/heatmap
     * Données de la matrice de risque 5×5 (probabilité × impact).
     * Chaque cellule contient le nombre de scénarios et leurs IDs.
     */
    @GetMapping("/heatmap")
    public ResponseEntity<RiskHeatmapDTO> getHeatmap() {
        return ResponseEntity.ok(service.getHeatmap());
    }
}