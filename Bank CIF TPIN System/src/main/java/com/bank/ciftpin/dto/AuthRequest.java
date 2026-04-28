package com.bank.ciftpin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request payload for authenticating a CIF with TPIN")
public class AuthRequest {

    @NotBlank(message = "CIF number is required")
    @Schema(description = "The CIF number to authenticate", example = "123456789")
    private String cifNumber;

    @NotBlank(message = "TPIN is required")
    @Schema(description = "The TPIN for authentication", example = "1234")
    private String tpin;
}