package com.project.grcplatform.dto;

import com.project.grcplatform.constant.WatchSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ThreatWatchSourceRequestDTO {

    @NotBlank
    private String name;

    @NotBlank
    private String url;

    @NotNull
    private WatchSourceType type;

    private String description;
    private Boolean enabled;
}
