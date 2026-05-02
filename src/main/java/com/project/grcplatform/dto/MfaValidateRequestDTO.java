package com.project.grcplatform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MfaValidateRequestDTO {

    @NotBlank(message = "Pending token is required")
    private String pendingToken;

    @NotBlank(message = "OTP is required")
    private String otp;
}
