package com.project.grcplatform.dto;

import com.project.grcplatform.model.Asset;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class AssetSummaryDTO {
    private String id;
    private String ref;
    private String name;

    public static AssetSummaryDTO of(Asset asset) {
        if (asset == null) return null;
        return builder()
                .id(asset.getId())
                .ref(asset.getRef())
                .name(asset.getName())
                .build();
    }
}
