package com.tuckersoft.branchengine.dto;

public record AuthResponse(
        String token,
        String type,
        String email,
        String displayName,
        String role
) {
}
