package com.project.grcplatform.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data @Builder
public class RiskHeatmapDTO {

    // Chaque cellule de la matrice 5x5
    private List<HeatmapCell> cells;

    @Data @Builder
    public static class HeatmapCell {
        private int likelihood;   // 1-5
        private int impact;       // 1-5
        private int riskScore;    // likelihood × impact
        private String riskLevel; // LOW / MEDIUM / HIGH / CRITICAL
        private long count;       // nombre de scénarios dans cette cellule
        private List<String> scenarioIds; // IDs des scénarios dans cette cellule
    }
}