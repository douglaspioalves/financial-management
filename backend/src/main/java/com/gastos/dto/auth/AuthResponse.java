package com.gastos.dto.auth;

public record AuthResponse(
    String token,
    String type,
    String name,
    String email
) {
    public static AuthResponse of(String token, String name, String email) {
        return new AuthResponse(token, "Bearer", name, email);
    }
}
