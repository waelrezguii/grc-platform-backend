package com.project.grcplatform.dto;

public record LoginResult(
        boolean mfaRequired,
        String pendingToken,   // only set when mfaRequired=true
        UserResponseDTO user   // only set when mfaRequired=false
) {}
