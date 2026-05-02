package com.project.grcplatform.dto;

import lombok.Data;

@Data
public class UpdatePasswordDTO {
    private String email;
    private String newPassword;
}