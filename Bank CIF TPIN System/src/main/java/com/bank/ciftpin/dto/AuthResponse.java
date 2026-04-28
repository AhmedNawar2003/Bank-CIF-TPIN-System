package com.bank.ciftpin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Authentication response containing JWT token")
public class AuthResponse {

    @Schema(description = "JWT Bearer token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "Token type", example = "Bearer")
    private String tokenType;

    @Schema(description = "Token expiry in seconds", example = "86400")
    private long expiresIn;

    @Schema(description = "Authenticated CIF account info")
    private CifAccountResponse account;
}