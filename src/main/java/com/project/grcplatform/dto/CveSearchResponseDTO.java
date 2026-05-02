package com.project.grcplatform.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data @Builder
public class CveSearchResponseDTO {
    private int totalResults;
    private int resultsPerPage;
    private int startIndex;
    private List<CveItemDTO> vulnerabilities;
}