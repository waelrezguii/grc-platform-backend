package com.project.grcplatform.dto;

import com.project.grcplatform.constant.Language;
import lombok.Data;

import java.util.Map;
import java.util.Set;

@Data
public class UpdateUserDTO {

    private String email;

    private String firstname;
    private String lastname;
    private String password;
    private String phoneNumber;

    private String role;

    private String gradeId;

    private Set<String> permissions;

    private Map<String, Object> preferences;

    private Language language;
}