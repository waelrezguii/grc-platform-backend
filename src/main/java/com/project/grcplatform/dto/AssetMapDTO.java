package com.project.grcplatform.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AssetMapDTO {

    private SummaryDTO summary;
    private Map<String, List<AssetMapItemDTO>> byType;
    private Map<String, List<AssetMapItemDTO>> byDirection;

    @Data
    @Builder
    public static class SummaryDTO {
        private long total;
        private Map<String, Long> byCategory;
        private Map<String, Long> byType;
        private Map<Integer, Long> byCriticality;
    }

    @Data
    @Builder
    public static class AssetMapItemDTO {
        private String id;
        private String ref;
        private String name;
        private String typeName;
        private String categoryName;
        private Short criticalityScore;
        private String ownerName;
        private String directionName;
        private String directionCentraleName;
    }
}
