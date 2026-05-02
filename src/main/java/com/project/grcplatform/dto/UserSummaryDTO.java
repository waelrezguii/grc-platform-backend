package com.project.grcplatform.dto;

import com.project.grcplatform.model.User;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class UserSummaryDTO {
    private String id;
    private String firstname;
    private String lastname;
    private String email;

    public static UserSummaryDTO of(User user) {
        if (user == null) return null;
        return builder()
                .id(user.getId())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .email(user.getEmail())
                .build();
    }
}